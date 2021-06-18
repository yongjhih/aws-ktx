[![Apache 2.0 License](https://img.shields.io/badge/license-Apache%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Release](https://jitpack.io/v/yongjhih/aws-ktx.svg)](https://jitpack.io/#yongjhih/aws-ktx)

# AWS Android SDK Kotlin function extensions with coroutines

## Installation

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.yongjhih.aws-ktx:awx-iot:-SNAPSHOT'
    implementation 'com.github.yongjhih.aws-ktx:awx-cognito:-SNAPSHOT'
}
```

## Usage

Allow disconnect and unsubscribe when the view lifecycle eneded.

Before:

```kt
mqttManager.connect(credentialsProvider) { status, _ ->
    if (status == AWSIotMqttClientStatus.Connected) {
        mqttManager.subscribeToTopic(
            topic,
            AWSIotMqttQos.QOS0,
            object : AWSIotMqttSubscriptionStatusCallback {
                override fun onSuccess() {
                }

                override fun onFailure(exception: Throwable) {
                    mqttManager.disconnect()
                }
            }) { _, data ->
                println(String(data, StandardCharsets.UTF_8))
            }
    }
}

override fun onDestroyView() {
    super.onDestroyView()
    mqttManager.disconnect()
}
```

After:

```kt
 viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
     // Auto discoonnect and auto unsubscribe when coroutine canceled
     mqttManager.inConnection(credentialsProvider) {
         mqttManager.subscribe(topic, AWSIotMqttQos.QOS0)
             .map { String(it.second, StandardCharsets.UTF_8) }
             .onEach { println(it) }
     }
 }
```

or

```kt
viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
    // Auto discoonnect and auto unsubscribe when coroutine canceled
    mqttManager.connect(credentialsProvider)
        .distinctUntilChanged()
        .filter { it == AWSIotMqttClientStatus.Connected }
        .take(1)
        .flatMapConcat { mqttManager.subscribe(topic, AWSIotMqttQos.QOS0) }
        .map { String(it.second, StandardCharsets.UTF_8) }
        .onEach { println(it) }
}
```

If we just need a first message from the topic, then unsubscribe and disconnect.


Before:

```kt
 mqttManager.connect(credentialsProvider) { status, _ ->
     if (status == AWSIotMqttClientStatus.Connected) {
         mqttManager.subscribeToTopic(
             topic,
             AWSIotMqttQos.QOS0,
             object : AWSIotMqttSubscriptionStatusCallback {
                 override fun onSuccess() {
                 }
 
                 override fun onFailure(exception: Throwable) {
                    mqttManager.disconnect()
                 }
             }) { topic, data ->
             println(String(data, StandardCharsets.UTF_8))
+            mqttManager.unsubscribeTopic(topic)
+            mqttManager.disconnect()
         }
     }
 }
 
 override fun onDestroyView() {
     super.onDestroyView()
     mqttManager.disconnect()
 }
```

After:

```kt
 viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
     // Auto discoonnect and auto unsubscribe when coroutine canceled
     mqttManager.inConnection(credentialsProvider) {
         mqttManager.subscribe(topic, AWSIotMqttQos.QOS0)
             .map { String(it.second, StandardCharsets.UTF_8) }
+            .take(1)
             .onEach { println(it) }
     }
 }
```

or

```kt
 viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
     // Auto discoonnect and auto unsubscribe when coroutine canceled
     mqttManager.connect(credentialsProvider)
         .distinctUntilChanged()
         .filter { it == AWSIotMqttClientStatus.Connected }
         .take(1)
         .flatMapConcat { mqttManager.subscribe(topic, AWSIotMqttQos.QOS0) }
         .map { String(it.second, StandardCharsets.UTF_8) }
+        .take(1)
         .onEach { println(it) }
 }
```

You can also get the first message concisely:


```kt
viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
    // Auto discoonnect and auto unsubscribe when coroutine canceled
    val firstMessage = mqttManager.inConnection(credentialsProvider) {
            mqttManager.subscribe(topic, AWSIotMqttQos.QOS0)
                .map { String(it.second, StandardCharsets.UTF_8) }
                .first()
        }

    println(firstMessage)
}
```

or

```kt
viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
    // Auto discoonnect and auto unsubscribe when coroutine canceled
    val firstMessage = mqttManager.connect(credentialsProvider)
            .distinctUntilChanged()
            .filter { it == AWSIotMqttClientStatus.Connected }
            .take(1)
            .flatMapConcat { mqttManager.subscribe(topic, AWSIotMqttQos.QOS0) }
            .map { String(it.second, StandardCharsets.UTF_8) }
            .first()

   println(firstMessage)
}
```



CognitoUser.getSession()

Before:


```kt
cognitoUserPool.getUser(username).getSessionInBackground(object : AuthenticationHandler {
    override fun onSuccess(
        userSession: CognitoUserSession,
        newDevice: CognitoDevice,
    ) {
	println(userSession.idToken?.jwtToken)
    }

    override fun getAuthenticationDetails(
        authenticationContinuation: AuthenticationContinuation,
        userId: String,
    ) {
        authenticationContinuation.apply {
            setAuthenticationDetails(AuthenticationDetails(userId, password, null))
            continueTask()
        }
    }

    override fun getMFACode(continuation: MultiFactorAuthenticationContinuation) {
    }

    override fun authenticationChallenge(continuation: ChallengeContinuation?) {
    }

    override fun onFailure(exception: Exception) {
    }
})
```

After:

```kt
println(cognitoUserPool.getUser(username).getSessionAsync { _, userId ->
  AuthenticationDetails(userId, password, null)
}.first.idToken?.jwtToken)
```

