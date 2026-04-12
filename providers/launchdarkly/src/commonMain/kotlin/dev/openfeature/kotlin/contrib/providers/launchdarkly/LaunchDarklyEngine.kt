package dev.openfeature.kotlin.contrib.providers.launchdarkly

import dev.openfeature.kotlin.sdk.EvaluationContext
import dev.openfeature.kotlin.sdk.Value

internal interface LaunchDarklyEngine {
    suspend fun initialize(initialContext: EvaluationContext?)

    fun shutdown()

    suspend fun onContextSet(
        oldContext: EvaluationContext?,
        newContext: EvaluationContext,
    )

    fun getBooleanDetail(
        key: String,
        defaultValue: Boolean,
        context: EvaluationContext?,
    ): LdEvaluationDetail<Boolean>

    fun getStringDetail(
        key: String,
        defaultValue: String,
        context: EvaluationContext?,
    ): LdEvaluationDetail<String>

    fun getIntegerDetail(
        key: String,
        defaultValue: Int,
        context: EvaluationContext?,
    ): LdEvaluationDetail<Int>

    fun getDoubleDetail(
        key: String,
        defaultValue: Double,
        context: EvaluationContext?,
    ): LdEvaluationDetail<Double>

    fun getObjectDetail(
        key: String,
        defaultValue: Value,
        context: EvaluationContext?,
    ): LdEvaluationDetail<Value>
}
