package dev.openfeature.kotlin.contrib.providers.envvar

import dev.openfeature.kotlin.sdk.EvaluationContext
import dev.openfeature.kotlin.sdk.Hook
import dev.openfeature.kotlin.sdk.OpenFeatureStatus
import dev.openfeature.kotlin.sdk.ProviderEvaluation
import dev.openfeature.kotlin.sdk.ProviderMetadata
import dev.openfeature.kotlin.sdk.ProviderStatusTracker
import dev.openfeature.kotlin.sdk.Reason
import dev.openfeature.kotlin.sdk.StateManagingProvider
import dev.openfeature.kotlin.sdk.Value
import dev.openfeature.kotlin.sdk.events.OpenFeatureProviderEvents
import dev.openfeature.kotlin.sdk.exceptions.ErrorCode
import dev.openfeature.kotlin.sdk.exceptions.OpenFeatureError
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.yield

/** EnvVarProvider is the Kotlin provider implementation for the environment variables.  */
class EnvVarProvider(
    private val environmentGateway: EnvironmentGateway = platformSpecificEnvironmentGateway(),
    private val keyTransformer: EnvironmentKeyTransformer = EnvironmentKeyTransformer.doNothing(),
) : StateManagingProvider {
    private val statusTracker = ProviderStatusTracker()

    override val hooks: List<Hook<*>> = emptyList()
    override val metadata: ProviderMetadata
        get() = Metadata

    override val status: StateFlow<OpenFeatureStatus> = statusTracker.status

    override suspend fun initialize(initialContext: EvaluationContext?) {
        statusTracker.send(OpenFeatureProviderEvents.ProviderReady())
    }

    override fun shutdown() {
        statusTracker.send(
            OpenFeatureProviderEvents.ProviderError(
                OpenFeatureProviderEvents.EventDetails(
                    message = "Environment Variables provider shut down; not ready for evaluation",
                    errorCode = ErrorCode.PROVIDER_NOT_READY,
                ),
            ),
        )
    }

    override suspend fun onContextSet(
        oldContext: EvaluationContext?,
        newContext: EvaluationContext,
    ) {
        statusTracker.send(OpenFeatureProviderEvents.ProviderReconciling())
        yield()
        statusTracker.send(OpenFeatureProviderEvents.ProviderReady())
    }

    override fun observe() = statusTracker.observe()

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

    override fun getLongEvaluation(
        key: String,
        defaultValue: Long,
        context: EvaluationContext?,
    ): ProviderEvaluation<Long> = evaluateEnvironmentVariable(key, String::toLong)

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
