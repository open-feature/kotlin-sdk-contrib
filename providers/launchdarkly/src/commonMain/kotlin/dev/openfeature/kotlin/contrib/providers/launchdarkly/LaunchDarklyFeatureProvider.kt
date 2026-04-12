package dev.openfeature.kotlin.contrib.providers.launchdarkly

import dev.openfeature.kotlin.sdk.EvaluationContext
import dev.openfeature.kotlin.sdk.FeatureProvider
import dev.openfeature.kotlin.sdk.Hook
import dev.openfeature.kotlin.sdk.ProviderEvaluation
import dev.openfeature.kotlin.sdk.ProviderMetadata
import dev.openfeature.kotlin.sdk.Value

/**
 * OpenFeature [FeatureProvider] that delegates evaluation to a [LaunchDarklyEngine].
 */
class LaunchDarklyFeatureProvider internal constructor(
    private val engine: LaunchDarklyEngine,
) : FeatureProvider {
    override val hooks: List<Hook<*>> = emptyList()

    override val metadata: ProviderMetadata = Metadata

    override suspend fun initialize(initialContext: EvaluationContext?) {
        engine.initialize(initialContext)
    }

    override fun shutdown() {
        engine.shutdown()
    }

    override suspend fun onContextSet(
        oldContext: EvaluationContext?,
        newContext: EvaluationContext,
    ) {
        engine.onContextSet(oldContext, newContext)
    }

    override fun getBooleanEvaluation(
        key: String,
        defaultValue: Boolean,
        context: EvaluationContext?,
    ): ProviderEvaluation<Boolean> = engine.getBooleanDetail(key, defaultValue, context).toProviderEvaluation()

    override fun getStringEvaluation(
        key: String,
        defaultValue: String,
        context: EvaluationContext?,
    ): ProviderEvaluation<String> = engine.getStringDetail(key, defaultValue, context).toProviderEvaluation()

    override fun getIntegerEvaluation(
        key: String,
        defaultValue: Int,
        context: EvaluationContext?,
    ): ProviderEvaluation<Int> = engine.getIntegerDetail(key, defaultValue, context).toProviderEvaluation()

    override fun getDoubleEvaluation(
        key: String,
        defaultValue: Double,
        context: EvaluationContext?,
    ): ProviderEvaluation<Double> = engine.getDoubleDetail(key, defaultValue, context).toProviderEvaluation()

    override fun getObjectEvaluation(
        key: String,
        defaultValue: Value,
        context: EvaluationContext?,
    ): ProviderEvaluation<Value> = engine.getObjectDetail(key, defaultValue, context).toProviderEvaluation()

    private object Metadata : ProviderMetadata {
        override val name: String = "LaunchDarkly"
    }
}
