package dev.openfeature.kotlin.contrib.providers.envvar

import dev.openfeature.kotlin.sdk.EvaluationContext
import dev.openfeature.kotlin.sdk.FeatureProvider
import dev.openfeature.kotlin.sdk.Hook
import dev.openfeature.kotlin.sdk.ProviderEvaluation
import dev.openfeature.kotlin.sdk.ProviderMetadata
import dev.openfeature.kotlin.sdk.Reason
import dev.openfeature.kotlin.sdk.Value
import dev.openfeature.kotlin.sdk.exceptions.OpenFeatureError

/** EnvVarProvider is the Kotlin provider implementation for the environment variables.  */
class EnvVarProvider(
    private val environmentGateway: EnvironmentGateway = platformSpecificEnvironmentGateway(),
    private val keyTransformer: EnvironmentKeyTransformer = EnvironmentKeyTransformer.doNothing(),
) : FeatureProvider {
    override val hooks: List<Hook<*>> = emptyList()
    override val metadata: ProviderMetadata
        get() = Metadata

    override suspend fun initialize(initialContext: EvaluationContext?) {
        // Nothing to do here
    }

    override fun shutdown() {
        // Nothing to do here
    }

    override suspend fun onContextSet(
        oldContext: EvaluationContext?,
        newContext: EvaluationContext,
    ) {
        // Nothing to do here
    }

    override fun getBooleanEvaluation(
        key: String,
        defaultValue: Boolean,
        context: EvaluationContext?,
    ): ProviderEvaluation<Boolean> = evaluateEnvironmentVariable(key, String::toBoolean)

    override fun getDoubleEvaluation(
        key: String,
        defaultValue: Double,
        context: EvaluationContext?,
    ): ProviderEvaluation<Double> = evaluateEnvironmentVariable(key, String::toDouble)

    override fun getIntegerEvaluation(
        key: String,
        defaultValue: Int,
        context: EvaluationContext?,
    ): ProviderEvaluation<Int> = evaluateEnvironmentVariable(key, String::toInt)

    override fun getStringEvaluation(
        key: String,
        defaultValue: String,
        context: EvaluationContext?,
    ): ProviderEvaluation<String> = evaluateEnvironmentVariable(key, { it })

    override fun getObjectEvaluation(
        key: String,
        defaultValue: Value,
        context: EvaluationContext?,
    ): ProviderEvaluation<Value> = throw OpenFeatureError.GeneralError("EnvVarProvider supports only primitives")

    private fun <T> evaluateEnvironmentVariable(
        key: String,
        parse: (String) -> T,
    ): ProviderEvaluation<T> {
        val value: String =
            environmentGateway.getEnvironmentVariable(keyTransformer.transformKey(key))
                ?: throw OpenFeatureError.FlagNotFoundError(key)

        try {
            return ProviderEvaluation(
                value = parse(value),
                reason = Reason.STATIC.toString(),
            )
        } catch (e: Exception) {
            throw OpenFeatureError.ParseError(e.message ?: "Unknown parsing error")
        }
    }

    companion object {
        private val NAME: String = "Environment Variables Provider"
    }

    private object Metadata : ProviderMetadata {
        override val name: String
            get() = NAME
    }
}
