package dev.openfeature.kotlin.contrib.providers.ofrep.bean

import OfrepApiResponse

data class PostBulkEvaluationResult(
    val apiResponse: OfrepApiResponse?,
    val httpResponse: okhttp3.Response,
) {
    fun isError(): Boolean = apiResponse?.errorCode != null
}
