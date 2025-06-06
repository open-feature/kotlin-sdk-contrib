package org.gofeatureflag.openfeature.ofrep

import FlagDto
import dev.openfeature.sdk.EvaluationContext
import dev.openfeature.sdk.FeatureProvider
import dev.openfeature.sdk.Hook
import dev.openfeature.sdk.ImmutableContext
import dev.openfeature.sdk.ProviderEvaluation
import dev.openfeature.sdk.ProviderMetadata
import dev.openfeature.sdk.Value
import dev.openfeature.sdk.events.OpenFeatureProviderEvents
import dev.openfeature.sdk.exceptions.ErrorCode
import dev.openfeature.sdk.exceptions.OpenFeatureError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.gofeatureflag.openfeature.ofrep.bean.OfrepOptions
import org.gofeatureflag.openfeature.ofrep.bean.OfrepProviderMetadata
import org.gofeatureflag.openfeature.ofrep.controller.OfrepApi
import org.gofeatureflag.openfeature.ofrep.enum.BulkEvaluationStatus
import org.gofeatureflag.openfeature.ofrep.error.OfrepError
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Timer
import java.util.TimerTask
import kotlin.reflect.KClass

class OfrepProvider(
    private val ofrepOptions: OfrepOptions,
) : FeatureProvider {
    private val ofrepApi = OfrepApi(ofrepOptions)
    override val hooks: List<Hook<*>>
        get() = listOf()

    override val metadata: ProviderMetadata
        get() = OfrepProviderMetadata()

    private var evaluationContext: EvaluationContext? = null
    private var inMemoryCache: Map<String, FlagDto> = emptyMap()
    private var retryAfter: Date? = null
    private var pollingTimer: Timer? = null

    private val statusFlow = MutableSharedFlow<OpenFeatureProviderEvents>(replay = 1)

    override fun observe(): Flow<OpenFeatureProviderEvents> = statusFlow

    override suspend fun initialize(initialContext: EvaluationContext?) {
        this.evaluationContext = initialContext
        try {
            val bulkEvaluationStatus = evaluateFlags(initialContext ?: ImmutableContext())
            if (bulkEvaluationStatus == BulkEvaluationStatus.RATE_LIMITED) {
                statusFlow.emit(
                    OpenFeatureProviderEvents.ProviderError(
                        OpenFeatureError.GeneralError("Rate limited"),
                    ),
                )
            } else {
                statusFlow.emit(OpenFeatureProviderEvents.ProviderReady)
            }
        } catch (e: OpenFeatureError) {
            statusFlow.emit(OpenFeatureProviderEvents.ProviderError(e))
        } catch (e: Exception) {
            statusFlow.emit(OpenFeatureProviderEvents.ProviderError(OpenFeatureError.GeneralError(e.message ?: "Unknown error")))
        }
        this.startPolling(this.ofrepOptions.pollingIntervalInMillis)
    }

    /**
     * Start polling for flag updates
     */
    private fun startPolling(pollingIntervalInMillis: Long) {
        val task: TimerTask =
            object : TimerTask() {
                override fun run() {
                    runBlocking {
                        try {
                            val resp =
                                this@OfrepProvider.evaluateFlags(this@OfrepProvider.evaluationContext!!)

                            when (resp) {
                                BulkEvaluationStatus.RATE_LIMITED, BulkEvaluationStatus.SUCCESS_NO_CHANGE -> {
                                    // Nothing to do !
                                    //
                                    // if rate limited: the provider should already be in stale status and
                                    //    we don't need to emit an event or call again the API
                                    //
                                    // if no change: the provider should already be in ready status and
                                    //    we don't need to emit an event if nothing has changed
                                }

                                BulkEvaluationStatus.SUCCESS_UPDATED -> {
                                    // TODO: we should migrate to configuration change event when it's available
                                    // in the kotlin SDK
                                    statusFlow.emit(OpenFeatureProviderEvents.ProviderReady)
                                }
                            }
                        } catch (e: OfrepError.ApiTooManyRequestsError) {
                            // in that case the provider is just stale because we were not able to
                            statusFlow.emit(OpenFeatureProviderEvents.ProviderStale)
                        } catch (e: Throwable) {
                            statusFlow.emit(OpenFeatureProviderEvents.ProviderError(OpenFeatureError.GeneralError(e.message ?: "")))
                        }
                    }
                }
            }
        val timer = Timer()
        timer.schedule(task, pollingIntervalInMillis, pollingIntervalInMillis)
        this.pollingTimer = timer
    }

    override fun getBooleanEvaluation(
        key: String,
        defaultValue: Boolean,
        context: EvaluationContext?,
    ): ProviderEvaluation<Boolean> = genericEvaluation<Boolean>(key, Boolean::class)

    override fun getDoubleEvaluation(
        key: String,
        defaultValue: Double,
        context: EvaluationContext?,
    ): ProviderEvaluation<Double> = genericEvaluation<Double>(key, Double::class)

    override fun getIntegerEvaluation(
        key: String,
        defaultValue: Int,
        context: EvaluationContext?,
    ): ProviderEvaluation<Int> = genericEvaluation<Int>(key, Int::class)

    override fun getObjectEvaluation(
        key: String,
        defaultValue: Value,
        context: EvaluationContext?,
    ): ProviderEvaluation<Value> = genericEvaluation<Value>(key, Object::class)

    override fun getStringEvaluation(
        key: String,
        defaultValue: String,
        context: EvaluationContext?,
    ): ProviderEvaluation<String> = genericEvaluation<String>(key, String::class)

    override suspend fun onContextSet(
        oldContext: EvaluationContext?,
        newContext: EvaluationContext,
    ) {
        this.statusFlow.emit(OpenFeatureProviderEvents.ProviderStale)
        this.evaluationContext = newContext

        try {
            val postBulkEvaluateFlags = evaluateFlags(newContext)
            // we don't emit event if the evaluation is rate limited because
            // the provider is still stale
            if (postBulkEvaluateFlags != BulkEvaluationStatus.RATE_LIMITED) {
                statusFlow.emit(OpenFeatureProviderEvents.ProviderReady)
            }
        } catch (e: Throwable) {
            statusFlow.emit(OpenFeatureProviderEvents.ProviderError(OpenFeatureError.GeneralError(e.message ?: "")))
        }
    }

    override fun shutdown() {
        this.pollingTimer?.cancel()
    }

    private fun <T : Any> genericEvaluation(
        key: String,
        expectedType: KClass<*>,
    ): ProviderEvaluation<T> {
        val flag = this.inMemoryCache[key] ?: throw OpenFeatureError.FlagNotFoundError(key)

        if (flag.isError()) {
            when (flag.errorCode) {
                ErrorCode.FLAG_NOT_FOUND -> throw OpenFeatureError.FlagNotFoundError(key)
                ErrorCode.INVALID_CONTEXT -> throw OpenFeatureError.InvalidContextError()
                ErrorCode.PARSE_ERROR -> throw OpenFeatureError.ParseError(
                    flag.errorDetails ?: "parse error",
                )

                ErrorCode.PROVIDER_NOT_READY -> throw OpenFeatureError.ProviderNotReadyError()
                ErrorCode.TARGETING_KEY_MISSING -> throw OpenFeatureError.TargetingKeyMissingError()
                else -> throw OpenFeatureError.GeneralError(flag.errorDetails ?: "general error")
            }
        }
        return flag.toProviderEvaluation(expectedType)
    }

    /**
     * Evaluate the flags for the given context.
     * It will store the flags in the in-memory cache, if any error occurs it will throw an exception.
     */
    private suspend fun evaluateFlags(context: EvaluationContext): BulkEvaluationStatus {
        if (this.retryAfter != null && this.retryAfter!! > Date()) {
            return BulkEvaluationStatus.RATE_LIMITED
        }

        try {
            val postBulkEvaluateFlags =
                this@OfrepProvider.ofrepApi.postBulkEvaluateFlags(context)
            val ofrepEvalResp = postBulkEvaluateFlags.apiResponse
            val httpResp = postBulkEvaluateFlags.httpResponse

            if (httpResp.code == 304) {
                return BulkEvaluationStatus.SUCCESS_NO_CHANGE
            }

            if (postBulkEvaluateFlags.isError()) {
                when (ofrepEvalResp?.errorCode) {
                    ErrorCode.PROVIDER_NOT_READY -> throw OpenFeatureError.ProviderNotReadyError()
                    ErrorCode.PARSE_ERROR -> throw OpenFeatureError.ParseError(
                        ofrepEvalResp.errorDetails ?: "",
                    )

                    ErrorCode.TARGETING_KEY_MISSING -> throw OpenFeatureError.TargetingKeyMissingError()
                    ErrorCode.INVALID_CONTEXT -> throw OpenFeatureError.InvalidContextError()
                    else -> throw OpenFeatureError.GeneralError(ofrepEvalResp?.errorDetails ?: "")
                }
            }
            val inMemoryCacheNew = ofrepEvalResp?.flags?.associateBy { it.key } ?: emptyMap()
            this.inMemoryCache = inMemoryCacheNew
            return BulkEvaluationStatus.SUCCESS_UPDATED
        } catch (e: OfrepError.ApiTooManyRequestsError) {
            this.retryAfter = calculateRetryDate(e.response?.headers?.get("Retry-After") ?: "")
            return BulkEvaluationStatus.RATE_LIMITED
        }
    }

    private fun calculateRetryDate(retryAfter: String): Date? {
        if (retryAfter.isEmpty()) {
            return null
        }

        val retryDate: Calendar = Calendar.getInstance()
        try {
            // If retryAfter is a number, it represents seconds to wait.
            val delayInSeconds = retryAfter.toInt()
            retryDate.add(Calendar.SECOND, delayInSeconds)
        } catch (e: NumberFormatException) {
            // If retryAfter is not a number, it's an HTTP-date.
            val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("GMT")
            retryDate.time = dateFormat.parse(retryAfter) ?: return null
        }
        return retryDate.time
    }
}
