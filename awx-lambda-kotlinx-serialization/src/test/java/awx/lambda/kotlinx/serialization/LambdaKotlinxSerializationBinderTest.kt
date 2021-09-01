package awx.lambda.kotlinx.serialization

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.Test

@Serializable
data class LoggedInUser(
    val username: String,
    val federatedIdentity: String,
    val externalProviderId: String,
)

@ExperimentalSerializationApi
class LambdaKotlinxSerializationBinderTest {
    @Test
    fun test() {
        val binder = LambdaKotlinxSerializationBinder()
        assertThat(binder.deserialize("""
            {
                "username": "username0",
                "federatedIdentity": "federatedIdentity0",
                "externalProviderId": "externalProviderId0"
            }
        """.trimIndent().toByteArray(), LoggedInUser::class.java)).isEqualTo(
            LoggedInUser(
                username = "username0",
                federatedIdentity = "federatedIdentity0",
                externalProviderId = "externalProviderId0",
            )
        )

        // KotlinxSerialization doesn't support reflection
        //LoggedInUser(
        //    username = "username0",
        //    federatedIdentity = "federatedIdentity0",
        //    externalProviderId = "externalProviderId0",
        //).let { user ->
        //    assertThat(binder.deserialize(binder.serialize(user), LoggedInUser::class.java)).isEqualTo(user)
        //}
    }
}
