package dev.openfeature.kotlin.contrib.providers.launchdarkly

import dev.openfeature.kotlin.sdk.EvaluationMetadata
import dev.openfeature.kotlin.sdk.ProviderEvaluation
import dev.openfeature.kotlin.sdk.exceptions.ErrorCode

internal enum class LdErrorKind {
    CLIENT_NOT_READY,
    FLAG_NOT_FOUND,
    MALFORMED_FLAG,
    USER_NOT_SPECIFIED,
    WRONG_TYPE,
    EXCEPTION,
}

internal fun LdErrorKind.toErrorCode(): ErrorCode =
    when (this) {
        LdErrorKind.CLIENT_NOT_READY -> ErrorCode.PROVIDER_NOT_READY
        LdErrorKind.FLAG_NOT_FOUND -> ErrorCode.FLAG_NOT_FOUND
        LdErrorKind.MALFORMED_FLAG -> ErrorCode.PARSE_ERROR
        LdErrorKind.USER_NOT_SPECIFIED -> ErrorCode.TARGETING_KEY_MISSING
        LdErrorKind.WRONG_TYPE -> ErrorCode.TYPE_MISMATCH
        LdErrorKind.EXCEPTION -> ErrorCode.GENERAL
    }

internal data class LdEvaluationDetail<out T>(
    val value: T,
    val variationIndex: Int?,
    val reasonKind: String?,
    val errorKind: LdErrorKind?,
    val exceptionMessage: String?,
)

internal fun <T> ldClientNotReadyEvaluationDetail(defaultValue: T): LdEvaluationDetail<T> =
    LdEvaluationDetail(
        value = defaultValue,
        variationIndex = null,
        reasonKind = null,
        errorKind = LdErrorKind.CLIENT_NOT_READY,
        exceptionMessage = null,
    )

internal fun <T> LdEvaluationDetail<T>.toProviderEvaluation(): ProviderEvaluation<T> =
    ProviderEvaluation(
        value = value,
        variant = variationIndex?.toString(),
        reason = reasonKind,
        errorCode = errorKind?.toErrorCode(),
        errorMessage = exceptionMessage,
        metadata = EvaluationMetadata.EMPTY,
    )
