package dev.openfeature.kotlin.contrib.providers.envvar

import dev.openfeature.kotlin.sdk.FeatureProvider
import dev.openfeature.kotlin.sdk.ImmutableContext
import dev.openfeature.kotlin.sdk.ProviderEvaluation
import dev.openfeature.kotlin.sdk.Reason
import dev.openfeature.kotlin.sdk.Value
import dev.openfeature.kotlin.sdk.exceptions.OpenFeatureError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private val REASON_STATIC = Reason.STATIC.toString()

internal class EnvVarProviderTest {
    @Test
    fun `should throw on getObjectEvaluation`() {
        assertFailsWith<OpenFeatureError.GeneralError> {
            EnvVarProvider()
                .getObjectEvaluation("any-key", Value.Null, ImmutableContext())
        }
    }

    @Test
    fun `should evaluate true as Boolean correctly`() =
        evaluationTest(
            "bool_true",
            "true",
            {
                getBooleanEvaluation(
                    "bool_true",
                    false,
                    null,
                )
            },
            { evaluation ->
                evaluationChecks(
                    evaluation,
                    REASON_STATIC,
                    true,
                )
            },
        )

    @Test
    fun `should evaluate false as Boolean correctly`() =
        evaluationTest(
            "bool_false",
            "FaLsE",
            {
                getBooleanEvaluation(
                    "bool_false",
                    true,
                    null,
                )
            },
            { evaluation ->
                evaluationChecks(
                    evaluation,
                    REASON_STATIC,
                    false,
                )
            },
        )

    @Test
    fun `should evaluate unrecognized Boolean as Boolean false correctly`() =
        evaluationTest(
            "bool_false",
            "not-a-bool",
            {
                getBooleanEvaluation(
                    "bool_false",
                    true,
                    null,
                )
            },
            { evaluation ->
                evaluationChecks(
                    evaluation,
                    REASON_STATIC,
                    false,
                )
            },
        )

    @Test
    fun `should evaluate String value correctly`() =
        evaluationTest(
            "string",
            "value",
            {
                getStringEvaluation(
                    "string",
                    "",
                    null,
                )
            },
            { evaluation ->
                evaluationChecks(
                    evaluation,
                    REASON_STATIC,
                    "value",
                )
            },
        )

    @Test
    fun `should evaluate Int value correctly`() =
        evaluationTest(
            "INT",
            "42",
            {
                getIntegerEvaluation(
                    "INT",
                    0,
                    null,
                )
            },
            { evaluation ->
                evaluationChecks(
                    evaluation,
                    REASON_STATIC,
                    42,
                )
            },
        )

    @Test
    fun `should evaluate Double value correctly`() =
        evaluationTest(
            "double",
            "42.0",
            {
                getDoubleEvaluation(
                    "double",
                    0.0,
                    null,
                )
            },
            { evaluation ->
                evaluationChecks(
                    evaluation,
                    REASON_STATIC,
                    42.0,
                )
            },
        )

    @Test
    fun `should throw FlagNotFound on missing Boolean env`() =
        throwingEvaluationTest<OpenFeatureError.FlagNotFoundError, Boolean>(
            "other",
            "other",
        ) {
            getBooleanEvaluation(
                "bool_default",
                true,
                null,
            )
        }

    @Test
    fun `should throw FlagNotFound on missing String env`() =
        throwingEvaluationTest<OpenFeatureError.FlagNotFoundError, String>(
            "other",
            "other",
        ) {
            getStringEvaluation(
                "string_default",
                "value",
                null,
            )
        }

    @Test
    fun `should throw FlagNotFound on missing Int env`() =
        throwingEvaluationTest<OpenFeatureError.FlagNotFoundError, Int>(
            "other",
            "other",
        ) {
            getIntegerEvaluation(
                "int_default",
                42,
                null,
            )
        }

    @Test
    fun `should throw FlagNotFound on missing Double env`() =
        throwingEvaluationTest<OpenFeatureError.FlagNotFoundError, Double>(
            "other",
            "other",
        ) {
            getDoubleEvaluation(
                "double_default",
                42.0,
                null,
            )
        }

    @Test
    fun `should throw on unparseable values`() =
        throwingEvaluationTest<OpenFeatureError.ParseError, Int>(
            "int_incorrect",
            "fourty-two",
        ) {
            getIntegerEvaluation(
                "int_incorrect",
                0,
                null,
            )
        }

    @Test
    fun `should throw FlagNotFound on missing double_incorrect env`() =
        throwingEvaluationTest<OpenFeatureError.ParseError, Double>(
            "double_incorrect",
            "fourty-two",
        ) {
            getDoubleEvaluation(
                "double_incorrect",
                0.0,
                null,
            )
        }

    @Test
    fun `should transform key if configured`() {
        val key = "key.transformed"
        val expected = "key_transformed"

        val transformer = EnvironmentKeyTransformer.replaceDotWithUnderscoreTransformer()
        val gateway = EnvironmentGateway { s -> s }
        val provider = EnvVarProvider(gateway, transformer)

        val environmentVariableValue =
            provider.getStringEvaluation(key, "failed", null).value

        assertEquals(expected, environmentVariableValue)
    }

    @Test
    fun `should use passed-in EnvironmentGateway`() {
        val testFake = EnvironmentGateway { s -> "true" }

        val provider = EnvVarProvider(testFake)

        val actual: Boolean = provider.getBooleanEvaluation("any key", false, null).value

        assertTrue(actual)
    }

    private fun <T> evaluationTest(
        variableName: String,
        value: String,
        callback: FeatureProvider.() -> ProviderEvaluation<T>,
        checks: (ProviderEvaluation<T>) -> Unit,
    ) {
        // Given
        val provider = provider(variableName, value)

        // When
        val evaluation = callback(provider)

        // Then
        checks(evaluation)
    }

    private inline fun <reified T : Throwable, E> throwingEvaluationTest(
        variableName: String?,
        value: String,
        callback: FeatureProvider.() -> ProviderEvaluation<E>,
    ) {
        // Given
        val provider: FeatureProvider = provider(variableName, value)

        // Then
        assertFailsWith<T> {
            // When
            callback(provider)
        }
    }

    private fun provider(
        variableName: String?,
        value: String,
    ): FeatureProvider =
        EnvVarProvider(
            environmentGateway = { name -> if (name == variableName) value else null },
        )

    private fun <T> evaluationChecks(
        evaluation: ProviderEvaluation<T>,
        reason: String?,
        expected: T?,
    ) {
        assertEquals(reason, evaluation.reason)
        assertEquals(expected, evaluation.value)
    }
}
