package com.amazonaws.mobileconnectors.cognitoidentityprovider

import android.content.Context
import android.content.res.Resources
import android.os.Build
import androidx.annotation.RawRes
import androidx.test.core.app.ApplicationProvider
import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.auth.BasicSessionCredentials
import com.amazonaws.internal.keyvaluestore.AWSKeyValueStore
import com.amazonaws.internal.keyvaluestore.KeyNotFoundException
import com.amazonaws.regions.Regions
import com.amazonaws.services.cognitoidentityprovider.model.CodeDeliveryDetailsType
import com.amazonaws.services.cognitoidentityprovider.model.SignUpResult
import com.amazonaws.util.StringUtils
import com.google.common.truth.Truth.assertThat
import junit.framework.Assert
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.*
import java.security.KeyStore
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import awx.cognito.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.mockito.kotlin.mock


@RunWith(RobolectricTestRunner::class)
class CognitoTest : CognitoIdentityProviderUnitTestBase() {
    companion object {
        val TEST_USER_POOL = "DummyUserPool"
        val TEST_CLIENT_ID = "DummyClientId"
        val TEST_CLIENT_SECRET = "DummyClientSecret"
        val TEST_PINPOINT_APP_ID = "DummyPinpointAppId"
        val DUMMY_CUSTOM_ENDPOINT = "my-custom-endpoint.amazon.com"
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun test() {
        val testPool = CognitoUserPool(
            ApplicationProvider.getApplicationContext(),
            TEST_USER_POOL,
            TEST_CLIENT_ID,
            TEST_CLIENT_SECRET,
            mock<AmazonCognitoIdentityProvider>(),
        )

        val TEST_IN_USER_ATTRIBUTES_LIST = CognitoUserAttributes().apply {
            addAttribute("email", TEST_USER_EMAIL)
            addAttribute("phone_number", TEST_USER_PHONE)
        }

        val TEST_IN_VALIDATION_DATA = HashMap<String, String>().apply {
            put("DummyAttribute_1", "Value4DummyAttribute_1")
            put("DummyAttribute_2", "Value4DummyAttribute_2")
            put("DummyAttribute_3", "Value4DummyAttribute_3")
        }

        val TEST_REGISTER_USER_RESPONSE = SignUpResult().apply {
            isUserConfirmed = true
            codeDeliveryDetails = TEST_CODE_DELIVERY_DETAIL
        }

        assertThat(testPool.userPoolId).isEqualTo(TEST_USER_POOL)
        assertThat(testPool.clientId).isEqualTo(TEST_CLIENT_ID)
        assertThat(testPool.getUser(null)).isNotNull()
        assertThat(testPool.getUser(null)?.userId).isNull()
        assertThat(testPool.getUser("")).isNotNull()
        assertThat(testPool.getUser("")?.userId).isNull()

        runBlockingTest {
            assertThat(testPool.signUpAsync("example@example.com", "passw0rd").first.userId)
                .isEqualTo("example@example.com")
            assertThat(testPool.getUser("example@example.com").userId)
                .isEqualTo("example@example.com")

            /* TODO mock the auth
            val session = testPool.getUser("example@example.com")
                .getSessionAsync { _, userId ->
                    AuthenticationDetails(userId, "passw0rd", null)
                }
                .first
            println(session)
            */
        }
    }
}

abstract class AWSTestBase {
    class JSONConfiguration(private val mJSONObject: JSONObject) {
        fun getPackageConfigure(packageName: String): JSONObject {
            try {
                return mJSONObject.getJSONObject("packages")
                    .getJSONObject(packageName)
            } catch (configurationFileError: JSONException) {
                throw RuntimeException(
                    "Failed to get configuration for package = " + packageName + " from  " +
                            TEST_CONFIGURATION_FILENAME + ".", configurationFileError
                )
            } catch (configurationFileError: NullPointerException) {
                throw RuntimeException(
                    ("Failed to get configuration for package = " + packageName + " from  " +
                            TEST_CONFIGURATION_FILENAME + "."), configurationFileError
                )
            }
        }

        @get:Throws(KeyNotFoundException::class)
        val accessKey: String
            get() = extractStringByPath("credentials.accessKey")

        @get:Throws(KeyNotFoundException::class)
        val secretKey: String
            get() = extractStringByPath("credentials.secretKey")

        @get:Throws(KeyNotFoundException::class)
        val sessionToken: String
            get() = extractStringByPath("credentials.sessionToken")

        @get:Throws(KeyNotFoundException::class)
        val accountId: String
            get() = extractStringByPath("credentials.accountId")

        @Throws(KeyNotFoundException::class)
        private fun extractStringByPath(path: String): String {
            return extractStringByPath(mJSONObject, path)
        }

        // This is a poor man's implementation of JSONPath, that just handles literals,
        // with the '.' meaning "down one more level." This will break if your key contains a period.
        @Throws(KeyNotFoundException::class)
        private fun extractStringByPath(container: JSONObject, path: String): String {
            val indexOfFirstPeriod = path.indexOf(".")
            if (indexOfFirstPeriod != -1) {
                val firstPortion = path.substring(0, indexOfFirstPeriod)
                val rest = path.substring(indexOfFirstPeriod + 1)
                try {
                    return extractStringByPath(container.getJSONObject(firstPortion), rest)
                } catch (e: JSONException) {
                    throw KeyNotFoundException("could not find $path")
                }
            }
            try {
                return container.getString(path)
            } catch (jsonException: JSONException) {
                throw RuntimeException(
                    ("Failed to get key " + path + " from " + TEST_CONFIGURATION_FILENAME +
                            ", please check that it is correctly formed."), jsonException
                )
            }
        }
    }

    /**
     * An implementation of AWSCredentialProvider that fetches the credentials
     * from test configuration json file.
     */
    internal class JSONCredentialProvider() : AWSCredentialsProvider {
        override fun getCredentials(): AWSCredentials {
            try {
                return BasicSessionCredentials(accessKey, secretKey, sessionToken)
            } catch (e: KeyNotFoundException) {
                throw RuntimeException(e)
            }
        }

        override fun refresh() {}
    }

    /**
     * Asserts that the specified String is not null and not empty.
     *
     * @param s The String to test.
     */
    protected fun assertNotEmpty(s: String) {
        checkNotNull(s)
        check(s.length > 0)
    }

    /**
     * Asserts that the contents in the specified file are exactly equal to the
     * contents read from the specified input stream. The input stream will be
     * closed at the end of this method. If any problems are encountered, or the
     * stream's contents don't match up exactly with the file's contents, then
     * this method will fail the current test.
     *
     * @param expectedFile The file containing the expected contents.
     * @param inputStream The stream that will be read, compared to the expected
     * file contents, and finally closed.
     */
    protected fun assertFileEqualsStream(expectedFile: File?, inputStream: InputStream) {
        try {
            val expectedInputStream: InputStream = FileInputStream(expectedFile)
            assertStreamEqualsStream(expectedInputStream, inputStream)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            error("Expected file doesn't exist: " + e.message)
        }
    }

    protected fun assertStreamEqualsStream(
        expectedInputStream: InputStream,
        inputStream: InputStream
    ) {
        try {
            check(doesStreamEqualStream(expectedInputStream, inputStream))
        } catch (e: IOException) {
            e.printStackTrace()
            error("Error reading from stream: " + e.message)
        }
    }

    protected fun assertFileEqualsFile(expectedFile: File, file: File) {
        if (expectedFile.exists() === false) error("Expected file doesn't exist")
        if (file.exists() === false) error("Testing file doesn't exist")
        Assert.assertEquals(expectedFile.length(), file.length())
        try {
            val expectedInputStream = FileInputStream(expectedFile)
            val testedInputStream = FileInputStream(file)
            assertStreamEqualsStream(expectedInputStream, testedInputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            error("Unable to compare files: " + e.message)
        }
    }

    /**
     * Asserts that the contents in the specified string are exactly equal to
     * the contents read from the specified input stream. The input stream will
     * be closed at the end of this method. If any problems are encountered, or
     * the stream's contents don't match up exactly with the string's contents,
     * then this method will fail the current test.
     *
     * @param expectedString The string containing the expected data.
     * @param inputStream The stream that will be read, compared to the expected
     * string data, and finally closed.
     */
    protected fun assertStringEqualsStream(expectedString: String, inputStream: InputStream) {
        try {
            val expectedInputStream: InputStream = ByteArrayInputStream(
                expectedString.toByteArray(StringUtils.UTF8)
            )
            check(doesStreamEqualStream(expectedInputStream, inputStream))
        } catch (e: IOException) {
            e.printStackTrace()
            error("Error reading from stream: " + e.message)
        }
    }

    /**
     * Returns true if, and only if, the contents read from the specified input
     * streams are exactly equal. Both input streams will be closed at the end
     * of this method.
     *
     * @param expectedInputStream The input stream containing the expected
     * contents.
     * @return True if the two input streams contain the same data.
     * @throws IOException If any problems are encountered comparing the file
     * and stream.
     */
    @Throws(IOException::class)
    protected fun doesStreamEqualStream(
        expectedInputStream: InputStream,
        actualInputStream: InputStream
    ): Boolean {
        var expectedDigest: ByteArray? = null
        var actualDigest: ByteArray? = null
        try {
            expectedDigest = calculateMD5Digest(expectedInputStream)
            actualDigest = calculateMD5Digest(actualInputStream)
            return Arrays.equals(expectedDigest, actualDigest)
        } catch (nse: NoSuchAlgorithmException) {
            throw AmazonClientException(nse.message, nse)
        } finally {
            try {
                expectedInputStream.close()
            } catch (e: Exception) {
            }
            try {
                actualInputStream.close()
            } catch (e: Exception) {
            }
        }
    }

    /**
     * Calculates the MD5 digest for the given input stream and returns it.
     */
    @Throws(NoSuchAlgorithmException::class, IOException::class)
    private fun calculateMD5Digest(`is`: InputStream): ByteArray {
        var bytesRead = 0
        val buffer = ByteArray(2048)
        val md5: MessageDigest = MessageDigest.getInstance("MD5")
        while ((`is`.read(buffer).also { bytesRead = it }) != -1) {
            md5.update(buffer, 0, bytesRead)
        }
        return md5.digest()
    }

    protected fun drainInputStream(inputStream: InputStream): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        try {
            val buffer = ByteArray(1024)
            var bytesRead: Long = 0
            while ((inputStream.read(buffer).also { bytesRead = it.toLong() }) > -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead.toInt())
            }
            return byteArrayOutputStream.toByteArray()
        } catch (e: IOException) {
            throw RuntimeException(e)
        } finally {
            try {
                byteArrayOutputStream.close()
            } catch (e: Exception) {
            }
        }
    }

    /**
     * Returns true if, and only if, the contents in the specified file are
     * exactly equal to the contents read from the specified input stream. The
     * input stream will be closed at the end of this method.
     *
     * @param expectedFile The file containing the expected contents.
     * @param inputStream The stream that will be read, compared to the expected
     * file contents, and finally closed.
     * @throws IOException If any problems are encountered comparing the file
     * and stream.
     */
    @Throws(IOException::class)
    protected fun doesFileEqualStream(expectedFile: File?, inputStream: InputStream): Boolean {
        val expectedInputStream: InputStream = FileInputStream(expectedFile)
        return doesStreamEqualStream(expectedInputStream, inputStream)
    }

    /**
     * Asserts that the specified AmazonServiceException is valid, meaning it
     * has a non-empty, non-null value for its message, requestId, etc.
     *
     * @param e The exception to validate.
     */
    protected fun assertValidException(e: AmazonServiceException) {
        checkNotNull(e.getRequestId())
        check(e.getRequestId().trim().length > 0)
        checkNotNull(e.message)
        check(e.message?.trim()?.length!! > 0)
        checkNotNull(e.getErrorCode())
        check(e.getErrorCode().trim().length > 0)
        checkNotNull(e.getServiceName())
        check(e.getServiceName().startsWith("Amazon"))
    }

    protected fun deleteAllEncryptionKeys() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        appContext.getSharedPreferences(
            "com.amazonaws.android.auth.encryptionkey",
            Context.MODE_PRIVATE
        )
            .edit()
            .clear()
            .apply()
        appContext.getSharedPreferences(
            "CognitoIdentityProviderCache.encryptionkey",
            Context.MODE_PRIVATE
        )
            .edit()
            .clear()
            .apply()
        appContext.getSharedPreferences(
            "com.amazonaws.mobile.client.encryptionkey",
            Context.MODE_PRIVATE
        )
            .edit()
            .clear()
            .apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            try {
                val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)
                val keyAliases: Enumeration<String> = keyStore.aliases()
                while (keyAliases.hasMoreElements()) {
                    val keyAlias: String = keyAliases.nextElement()
                    Assert.assertTrue(keyStore.containsAlias(keyAlias))
                    keyStore.deleteEntry(keyAlias)
                    check(!keyStore.containsAlias(keyAlias))
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                error("Error in deleting encryption keys from the Android KeyStore.")
            }
        }
    }

    companion object {
        protected val TEST_CONFIGURATION_FILENAME = "testconfiguration.json"
        val TAG = AWSTestBase::class.java.simpleName

        /** Shared AWS credentials, loaded from a properties file  */
        var credentials: AWSCredentials? = null
        private var mJSONConfiguration: JSONConfiguration? = null
        val jSONConfiguration: JSONConfiguration?
            get() {
                if (mJSONConfiguration != null) {
                    return mJSONConfiguration
                }
                try {
                    val periodIndex = TEST_CONFIGURATION_FILENAME.indexOf(".")
                    val resourceName = TEST_CONFIGURATION_FILENAME.substring(0, periodIndex)
                    val fileContents = readRawResourceContents(resourceName)
                    mJSONConfiguration = JSONConfiguration(JSONObject(fileContents))
                    return mJSONConfiguration
                } catch (configurationFileError: JSONException) {
                    throw RuntimeException(
                        "Failed to read " + TEST_CONFIGURATION_FILENAME + " please check that it is correctly formed.",
                        configurationFileError
                    )
                }
            }

        private fun readRawResourceContents(rawResourceName: String): String {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val resources: Resources = context.resources
            val packageName = context.packageName
            @RawRes val resourceId: Int =
                resources.getIdentifier(rawResourceName, "raw", packageName)
            val inputStream: InputStream = resources.openRawResource(resourceId)
            val `in` = Scanner(inputStream)
            val sb = StringBuilder()
            while (`in`.hasNextLine()) {
                sb.append(`in`.nextLine())
            }
            `in`.close()
            return sb.toString()
        }

        @get:Throws(KeyNotFoundException::class)
        val accessKey: String
            get() = jSONConfiguration!!.accessKey

        @get:Throws(KeyNotFoundException::class)
        val secretKey: String
            get() = jSONConfiguration!!.secretKey

        @get:Throws(KeyNotFoundException::class)
        val sessionToken: String
            get() = jSONConfiguration!!.sessionToken

        @get:Throws(KeyNotFoundException::class)
        val accountId: String
            get() = jSONConfiguration!!.accountId

        fun getPackageConfigure(packageName: String): JSONObject {
            val configuration = jSONConfiguration?.getPackageConfigure(packageName)
            checkNotNull(configuration) {
                ("No configuration for package " + packageName + ". Did you include a " +
                        "tesconfiguration.json with the test package?")
            }
            return configuration
        }

        fun setUpCredentials() {
            if (credentials == null) {
                val provider: AWSCredentialsProvider = JSONCredentialProvider()
                val chain = AWSCredentialsProviderChain(provider)
                credentials = chain.getCredentials()
            }
        }
    }
}

abstract class CognitoIdentityProviderUnitTestBase : AWSTestBase() {
    // App Context
    protected var appContext: Context

    companion object {
        // Test values
        const val TEST_USER_POOL = "alpha_DummyID"
        const val TEST_CLIENT_ID = "DummyClientId"
        const val TEST_CLIENT_SECRET = "DummyClientSecret"
        const val TEST_USER_NAME = "DummyUserName"
        const val TEST_USER_NAME_2 = "DummyUserName_2"
        const val TEST_USER_PASSWORD = "DummyTestPAssword"
        const val TEST_USER_NEW_PASS = "DummyNewTestPassword"
        const val TEST_USER_GN = "DummyUserGivenName"
        const val TEST_USER_FN = "DummyUserFamilyName"
        const val TEST_USER_PHONE = "1234567890"
        const val TEST_USER_EMAIL = "DummyEmail@exmaple.com"
        const val TEST_CACHED_ATOKEN = "DummyCachedAccessToken"
        const val TEST_CACHED_ITOKEN = "DummyCachedIdToken"
        const val TEST_CACHED_RTOKEN = "DummyCachedRefToken"
        const val TEST_NEW_ATOKEN = "DummyNewAccessToken"
        const val TEST_NEW_ITOKEN = "DummyNewIdToken"
        const val TEST_NEW_RTOKEN = "DummyNewRefToken"
        const val TEST_CODE = "123456"
        const val TEST_ENHANCED_SEC = "VeryEnhancedSecret!"
        const val TEST_CODE_DESTINA = "DummyCodeDeliveryDestination"
        const val TEST_CODE_DEL_MED = "DummyCodeDeliveryMEDIUM"
        const val TEST_CODE_ATTRIBU = "DummyCodeDeliveryAttribute"
        const val TEST_DEVICE_KEY = "DummyDeviceKey"
        const val TEST_DEV_GRP_KEY = "DummyDeviceGroupKey"
        const val TEST_DEV_VERIFIR = "DummyDeviceVerifier"
        const val TEST_PP_APP_ID_1 = "this_is_a_random_pp_app_Id"
        const val TEST_PP_APP_ID_2 = "this_is_another_random_pp_app_Id"
        const val TEST_PP_ENDPOINT_1 = "a_random_pp_endpoint"
        const val TEST_PP_ENDPOINT_2 = "another_random_pp_endpoint"
        val TEST_AWS_REGION_1: Regions = Regions.US_EAST_1
        val TEST_AWS_REGION_2: Regions = Regions.US_EAST_2

        // SDK constants for PP integration
        const val PP_PREFERENCES_AND_FILE_MANAGER_SUFFIX = "515d6767-01b7-49e5-8273-c8d11b0f331d"
        const val PP_UNIQUE_ID_KEY = "UniqueId"

        // High level constructs
        var TEST_CODE_DELIVERY_DETAIL: CodeDeliveryDetailsType? = null

        init {
            // Code delivery details
            TEST_CODE_DELIVERY_DETAIL = CodeDeliveryDetailsType()
            TEST_CODE_DELIVERY_DETAIL?.setDestination(TEST_CODE_DESTINA)
            TEST_CODE_DELIVERY_DETAIL?.setDeliveryMedium(TEST_CODE_DEL_MED)
            TEST_CODE_DELIVERY_DETAIL?.setAttributeName(TEST_CODE_ATTRIBU)
        }
    }

    fun getAWSKeyValueStorageUtility(cognitoUserPool: CognitoUserPool): AWSKeyValueStore {
        return cognitoUserPool.awsKeyValueStore
    }

    init {
        appContext = ApplicationProvider.getApplicationContext()
    }
}