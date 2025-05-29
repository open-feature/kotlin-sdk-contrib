package dev.openfeature.kotlin.contrib.providers.envvar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Defined in build.gradle
private const val EXISTING_ENVIRONMENT_VARIABLE_NAME = "TEST_ENVIRONMENT_VARIABLE"
private const val EXISTING_ENVIRONMENT_VARIABLE_VALUE = "foo"
private const val NON_EXISTING_ENVIRONMENT_VARIABLE_NAME = "NON_EXISTING_ENVIRONMENT_VARIABLE"

class PlatformSpecificEnvironmentGatewayTest {
    @Test
    fun `should return existing environment variable`() {
        val gateway = platformSpecificEnvironmentGateway()
        val result = gateway.getEnvironmentVariable(EXISTING_ENVIRONMENT_VARIABLE_NAME)
        assertEquals(EXISTING_ENVIRONMENT_VARIABLE_VALUE, result)
    }

    @Test
    fun `should return null for non-existing environment variable`() {
        val gateway = platformSpecificEnvironmentGateway()
        val result = gateway.getEnvironmentVariable(NON_EXISTING_ENVIRONMENT_VARIABLE_NAME)
        assertNull(result)
    }
}
