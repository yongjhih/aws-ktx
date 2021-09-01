package awx.lambda.kotlinx.serialization

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaDataBinder
import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@ExperimentalSerializationApi
class LambdaKotlinxSerializationBinder(private val stringFormat: StringFormat = Json {}) : LambdaDataBinder {
    override fun <T> deserialize(content: ByteArray?, clazz: Class<T>): T? =
        content?.toString(Charsets.UTF_8)?.let { stringFormat.decodeFromString(it, clazz) }

    override fun serialize(obj: Any): ByteArray =
        stringFormat.encodeToString(obj).toByteArray()
}

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalSerializationApi::class)
fun <T> StringFormat.encodeToString(value: T, clazz: Class<T>): String =
    encodeToString(serializersModule.serializer(clazz) as SerializationStrategy<T>, value)

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalSerializationApi::class)
fun <T> StringFormat.decodeFromString(string: String, clazz: Class<T>): T =
    decodeFromString(serializersModule.serializer(clazz) as DeserializationStrategy<T>, string)
