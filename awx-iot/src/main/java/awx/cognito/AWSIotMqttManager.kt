package awx.cognito

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttMessageDeliveryCallback.MessageDeliveryStatus
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos
import com.amazonaws.mobileconnectors.iot.AWSIotMqttSubscriptionStatusCallback
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import java.security.KeyStore
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalCoroutinesApi::class)
fun AWSIotMqttManager.subscribe(
    topic: String,
    qos: AWSIotMqttQos,
    autoUnsubscribe: Boolean = true,
    onUnsubscribeException: suspend (Throwable) -> Unit = {},
    onSubscribed: suspend () -> Unit = {},
): Flow<Pair<String, ByteArray>> =
    callbackFlow {
        try {
            subscribeToTopic(
                topic,
                qos,
                mqttSubscriptionStatusCallback(
                    onFailure = { cancel(CancellationException(it)) },
                    onSuccess = { launch { onSubscribed() } },
                ),
            ) { topic, data -> trySendBlocking(topic to data) }
            awaitCancellation()
        } finally {
            if (autoUnsubscribe) {
                try { unsubscribeTopic(topic) }
                catch (e: Exception) { onUnsubscribeException(e) }
            }
        }
    }

fun mqttSubscriptionStatusCallback(
    onFailure: (Throwable) -> Unit = {},
    onSuccess: () -> Unit = {},
): AWSIotMqttSubscriptionStatusCallback =
    object : AWSIotMqttSubscriptionStatusCallback {
        override fun onSuccess() = onSuccess()
        override fun onFailure(exception: Throwable) = onFailure(exception)
    }

/**
 * Allow auto-disconnect
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun AWSIotMqttManager.connect(
    credentialsProvider: AWSCredentialsProvider,
    autoDisconnect: Boolean = true,
): Flow<AWSIotMqttClientStatus> =
    callbackFlow {
        try {
            connect(credentialsProvider) { status, e ->
                if (e != null) cancel(CancellationException(e))
                else trySendBlocking(status)
            }
            awaitCancellation()
        } finally {
            if (autoDisconnect) disconnect()
        }
    }

@OptIn(ExperimentalCoroutinesApi::class)
fun AWSIotMqttManager.connect(
    keyStore: KeyStore,
    autoDisconnect: Boolean = true,
): Flow<AWSIotMqttClientStatus> =
    callbackFlow {
        try {
            connect(keyStore) { status, e ->
                if (e != null) cancel(CancellationException(e))
                else trySendBlocking(status)
            }
            awaitCancellation()
        } finally {
            if (autoDisconnect) disconnect()
        }
    }

/**
 * Allow auto-disconnect
 */
@OptIn(FlowPreview::class)
suspend fun <R> AWSIotMqttManager.inConnection(
    credentialsProvider: AWSCredentialsProvider,
    onConnectionChanged: suspend (AWSIotMqttClientStatus) -> Unit = {},
    block: suspend AWSIotMqttManager.() -> R,
): R? =
    connect(credentialsProvider)
        .onEach(onConnectionChanged)
        .distinctUntilChanged()
        .flatMapConcat {
            if (it == AWSIotMqttClientStatus.ConnectionLost) emptyFlow()
            else flowOf(it)
        }
        .filter { it == AWSIotMqttClientStatus.Connected }
        .take(1)
        .map { block() }
        .firstOrNull()

suspend fun AWSIotMqttManager.publish(
    data: ByteArray,
    topic: String,
    qos: AWSIotMqttQos,
    userData: Any? = null,
): MessageDeliveryStatus =
    suspendCoroutine { cont ->
        publishData(
            data,
            topic,
            qos,
            { status, _ ->
                if (status == MessageDeliveryStatus.Fail) {
                    cont.resumeWithException(RuntimeException())
                } else {
                    cont.resume(status)
                }
            },
            userData,
        )
    }

suspend fun AWSIotMqttManager.publish(
    data: String,
    topic: String,
    qos: AWSIotMqttQos,
    userData: Any? = null,
): MessageDeliveryStatus =
    publish(data.toByteArray(), topic, qos, userData)