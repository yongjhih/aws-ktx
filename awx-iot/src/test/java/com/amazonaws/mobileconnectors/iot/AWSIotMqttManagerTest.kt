package com.amazonaws.mobileconnectors.iot

import awx.cognito.connect
import awx.cognito.inConnection
import awx.cognito.publish
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runBlockingTest
import org.eclipse.paho.client.mqttv3.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.security.KeyPair


@OptIn(
    ExperimentalCoroutinesApi::class,
    ObsoleteCoroutinesApi::class,
)
@RunWith(RobolectricTestRunner::class)
class AWSIotMqttManagerTest {

    // This certificate is an invalid (to AWS IoT) certificate for unit testing only.
    private val testCert = """
        -----BEGIN CERTIFICATE-----
        MIIDlTCCAn2gAwIBAgIVAKuR4L6GajQRv1DzXlUFigMoiwzsMA0GCSqGSIb3DQEB
        CwUAME0xSzBJBgNVBAsMQkFtYXpvbiBXZWIgU2VydmljZXMgTz1BbWF6b24uY29t
        IEluYy4gTD1TZWF0dGxlIFNUPVdhc2hpbmd0b24gQz1VUzAeFw0xNTA5MTUwMDEz
        MjhaFw00OTEyMzEyMzU5NTlaMFkxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJXQTEQ
        MA4GA1UEBxMHU2VhdHRsZTEPMA0GA1UEChMGQW1hem9uMQwwCgYDVQQLEwNBV1Mx
        DDAKBgNVBAMTA1NESzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALrx
        n1ZGjBDdasdmuJh8F/KxhMSB/u7f8olGaoPtkvFHkzCf3sXqoHVxzYITzWH8UlmM
        hNJ0CaRfcT/Dmi0PDxPrPQLR1/MjV9WpHTLfI2kA+PI+d4LnnlYQYnQc9wgZIX2c
        +D0mA12By8BRduwM3rDAULmwjjfFX/MLLkDDng+mEIMjXOZnWjMJ3dorSzajVP2C
        bWt1JMRGoSjY2fQVBc2JzU+7y9s9fxMO5329Hne1E8bNVZd+rHJKlJhvIWJCAoya
        NnF4whXMp+UHGPJdhHQCnSPbX5r2c2UdDL/1bftNlX6grCmivuIv6qw+dtA4V7pc
        lsMSEt9zFrVJ6VkZXbUCAwEAAaNgMF4wHwYDVR0jBBgwFoAULXQSP9o80neirjAW
        SlF+yZWjLh0wHQYDVR0OBBYEFCDoMMxiSPyy4D6a5qhg+6FXZtMtMAwGA1UdEwEB
        /wQCMAAwDgYDVR0PAQH/BAQDAgeAMA0GCSqGSIb3DQEBCwUAA4IBAQAtlG5ytjMN
        c95dlafQVhPoAKEJ0JkDYl3ZmbNYHXgTQfG08a8zFQLLECODiiO/5HyNaAI3Pzc3
        M580RijF/D23XUHLCvVxaeZgQnJbs+xmHPIeFxCiGfBXBTET3IZApXW2V92dcZf3
        Pccbfemdl7t7KycuBNszbTsCZygg5sq1NTCF0ZkSGuJfmbjO9YBY2bV8H66pNdCq
        72nhlD7w3fTcfpo8rmz7CzNTVg9bGILUnr7WiaC3nCxsM1EiPH/JRGSKrbA2/96B
        7OWv9idOJbp/fKdub3lqzWwPtMwLWAyM/sevEqQbWOvH3sfPafYYp3iwAQmFdCJG
        0zaDUnQHDFV8
        -----END CERTIFICATE-----
        """.trimIndent()

    companion object {
        const val CERT_ID = "unittest"
        const val KEYSTORE_PATH = "./"
        const val KEYSTORE_NAME = "unit_test_keystore"
        const val KEYSTORE_PASSWORD = "test"
        const val TEST_ENDPOINT_PREFIX = "ABCDEFG"
        const val TEST_ENDPOINT = "${TEST_ENDPOINT_PREFIX}.iot.us-east-1.amazonaws.com"
    }

    @Test
    fun testConnect(): Unit = runBlockingTest {
        val mockClient = MockMqttClient()
        val testClient = AWSIotMqttManager("test-client",
            Region.getRegion(Regions.US_EAST_1), TEST_ENDPOINT_PREFIX).apply {
            setMqttClient(mockClient)
        }
        val testKeystore = AWSIotKeystoreHelper.getIotKeystore(
            CERT_ID,
            KEYSTORE_PATH,
            KEYSTORE_NAME,
            KEYSTORE_PASSWORD,
        )

        val statuses: MutableList<AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus> = mutableListOf()
        val scope = async {
            testClient.connect(testKeystore)
                .onEach { statuses.add(it) }
                .first { it == AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected }
        }
        mockClient.mockConnectSuccess()
        scope.await()

        assertThat(mockClient.connectCalls).isEqualTo(1)
        assertThat(mockClient.mostRecentOptions?.isCleanSession).isTrue()
        assertThat(mockClient.mostRecentOptions?.keepAliveInterval).isEqualTo(300)
        assertThat(statuses).isEqualTo(listOf(
            AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connecting,
            AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected,
        ))
    }

    @Before
    fun setUp() {
        val testKP: KeyPair = AWSIotKeystoreHelper.generatePrivateAndPublicKeys()
        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(
            CERT_ID, testCert, testKP.private,
            KEYSTORE_PATH, KEYSTORE_NAME, KEYSTORE_PASSWORD
        )
    }

    @After
    fun tearDown() {
        val keystoreFile = File(KEYSTORE_PATH, KEYSTORE_NAME)
        if (keystoreFile.exists()) {
            keystoreFile.delete()
        }
    }

    @Test
    fun testPublish(): Unit = runBlockingTest {
        val mockClient = MockMqttClient()
        val testClient = AWSIotMqttManager("test-client",
            Region.getRegion(Regions.US_EAST_1), TEST_ENDPOINT_PREFIX).apply {
            setMqttClient(mockClient)
        }
        val testKeystore = AWSIotKeystoreHelper.getIotKeystore(
            CERT_ID,
            KEYSTORE_PATH,
            KEYSTORE_NAME,
            KEYSTORE_PASSWORD,
        )
        val scope = async {
            val status = testClient.inConnection(keyStore = testKeystore) {
                publish("Hello, world!", "#", AWSIotMqttQos.QOS0)
            }
            assertThat(status).isEqualTo(AWSIotMqttMessageDeliveryCallback.MessageDeliveryStatus.Success)
        }
        mockClient.mockConnectSuccess()
        mockClient.mockPublishSuccess("#", MqttMessage("Hello, world!".toByteArray()))
        scope.await()

        assertThat(mockClient.publishCalls).isEqualTo(1)
        assertThat(mockClient.mostRecentPublishTopic).isEqualTo("#")
        assertThat(mockClient.mostRecentPublishPayload?.decodeToString()).isEqualTo("Hello, world!")
    }
}

class MockMqttClient : MqttAsyncClient(
    "wss://mockendpoint.example.com", "mock-id",
) {
    var connectCalls = 0
    var publishCalls = 0
    var subscribeCalls = 0
    var unsubscribeCalls = 0
    var disconnectCalls = 0
    var mostRecentOptions: MqttConnectOptions? = null
    var mostRecentPublishTopic: String? = null
    var mostRecentPublishPayload: ByteArray? = null
    var mostRecentPublishQoS = 0
    var mostRecentPublishRetained = false
    var mostRecentPublishUserContext: Any? = null
    var mostRecentPublishCallback: IMqttActionListener? = null
    var isConnected: Boolean? = false
    var mockCallback: MqttCallback? = null
    var mockConnectionStatusCallback: IMqttActionListener? = null
    var mockSubscriptionStatusCallback: IMqttActionListener? = null
    var throwsExceptionOnConnect: Boolean
    var connectException: MqttException?
    var throwsExceptionOnPublish: Boolean
    var throwsExceptionOnSubscribe: Boolean
    var throwsExceptionOnUnsubscribe: Boolean
    var throwsExceptionOnDisconnect: Boolean
    var mockSubscriptions: HashMap<String, Int> = HashMap()
    var testToken: IMqttToken = MqttToken("unit-test")
    var testDeliveryToken: IMqttDeliveryToken = MqttDeliveryToken()

    @Throws(MqttException::class, MqttSecurityException::class)
    override fun connect(
        options: MqttConnectOptions,
        userContext: Any?,
        callback: IMqttActionListener?,
    ): IMqttToken {
        if (throwsExceptionOnConnect && connectException != null) {
            throw connectException!!
        }
        ++connectCalls
        mostRecentOptions = options
        mockConnectionStatusCallback = callback
        return testToken
    }

    @Throws(MqttException::class)
    override fun disconnect(quiesceTimeout: Long): IMqttToken {
        if (throwsExceptionOnDisconnect) {
            throw MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION.toInt())
        }
        ++disconnectCalls
        return testToken
    }

    @Throws(MqttException::class)
    override fun publish(
        topic: String,
        payload: ByteArray,
        qos: Int,
        retained: Boolean,
    ): IMqttDeliveryToken {
        if (throwsExceptionOnPublish) {
            throw MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION.toInt())
        }
        ++publishCalls
        mostRecentPublishTopic = topic
        mostRecentPublishPayload = payload
        mostRecentPublishQoS = qos
        mostRecentPublishRetained = retained
        return testDeliveryToken
    }

    @Throws(MqttException::class)
    override fun publish(
        topic: String,
        payload: ByteArray,
        qos: Int,
        retained: Boolean,
        userContext: Any?,
        callback: IMqttActionListener?,
    ): IMqttDeliveryToken {
        if (throwsExceptionOnPublish) {
            throw MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION.toInt())
        }
        ++publishCalls
        mostRecentPublishTopic = topic
        mostRecentPublishPayload = payload
        mostRecentPublishQoS = qos
        mostRecentPublishRetained = retained
        mostRecentPublishUserContext = userContext
        mostRecentPublishCallback = callback
        return testDeliveryToken
    }

    @Throws(MqttException::class)
    override fun subscribe(topicFilter: String, qos: Int): IMqttToken {
        if (throwsExceptionOnSubscribe) {
            throw MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION.toInt())
        }
        ++subscribeCalls
        mockSubscriptions[topicFilter] = qos
        return testToken
    }

    @Throws(MqttException::class)
    override fun subscribe(
        topicFilter: String, qos: Int, userContext: Any,
        callback: IMqttActionListener
    ): IMqttToken {
        if (throwsExceptionOnSubscribe) {
            throw MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION.toInt())
        }
        ++subscribeCalls
        mockSubscriptionStatusCallback = callback
        mockSubscriptions[topicFilter] = qos
        callback.onSuccess(testToken)
        return testToken
    }

    @Throws(MqttException::class)
    override fun unsubscribe(topicFilter: String): IMqttToken {
        if (throwsExceptionOnUnsubscribe) {
            throw MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION.toInt())
        }
        ++unsubscribeCalls
        mockSubscriptions.remove(topicFilter)
        return testToken
    }

    override fun setCallback(callback: MqttCallback) {
        mockCallback = callback
    }

    fun mockConnectSuccess() {
        mockConnectionStatusCallback!!.onSuccess(testToken)
        isConnected = true
    }

    fun mockPublishSuccess(topic: String = "",
                           message: MqttMessage? = null,
                           token: IMqttDeliveryToken? = null,
                           userData: Any? = null,
    ) {
        mockCallback!!.messageArrived(topic, message)
        mockCallback!!.deliveryComplete(token)
        val userContext = mostRecentPublishUserContext as? PublishMessageUserData
        userContext?.userCallback?.statusChanged(
            AWSIotMqttMessageDeliveryCallback.MessageDeliveryStatus.Success,
            userData)
    }

    fun mockConnectFail() {
        mockConnectionStatusCallback!!.onFailure(testToken, Exception("fail"))
        isConnected = false
    }

    fun mockDisconnect() {
        isConnected = false
        mockCallback!!.connectionLost(Exception("disconnect"))
    }

    init {
        isConnected = false
        throwsExceptionOnConnect = false
        connectException = null
        throwsExceptionOnPublish = false
        throwsExceptionOnSubscribe = false
        throwsExceptionOnUnsubscribe = false
        throwsExceptionOnDisconnect = false
    }
}