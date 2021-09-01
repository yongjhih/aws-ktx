package awx.lambda.moshi

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaDataBinder
import com.squareup.moshi.*
import okio.*

fun BufferedSource.jsonReader(): JsonReader = JsonReader.of(this)

class LambdaMoshiBinder(private val moshi: Moshi = Moshi.Builder()
    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
    .build()) : LambdaDataBinder {
    override fun <T> deserialize(content: ByteArray?, clazz: Class<T>): T? =
        content?.inputStream()?.source()?.buffer()?.jsonReader()?.use { reader ->
            moshi.adapter(clazz).fromJson(reader)
        }

    override fun serialize(obj: Any): ByteArray? = null
}