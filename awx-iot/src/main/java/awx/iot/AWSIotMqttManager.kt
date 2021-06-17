package awx.iot

import com.amazonaws.auth.AWSCredentialsProvider
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttMessageDeliveryCallback.MessageDeliveryStatus
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos
import com.amazonaws.mobileconnectors.iot.AWSIotMqttSubscriptionStatusCallback
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow
import java.lang.RuntimeException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalCoroutinesApi::class)
fun AWSIotMqttManager.subscribe(
    topic: String,
    qos: AWSIotMqttQos,
    onUnsubscribeException: (Throwable) -> Unit = {},
    onSubscribed: () -> Unit = {},
): Flow<Pair<String, ByteArray>> =
    callbackFlow {
        try {
            subscribeToTopic(
                topic,
                qos,
                mqttSubscriptionStatusCallback(
                    onFailure = { cancel(CancellationException(it)) },
                    onSubscribed,
                ),
            ) { topic, data -> sendBlocking(topic to data) }
            awaitCancellation()
        } finally {
            try { unsubscribeTopic(topic) }
            catch (e: Exception) { onUnsubscribeException(e) }
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
): Flow<AWSIotMqttClientStatus> =
    callbackFlow {
        try {
            connect(credentialsProvider) { status, e ->
                if (e != null) cancel(CancellationException(e))
                else sendBlocking(status)
            }
            awaitCancellation()
        } finally {
            disconnect()
        }
    }

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