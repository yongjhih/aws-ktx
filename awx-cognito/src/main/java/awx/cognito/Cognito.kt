package awx.cognito

import com.amazonaws.mobileconnectors.cognitoidentityprovider.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.*
import com.amazonaws.services.cognitoidentityprovider.model.SignUpResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun CognitoUser.getSessionAsync(
    onMfaCode: (MultiFactorAuthenticationContinuation) -> Unit = {},
    onChallenge: (ChallengeContinuation) -> Unit = {},
    clientMetaData: Map<String, String?> = emptyMap(),
    onAuth: (AuthenticationContinuation, userId: String?) -> AuthenticationDetails,
): Pair<CognitoUserSession, CognitoDevice?> =
    suspendCoroutine { cont ->
        getSession(authenticationHandler(
            onAuth = onAuth,
            onSuccess = { session, device -> cont.resume(Pair(session, device)) },
            onMfaCode = onMfaCode,
            onChallenge = onChallenge,
            clientMetaData = clientMetaData,
            onFailure = { cont.resumeWithException(it) },
        ))
    }

suspend fun CognitoUser.confirmSignUpAsync(
    code: String,
    forcedAliasCreation: Boolean = false,
    clientMetadata: Map<String, String?> = emptyMap(),
): Unit =
    suspendCoroutine { cont ->
        confirmSignUp(code, forcedAliasCreation, clientMetadata, genericHandler(
            onSuccess = { cont.resume(Unit) },
            onFailure = { cont.resumeWithException(it) }
        ))

    }

suspend fun CognitoUser.forgotPasswordAsync(
    password: String? = null,
    verificationCode: String? = null,
    clientMetadata: Map<String, String?> = emptyMap(),
): Unit =
    suspendCoroutine { cont ->
        forgotPassword(clientMetadata, forgotPasswordHandler(
            password,
            verificationCode,
            clientMetadata,
            onSuccess = { cont.resume(Unit) },
            onFailure = { cont.resumeWithException(it) }
        ))
    }

fun forgotPasswordHandler(
    password: String? = null,
    verificationCode: String? = null,
    clientMetadata: Map<String, String?> = emptyMap(),
    onFailure: (Exception) -> Unit = {},
    onSuccess: () -> Unit = {},
): ForgotPasswordHandler =
    object : ForgotPasswordHandler {
        override fun onSuccess() =
            onSuccess()

        override fun getResetCode(continuation: ForgotPasswordContinuation) {
            if (!password.isNullOrEmpty()) {
                continuation.setPassword(password)
            }
            if (!verificationCode.isNullOrEmpty()) {
                continuation.setVerificationCode(verificationCode)
            }
            if (clientMetadata.isNotEmpty()) {
                continuation.setClientMetadata(clientMetadata)
            }
            continuation.continueTask()
        }

        override fun onFailure(exception: Exception) =
            onFailure(exception)
    }

fun genericHandler(
    onFailure: (Exception) -> Unit = {},
    onSuccess: () -> Unit = {},
): GenericHandler =
    object : GenericHandler {
        override fun onSuccess() =
            onSuccess()

        override fun onFailure(exception: Exception) =
            onFailure(exception)
    }

fun authenticationHandler(
    onAuth: (AuthenticationContinuation, userId: String?) -> AuthenticationDetails,
    onMfaCode: (MultiFactorAuthenticationContinuation) -> Unit = {},
    onChallenge: (ChallengeContinuation) -> Unit = {},
    onFailure: (Exception) -> Unit = {},
    clientMetaData: Map<String, String?> = emptyMap(),
    onSuccess: (CognitoUserSession, CognitoDevice?) -> Unit = { _, _ -> },
): AuthenticationHandler =
    object : AuthenticationHandler {
        override fun onSuccess(
            userSession: CognitoUserSession,
            newDevice: CognitoDevice?
        ) = onSuccess(userSession, newDevice)

        override fun getAuthenticationDetails(
            authenticationContinuation: AuthenticationContinuation,
            userId: String?
        ) = authenticationContinuation.run {
            if (clientMetaData.isNotEmpty()) {
                this.clientMetaData = clientMetaData
            }
            setAuthenticationDetails(onAuth(this, userId))
            continueTask()
        }

        override fun getMFACode(continuation: MultiFactorAuthenticationContinuation) =
            onMfaCode(continuation)

        override fun authenticationChallenge(continuation: ChallengeContinuation) =
            onChallenge(continuation)

        override fun onFailure(exception: Exception) =
            onFailure(exception)
    }

suspend fun CognitoUserPool.signUpAsync(
    userId: String,
    password: String,
    userAttributes: CognitoUserAttributes = CognitoUserAttributes(),
    validationData: Map<String, String?>? = null,
    clientMetadata: Map<String, String?> = emptyMap(),
): Pair<CognitoUser, SignUpResult?> =
    suspendCoroutine { cont ->
        signUp(
            userId,
            password,
            userAttributes,
            validationData,
            clientMetadata,
            signUpHandler(
                onSuccess = { user, signUpResult -> cont.resume(Pair(user, signUpResult)) },
                onFailure = { cont.resumeWithException(it) },
            )
        )
    }

fun signUpHandler(
    onFailure: (exception: Exception) -> Unit = {},
    onSuccess: (CognitoUser, SignUpResult?) -> Unit = { _, _ -> },
): SignUpHandler =
    object : SignUpHandler {
        override fun onSuccess(user: CognitoUser, signUpResult: SignUpResult?) =
            onSuccess(user, signUpResult)

        override fun onFailure(exception: Exception) =
            onFailure(exception)
    }

suspend fun CognitoUser.confirmPasswordAsync(
    code: String,
    password: String,
    clientMetadata: Map<String, String?> = emptyMap(),
): Unit =
    suspendCoroutine { cont ->
        confirmPassword(code, password, clientMetadata, forgotPasswordHandler(
            onSuccess = { cont.resume(Unit) },
            onFailure = { cont.resumeWithException(it) },
        ))
    }

suspend fun CognitoUser.resendConfirmationCodeAsync(
    clientMetadata: Map<String, String?> = emptyMap(),
): CognitoUserCodeDeliveryDetails =
    suspendCoroutine { cont ->
        resendConfirmationCode(clientMetadata, verificationHandler(
            onSuccess = { cont.resume(it) },
            onFailure = { cont.resumeWithException(it) },
        ))
    }

fun verificationHandler(
    onFailure: (exception: Exception) -> Unit = {},
    onSuccess: (CognitoUserCodeDeliveryDetails) -> Unit = {},
): VerificationHandler =
    object : VerificationHandler {
        override fun onSuccess(verificationCodeDeliveryMedium: CognitoUserCodeDeliveryDetails) =
            onSuccess(verificationCodeDeliveryMedium)

        override fun onFailure(exception: Exception) =
            onFailure(exception)
    }
