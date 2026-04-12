package dev.openfeature.kotlin.contrib.providers.launchdarkly

import android.app.Application
import com.launchdarkly.logging.LDLogLevel
import com.launchdarkly.sdk.EvaluationDetail
import com.launchdarkly.sdk.EvaluationReason
import com.launchdarkly.sdk.LDContext
import com.launchdarkly.sdk.android.Components
import com.launchdarkly.sdk.android.LDClient
import com.launchdarkly.sdk.android.LDConfig
import dev.openfeature.kotlin.sdk.EvaluationContext
import dev.openfeature.kotlin.sdk.Value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class AndroidLaunchDarklyEngine(
    private val application: Application,
    private val config: LaunchDarklyConfig,
) : LaunchDarklyEngine {
    private lateinit var client: LDClient

    override suspend fun initialize(initialContext: EvaluationContext?) {
        withContext(Dispatchers.IO) {
            val auto = if (config.autoEnvAttributes) {
                LDConfig.Builder.AutoEnvAttributes.Enabled
            } else {
                LDConfig.Builder.AutoEnvAttributes.Disabled
            }
            val ldConfig = LDConfig.Builder(auto)
                .mobileKey(config.mobileKey)
                .events(
                    if (config.sendEvents) {
                        Components.sendEvents()
                    } else {
                        Components.noEvents()
                    },
                )
                .let { builder ->
                    val name = config.wrapperName
                    if (name != null) {
                        builder.http(
                            Components.httpConfiguration().wrapper(
                                name,
                                config.wrapperVersion.orEmpty(),
                            ),
                        )
                    } else {
                        builder
                    }
                }
                .logLevel(if (config.debugLogging) LDLogLevel.DEBUG else LDLogLevel.NONE)
                .evaluationReasons(config.evaluationReasons)
                .build()
            val ldContext = initialContext.toLDContext()
            client = LDClient.init(
                application,
                ldConfig,
                ldContext,
                config.initWaitSeconds,
            )
        }
    }

    override fun shutdown() {
        if (::client.isInitialized) {
            runCatching { client.close() }
        }
    }

    override suspend fun onContextSet(
        oldContext: EvaluationContext?,
        newContext: EvaluationContext,
    ) {
        withContext(Dispatchers.IO) {
            val ldContext = newContext.toLDContext()
            client.identify(ldContext).get(config.contextUpdateTimeoutMs, TimeUnit.MILLISECONDS)
        }
    }

    override fun getBooleanDetail(
        key: String,
        defaultValue: Boolean,
        context: EvaluationContext?,
    ): LdEvaluationDetail<Boolean> = detail(client.boolVariationDetail(key, defaultValue))

    override fun getStringDetail(
        key: String,
        defaultValue: String,
        context: EvaluationContext?,
    ): LdEvaluationDetail<String> = detail(client.stringVariationDetail(key, defaultValue))

    override fun getIntegerDetail(
        key: String,
        defaultValue: Int,
        context: EvaluationContext?,
    ): LdEvaluationDetail<Int> = detail(client.intVariationDetail(key, defaultValue))

    override fun getDoubleDetail(
        key: String,
        defaultValue: Double,
        context: EvaluationContext?,
    ): LdEvaluationDetail<Double> = detail(client.doubleVariationDetail(key, defaultValue))

    override fun getObjectDetail(
        key: String,
        defaultValue: Value,
        context: EvaluationContext?,
    ): LdEvaluationDetail<Value> {
        val ldDefault = defaultValue.toLDValue()
        val evaluationDetail = client.jsonValueVariationDetail(key, ldDefault)
        val value = evaluationDetail.value?.toValue() ?: Value.Null
        return LdEvaluationDetail(
            value = value,
            variationIndex = evaluationDetail.variationIndex,
            reasonKind = evaluationDetail.reason.kind?.name,
            errorKind = evaluationDetail.errorKind(),
            exceptionMessage = evaluationDetail.reason.exception?.message,
        )
    }

    private fun <T> detail(detail: EvaluationDetail<T>): LdEvaluationDetail<T> =
        LdEvaluationDetail(
            value = detail.value,
            variationIndex = detail.variationIndex,
            reasonKind = detail.reason.kind?.name,
            errorKind = detail.errorKind(),
            exceptionMessage = detail.reason.exception?.message,
        )

    private fun <T> EvaluationDetail<T>.errorKind(): LdErrorKind? = when (reason.errorKind) {
        null -> null
        EvaluationReason.ErrorKind.CLIENT_NOT_READY -> LdErrorKind.CLIENT_NOT_READY
        EvaluationReason.ErrorKind.FLAG_NOT_FOUND -> LdErrorKind.FLAG_NOT_FOUND
        EvaluationReason.ErrorKind.MALFORMED_FLAG -> LdErrorKind.MALFORMED_FLAG
        EvaluationReason.ErrorKind.USER_NOT_SPECIFIED -> LdErrorKind.USER_NOT_SPECIFIED
        EvaluationReason.ErrorKind.WRONG_TYPE -> LdErrorKind.WRONG_TYPE
        EvaluationReason.ErrorKind.EXCEPTION -> LdErrorKind.EXCEPTION
    }

    private fun EvaluationContext?.toLDContext(): LDContext {
        if (this == null) {
            return LDContext.builder(DEFAULT_TARGETING_KEY).anonymous(true).build()
        }
        val builder = LDContext.builder(targetingKeyOrAnonymous)
        builder.anonymous(targetingKeyOrAnonymous == DEFAULT_TARGETING_KEY)
        for ((attrName, attrValue) in asMap()) {
            builder.trySet(attrName, attrValue.toLDValue())
        }
        return builder.build()
    }
}
