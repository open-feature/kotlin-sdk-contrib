@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.openfeature.kotlin.contrib.providers.launchdarkly

import dev.openfeature.kotlin.sdk.EvaluationContext
import dev.openfeature.kotlin.sdk.Value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.AutoEnvAttributesDisabled
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.AutoEnvAttributesEnabled
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.ContextBuilderResult
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.LDClient
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.LDConfig
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.LDContext
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.LDContextBuilder
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.LDValue
import kotlin.coroutines.resume

internal class IosLaunchDarklyEngine(
    private val config: LaunchDarklyConfig,
    private val clientProvider: LdClientProvider,
) : LaunchDarklyEngine {
    constructor(config: LaunchDarklyConfig) : this(config, DefaultLdClientProvider())

    override suspend fun initialize(initialContext: EvaluationContext?) {
        // LDClient and LDConfig are main-thread oriented; LaunchDarkly iOS 11+ also invokes start completions on
        // the main queue (Swift 6–friendly), so this dispatcher stays correct.
        withContext(Dispatchers.Main) {
            val autoEnvAttributesMode = if (config.autoEnvAttributes) {
                AutoEnvAttributesEnabled
            } else {
                AutoEnvAttributesDisabled
            }
            val ldConfig = LDConfig(
                mobileKey = config.mobileKey,
                autoEnvAttributes = autoEnvAttributesMode
            )

            ldConfig.debugMode = config.debugLogging
            ldConfig.evaluationReasons = config.evaluationReasons
            ldConfig.sendEvents = config.sendEvents
            config.wrapperName?.let { wrapperNameToApply ->
                ldConfig.wrapperName = wrapperNameToApply
                ldConfig.wrapperVersion = config.wrapperVersion
            }
            val ldContext = initialContext.toLDContext()
            suspendCancellableCoroutine { continuation ->
                LDClient.startWithConfiguration(
                    configuration = ldConfig,
                    context = ldContext,
                    startWaitSeconds = config.initWaitSeconds.toDouble(),
                    completion = {
                        continuation.resume(Unit)
                    },
                )
            }
            (clientProvider as? DefaultLdClientProvider)?.client = LDClient.get()
        }
    }

    override fun shutdown() {
        val ldClient = clientProvider.getClient()
        ldClient?.close()
        (clientProvider as? DefaultLdClientProvider)?.client = null
    }

    override suspend fun onContextSet(
        oldContext: EvaluationContext?,
        newContext: EvaluationContext,
    ) {
        withContext(Dispatchers.Main) {
            val ldClient = clientProvider.getClient() ?: return@withContext
            val ldContext = newContext.toLDContext()
            suspendCancellableCoroutine { continuation ->
                ldClient.identifyWithContext(ldContext) {
                    continuation.resume(Unit)
                }
            }
        }
    }

    override fun getBooleanDetail(
        key: String,
        defaultValue: Boolean,
        context: EvaluationContext?,
    ): LdEvaluationDetail<Boolean> {
        val ldClient = clientProvider.getClient() ?: return ldClientNotReadyEvaluationDetail(defaultValue)
        val evaluationDetail = ldClient.boolVariationDetailForKey(key, defaultValue)
        return toLdEvaluationDetail(
            evaluationDetail.value,
            evaluationDetail.variationIndex,
            evaluationDetail.reason,
        )
    }

    override fun getStringDetail(
        key: String,
        defaultValue: String,
        context: EvaluationContext?,
    ): LdEvaluationDetail<String> {
        val ldClient = clientProvider.getClient() ?: return ldClientNotReadyEvaluationDetail(defaultValue)
        val evaluationDetail = ldClient.stringVariationDetailForKey(key, defaultValue)
        val resolvedString = evaluationDetail.value ?: defaultValue
        return toLdEvaluationDetail(
            resolvedString,
            evaluationDetail.variationIndex,
            evaluationDetail.reason,
        )
    }

    override fun getIntegerDetail(
        key: String,
        defaultValue: Int,
        context: EvaluationContext?,
    ): LdEvaluationDetail<Int> {
        val ldClient = clientProvider.getClient() ?: return ldClientNotReadyEvaluationDetail(defaultValue)
        val evaluationDetail =
            ldClient.integerVariationDetailForKey(key, defaultValue.toLong())
        return toLdEvaluationDetail(
            evaluationDetail.value.toInt(),
            evaluationDetail.variationIndex,
            evaluationDetail.reason,
        )
    }

    override fun getDoubleDetail(
        key: String,
        defaultValue: Double,
        context: EvaluationContext?,
    ): LdEvaluationDetail<Double> {
        val ldClient = clientProvider.getClient() ?: return ldClientNotReadyEvaluationDetail(defaultValue)
        val evaluationDetail = ldClient.doubleVariationDetailForKey(key, defaultValue)
        return toLdEvaluationDetail(
            evaluationDetail.value,
            evaluationDetail.variationIndex,
            evaluationDetail.reason,
        )
    }

    override fun getObjectDetail(
        key: String,
        defaultValue: Value,
        context: EvaluationContext?,
    ): LdEvaluationDetail<Value> {
        val ldClient = clientProvider.getClient() ?: return ldClientNotReadyEvaluationDetail(defaultValue)
        val ldDefault = defaultValue.toLDValue()
        val jsonEvaluationDetail = ldClient.jsonVariationDetailForKey(key, ldDefault)
        val objectValue = jsonEvaluationDetail.value.toValue()
        return toLdEvaluationDetail(
            objectValue,
            jsonEvaluationDetail.variationIndex,
            jsonEvaluationDetail.reason,
        )
    }

    private fun <T> toLdEvaluationDetail(
        value: T,
        variationIndex: Long,
        reason: Map<out Any?, *>?,
    ): LdEvaluationDetail<T> =
        LdEvaluationDetail(
            value = value,
            variationIndex = variationIndex.toInt().takeIf { it >= 0 },
            reasonKind = reason.reasonKindString(),
            errorKind = reason.errorKindEnum(),
            exceptionMessage = null,
        )

    private fun Map<out Any?, *>?.reasonKindString(): String? {
        if (this == null) return null
        val kindEntry = this["kind"] ?: return null
        val kindLdValue = kindEntry as? LDValue ?: return null
        return kindLdValue.stringValue()
    }

    private fun Map<out Any?, *>?.errorKindEnum(): LdErrorKind? {
        if (this == null) return null
        val errorKindEntry = this["errorKind"] ?: return null
        val errorKindLdValue = errorKindEntry as? LDValue ?: return null
        val errorKindString = errorKindLdValue.stringValue()
        return when (errorKindString) {
            "CLIENT_NOT_READY" -> LdErrorKind.CLIENT_NOT_READY
            "FLAG_NOT_FOUND" -> LdErrorKind.FLAG_NOT_FOUND
            "MALFORMED_FLAG" -> LdErrorKind.MALFORMED_FLAG
            "USER_NOT_SPECIFIED" -> LdErrorKind.USER_NOT_SPECIFIED
            "WRONG_TYPE" -> LdErrorKind.WRONG_TYPE
            "EXCEPTION" -> LdErrorKind.EXCEPTION
            else -> null
        }
    }
}

private fun EvaluationContext?.toLDContext(): LDContext {
    if (this == null) {
        val contextBuilder = LDContextBuilder(key = DEFAULT_TARGETING_KEY)
        contextBuilder.anonymousWithAnonymous(true)
        return contextBuilder.build().unwrapContext()
    }
    val contextBuilder = LDContextBuilder(key = targetingKeyOrAnonymous)
    contextBuilder.anonymousWithAnonymous(targetingKeyOrAnonymous == DEFAULT_TARGETING_KEY)
    for ((attributeName, attributeValue) in asMap()) {
        contextBuilder.trySetValueWithName(attributeName, attributeValue.toLDValue())
    }
    return contextBuilder.build().unwrapContext()
}

private fun ContextBuilderResult.unwrapContext(): LDContext {
    val builtContext = success
    if (builtContext != null) return builtContext
    val failureDescription = failure?.localizedDescription ?: "unknown"
    error("LaunchDarkly LDContext build failed: $failureDescription")
}
