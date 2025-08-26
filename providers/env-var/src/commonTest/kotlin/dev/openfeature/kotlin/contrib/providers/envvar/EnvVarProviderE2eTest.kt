package dev.openfeature.kotlin.contrib.providers.envvar

import dev.openfeature.kotlin.sdk.Client
import dev.openfeature.kotlin.sdk.OpenFeatureAPI
import dev.openfeature.kotlin.sdk.Reason
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

// The TEST_ENVIRONMENT_VARIABLE environment variable is defined in `build.gradle.kts`
private const val EXISTING_FEATURE_FLAG_NAME = "test.environment.variable"
private const val EXISTING_FEATURE_FLAG_VALUE = "foo"
private const val NON_EXISTING_FEATURE_FLAG_NAME = "non.existing.test.environment.variable"
private const val DEFAULT_VALUE = "default value"

class EnvVarProviderE2eTest {
    // Converts `test.environment.variable` to `TEST_ENVIRONMENT_VARIABLE`
    private val environmentKeyTransformer =
        EnvironmentKeyTransformer
            .toUpperCaseTransformer()
            .andThen(EnvironmentKeyTransformer.replaceDotWithUnderscoreTransformer())

    private val provider =
        EnvVarProvider(
            platformSpecificEnvironmentGateway(),
            environmentKeyTransformer,
        )

    @Test
    fun `should return feature flag value if present`() =
        withClient { client ->
            val result = client.getStringDetails(EXISTING_FEATURE_FLAG_NAME, DEFAULT_VALUE)
            assertEquals(EXISTING_FEATURE_FLAG_VALUE, result.value)
            assertEquals(Reason.STATIC.toString(), result.reason)
        }

    @Test
    fun `should return default value if feature flag is not present`() =
        withClient { client ->
            val result = client.getStringDetails(NON_EXISTING_FEATURE_FLAG_NAME, DEFAULT_VALUE)
            assertEquals(DEFAULT_VALUE, result.value)
            assertEquals(Reason.ERROR.toString(), result.reason)
        }

    private fun withClient(callback: (client: Client) -> Unit) =
        runTest {
            try {
                OpenFeatureAPI.setProviderAndWait(provider)
                val client = OpenFeatureAPI.getClient()
                callback(client)
            } finally {
                OpenFeatureAPI.shutdown()
            }
        }
}
