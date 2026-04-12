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
) : LaunchDarklyEngine {
    private var client: LDClient? = null

    override suspend fun initialize(initialContext: EvaluationContext?) {
        // LDClient and LDConfig are main-thread oriented; LaunchDarkly iOS 11+ also invokes start completions on
        // the main queue (Swift 6–friendly), so this dispatcher stays correct.
        withContext(Dispatchers.Main) {
            val auto =
                if (config.autoEnvAttributes) {
                    AutoEnvAttributesEnabled
                } else {
                    AutoEnvAttributesDisabled
                }
            val ldConfig = LDConfig(mobileKey = config.mobileKey, autoEnvAttributes = auto)
            ldConfig.debugMode = config.debugLogging
            ldConfig.evaluationReasons = config.evaluationReasons
            ldConfig.sendEvents = config.sendEvents
            config.wrapperName?.let { name ->
                ldConfig.wrapperName = name
                ldConfig.wrapperVersion = config.wrapperVersion
            }
            val ldContext = initialContext.toLDContext()
            suspendCancellableCoroutine { cont ->
                LDClient.startWithConfiguration(
                    configuration = ldConfig,
                    context = ldContext,
                    startWaitSeconds = config.initWaitSeconds.toDouble(),
                    completion = { _ ->
                        cont.resume(Unit)
                    },
                )
            }
            client = LDClient.get()
        }
    }

    override fun shutdown() {
        client?.close()
        client = null
    }

    override suspend fun onContextSet(
        oldContext: EvaluationContext?,
        newContext: EvaluationContext,
    ) {
        withContext(Dispatchers.Main) {
            val c = client ?: return@withContext
            val ldContext = newContext.toLDContext()
            suspendCancellableCoroutine { cont ->
                c.identifyWithContext(ldContext) {
                    cont.resume(Unit)
                }
            }
        }
    }

    private fun requireClient(): LDClient =
        checkNotNull(client) { "LaunchDarkly LDClient not started" }

    override fun getBooleanDetail(
        key: String,
        defaultValue: Boolean,
        context: EvaluationContext?,
    ): LdEvaluationDetail<Boolean> {
        val d = requireClient().boolVariationDetailForKey(key, defaultValue)
        return toLdEvaluationDetail(d.value, d.variationIndex, d.reason)
    }

    override fun getStringDetail(
        key: String,
        defaultValue: String,
        context: EvaluationContext?,
    ): LdEvaluationDetail<String> {
        val d = requireClient().stringVariationDetailForKey(key, defaultValue)
        val v = d.value ?: defaultValue
        return toLdEvaluationDetail(v, d.variationIndex, d.reason)
    }

    override fun getIntegerDetail(
        key: String,
        defaultValue: Int,
        context: EvaluationContext?,
    ): LdEvaluationDetail<Int> {
        val d = requireClient().integerVariationDetailForKey(key, defaultValue.toLong())
        return toLdEvaluationDetail(d.value.toInt(), d.variationIndex, d.reason)
    }

    override fun getDoubleDetail(
        key: String,
        defaultValue: Double,
        context: EvaluationContext?,
    ): LdEvaluationDetail<Double> {
        val d = requireClient().doubleVariationDetailForKey(key, defaultValue)
        return toLdEvaluationDetail(d.value, d.variationIndex, d.reason)
    }

    override fun getObjectDetail(
        key: String,
        defaultValue: Value,
        context: EvaluationContext?,
    ): LdEvaluationDetail<Value> {
        val ldDefault = defaultValue.toLDValue()
        val jsonDetail = requireClient().jsonVariationDetailForKey(key, ldDefault)
        val value = jsonDetail.value.toValue()
        return toLdEvaluationDetail(value, jsonDetail.variationIndex, jsonDetail.reason)
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
        val raw = this["kind"] ?: return null
        val v = raw as? LDValue ?: return null
        return v.stringValue()
    }

    private fun Map<out Any?, *>?.errorKindEnum(): LdErrorKind? {
        if (this == null) return null
        val raw = this["errorKind"] ?: return null
        val v = raw as? LDValue ?: return null
        val s = v.stringValue()
        return when (s) {
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
        val b = LDContextBuilder(key = DEFAULT_TARGETING_KEY)
        b.anonymousWithAnonymous(true)
        return b.build().unwrapContext()
    }
    val b = LDContextBuilder(key = targetingKeyOrAnonymous)
    b.anonymousWithAnonymous(targetingKeyOrAnonymous == DEFAULT_TARGETING_KEY)
    for ((k, v) in asMap()) {
        b.trySetValueWithName(k, v.toLDValue())
    }
    return b.build().unwrapContext()
}

private fun ContextBuilderResult.unwrapContext(): LDContext {
    val s = success
    if (s != null) return s
    val err = failure?.localizedDescription ?: "unknown"
    error("LaunchDarkly LDContext build failed: $err")
}
