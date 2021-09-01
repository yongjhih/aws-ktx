package awx.lambda.moshi

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaDataBinder
import com.squareup.moshi.*
import okio.*

fun BufferedSource.jsonReader(): JsonReader = JsonReader.of(this)

class LambdaMoshiBinder(private val moshi: Moshi = Moshi.Builder()
    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
    .build()) : LambdaDataBinder {
    override fun <T> deserialize(content: ByteArray?, clazz: Class<T>): T? =
        content?.inputStream()?.source()?.buffer()?.jsonReader()?.apply {
            isLenient = true
        }?.use { reader ->
            moshi.adapter(clazz).fromJson(reader)
        }

    @OptIn(ExperimentalStdlibApi::class)
    override fun serialize(obj: Any?): ByteArray? =
        try {
            moshi.adapter<Any>().toJsonValue(obj).toString().toByteArray()
        } catch (e: Exception) {
            null
        }
}