package awx.lambda.moshi

import com.google.common.truth.Truth
import com.squareup.moshi.JsonClass
import org.junit.Test

@JsonClass(generateAdapter = false)
data class LoggedInUser(
    val username: String,
    val federatedIdentity: String,
    val externalProviderId: String,
)

class LambdaMoshiBinderTest {
    @Test
    fun test() {
        val binder = LambdaMoshiBinder()
        Truth.assertThat(
            binder.deserialize("""
            {
                "username": "username0",
                "federatedIdentity": "federatedIdentity0",
                "externalProviderId": "externalProviderId0"
            }
            """.trimIndent().toByteArray(), LoggedInUser::class.java)
        ).isEqualTo(
            LoggedInUser(
                username = "username0",
                federatedIdentity = "federatedIdentity0",
                externalProviderId = "externalProviderId0",
            )
        )

        // Doesn't support
        //LoggedInUser(
        //    username = "username0",
        //    federatedIdentity = "federatedIdentity0",
        //    externalProviderId = "externalProviderId0",
        //).let { user ->
        //    assertThat(binder.deserialize(binder.serialize(user), LoggedInUser::class.java)).isEqualTo(user)
        //}
    }
}
