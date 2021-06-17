# Aws Android SDK Kotlin function extensions

## Usage

```kt
// Auto discoonnect and auto unsubscribe when coroutine canceled
mqttManager.connect(credentialsProvider)
    .distinctUntilChanged()
    .filter { it == AWSIotMqttClientStatus.Connected }
    .take(1)
    .flatMapConcat {
        mqttManager
            .subscribeToTopic(
                "topic",
                AWSIotMqttQos.QOS0,
            )
            .map { String(it.second, StandardCharsets.UTF_8) }
     }
     .onEach { println(it) }
```
