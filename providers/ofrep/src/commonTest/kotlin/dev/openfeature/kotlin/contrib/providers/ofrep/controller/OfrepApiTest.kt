package dev.openfeature.kotlin.contrib.providers.ofrep.controller

import dev.openfeature.kotlin.contrib.providers.ofrep.FAKE_ENDPOINT
import dev.openfeature.kotlin.contrib.providers.ofrep.bean.FlagDto
import dev.openfeature.kotlin.contrib.providers.ofrep.bean.OfrepApiResponse
import dev.openfeature.kotlin.contrib.providers.ofrep.bean.OfrepOptions
import dev.openfeature.kotlin.contrib.providers.ofrep.error.OfrepError
import dev.openfeature.kotlin.contrib.providers.ofrep.getResourceAsString
import dev.openfeature.kotlin.contrib.providers.ofrep.mockEngineWithOneResponse
import dev.openfeature.kotlin.contrib.providers.ofrep.mockEngineWithTwoResponses
import dev.openfeature.kotlin.sdk.EvaluationMetadata
import dev.openfeature.kotlin.sdk.ImmutableContext
import dev.openfeature.kotlin.sdk.Value
import dev.openfeature.kotlin.sdk.exceptions.ErrorCode
import dev.openfeature.kotlin.sdk.exceptions.OpenFeatureError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private fun createOfrepApi(mockEngine: MockEngine) =
    OfrepApi(
        OfrepOptions(endpoint = FAKE_ENDPOINT, httpClientEngine = mockEngine),
    )

class OfrepApiTest {
    @Test
    fun shouldReturnAValidEvaluationResponse() =
        runBlocking {
            val jsonString = getResourceAsString("ofrep/valid_api_short_response.json")
            val mockEngine = mockEngineWithOneResponse(content = jsonString)

            val ofrepApi = createOfrepApi(mockEngine)

            val ctx =
                ImmutableContext(
                    targetingKey = "68cf565d-15cd-4e8b-95a6-9399987164cd",
                    attributes =
                        mutableMapOf(
                            "email" to Value.String("batman@gofeatureflag.org"),
                        ),
                )
            val res = ofrepApi.postBulkEvaluateFlags(ctx)
            assertEquals(200, res.httpResponse.status.value)

            val expected =
                OfrepApiResponse(
                    flags =
                        listOf(
                            FlagDto(
                                key = "badge-class2",
                                value = Value.String("green"),
                                reason = "DEFAULT",
                                variant = "nocolor",
                                errorCode = null,
                                errorDetails = null,
                                metadata = EvaluationMetadata.EMPTY,
                            ),
                            FlagDto(
                                key = "hide-logo",
                                value = Value.Boolean(false),
                                reason = "STATIC",
                                variant = "var_false",
                                errorCode = null,
                                errorDetails = null,
                                metadata = EvaluationMetadata.EMPTY,
                            ),
                            FlagDto(
                                key = "title-flag",
                                value = Value.String("GO Feature Flag"),
                                reason = "DEFAULT",
                                variant = "default_title",
                                errorCode = null,
                                errorDetails = null,
                                metadata =
                                    EvaluationMetadata
                                        .builder()
                                        .putString(
                                            "description",
                                            "This flag controls the title of the feature flag",
                                        ).putString("title", "Feature Flag Title")
                                        .build(),
                            ),
                        ),
                    null,
                    null,
                )
            assertEquals(expected, res.apiResponse)
        }

    @Test
    fun shouldThrowAnUnauthorizedError(): Unit =
        runBlocking {
            val mockEngine =
                mockEngineWithOneResponse(
                    content = "{}",
                    status = HttpStatusCode.fromValue(401),
                )
            val ofrepApi = createOfrepApi(mockEngine)
            val ctx = ImmutableContext(targetingKey = "68cf565d-15cd-4e8b-95a6-9399987164cd")
            assertThrows(OfrepError.ApiUnauthorizedError::class.java) {
                runBlocking {
                    ofrepApi.postBulkEvaluateFlags(ctx)
                }
            }
        }

    @Test
    fun shouldThrowAForbiddenError(): Unit =
        runBlocking {
            val mockEngine =
                mockEngineWithOneResponse(
                    content = "{}",
                    status = HttpStatusCode.fromValue(403),
                )
            val ofrepApi = createOfrepApi(mockEngine)
            val ctx = ImmutableContext(targetingKey = "68cf565d-15cd-4e8b-95a6-9399987164cd")
            assertThrows(OfrepError.ForbiddenError::class.java) {
                runBlocking {
                    ofrepApi.postBulkEvaluateFlags(ctx)
                }
            }
        }

    @Test
    fun shouldThrowTooManyRequest(): Unit =
        runBlocking {
            val mockEngine =
                mockEngineWithOneResponse(
                    content = "{}",
                    status = HttpStatusCode.fromValue(429),
                    additionalHeaders = headersOf(HttpHeaders.RetryAfter, "120"),
                )
            val ofrepApi = createOfrepApi(mockEngine)
            val ctx = ImmutableContext(targetingKey = "68cf565d-15cd-4e8b-95a6-9399987164cd")
            try {
                ofrepApi.postBulkEvaluateFlags(ctx)
                assertTrue("we exited the try block without throwing an exception", false)
            } catch (e: OfrepError.ApiTooManyRequestsError) {
                assertEquals(429, e.response?.status?.value)
                assertEquals(e.response?.headers?.get("Retry-After"), "120")
            }
        }

    @Test
    fun shouldThrowUnexpectedError(): Unit =
        runBlocking {
            val mockEngine =
                mockEngineWithOneResponse(
                    content = "{}",
                    status = HttpStatusCode.fromValue(500),
                )
            val ofrepApi = createOfrepApi(mockEngine)

            val ctx = ImmutableContext(targetingKey = "68cf565d-15cd-4e8b-95a6-9399987164cd")
            assertThrows(OfrepError.UnexpectedResponseError::class.java) {
                runBlocking {
                    ofrepApi.postBulkEvaluateFlags(ctx)
                }
            }
        }

    @Test
    fun shouldReturnAnEvaluationResponseInError(): Unit =
        runBlocking {
            val mockEngine =
                mockEngineWithOneResponse(
                    content =
                        """
                        {"errorCode": "INVALID_CONTEXT", "errorDetails":"explanation of the error"}
                        """.trimIndent(),
                    status = HttpStatusCode.fromValue(400),
                )

            val ofrepApi = createOfrepApi(mockEngine)

            val ctx = ImmutableContext(targetingKey = "68cf565d-15cd-4e8b-95a6-9399987164cd")
            val resp = ofrepApi.postBulkEvaluateFlags(ctx)
            assertTrue(resp.isError())
            assertEquals(ErrorCode.INVALID_CONTEXT, resp.apiResponse?.errorCode)
            assertEquals("explanation of the error", resp.apiResponse?.errorDetails)
            assertEquals(400, resp.httpResponse.status.value)
        }

    @Test
    fun shouldReturnaEvaluationResponseIfWeReceiveA304(): Unit =
        runBlocking {
            val mockEngine =
                mockEngineWithOneResponse(
                    content = "{}",
                    status = HttpStatusCode.fromValue(304),
                )

            val ofrepApi = createOfrepApi(mockEngine)

            val ctx = ImmutableContext(targetingKey = "68cf565d-15cd-4e8b-95a6-9399987164cd")
            val resp = ofrepApi.postBulkEvaluateFlags(ctx)
            assertFalse(resp.isError())
            assertEquals(304, resp.httpResponse.status.value)
        }

    @Test
    fun shouldThrowTargetingKeyMissingErrorWithNoTargetingKey(): Unit =
        runBlocking {
            val mockEngine =
                mockEngineWithOneResponse(
                    content = "{}",
                    status = HttpStatusCode.fromValue(304),
                )
            val ofrepApi = createOfrepApi(mockEngine)

            val ctx = ImmutableContext(targetingKey = "")
            assertThrows(OpenFeatureError.TargetingKeyMissingError::class.java) {
                runBlocking {
                    ofrepApi.postBulkEvaluateFlags(ctx)
                }
            }
        }

    @Test
    fun shouldThrowUnmarshallErrorWithInvalidJson(): Unit =
        runBlocking {
            val jsonString = getResourceAsString("ofrep/invalid_api_response.json")

            val mockEngine =
                mockEngineWithOneResponse(
                    content = jsonString,
                    status = HttpStatusCode.fromValue(400),
                )
            val ofrepApi = createOfrepApi(mockEngine)

            val ctx = ImmutableContext(targetingKey = "68cf565d-15cd-4e8b-95a6-9399987164cd")
            assertThrows(OfrepError.UnmarshallError::class.java) {
                runBlocking {
                    ofrepApi.postBulkEvaluateFlags(ctx)
                }
            }
        }

    @Test
    fun shouldThrowWithInvalidOptions(): Unit =
        runBlocking {
            assertThrows(OfrepError.InvalidOptionsError::class.java) {
                runBlocking {
                    OfrepApi(OfrepOptions(endpoint = "invalid_url"))
                }
            }
        }

    @Test
    fun shouldETagShouldNotMatch(): Unit =
        runBlocking {
            val jsonString = getResourceAsString("ofrep/valid_api_response.json")
            val mockEngine =
                mockEngineWithTwoResponses(
                    firstContent = jsonString,
                    firstStatus = HttpStatusCode.fromValue(200),
                    firstAdditionalHeaders = headersOf(HttpHeaders.ETag, "123"),
                    secondContent = "",
                    secondStatus = HttpStatusCode.fromValue(304),
                    secondAdditionalHeaders = headersOf(HttpHeaders.ETag, "123"),
                )

            val ofrepApi = createOfrepApi(mockEngine)

            val ctx = ImmutableContext(targetingKey = "68cf565d-15cd-4e8b-95a6-9399987164cd")
            val eval1 = ofrepApi.postBulkEvaluateFlags(ctx)
            val eval2 = ofrepApi.postBulkEvaluateFlags(ctx)
            assertEquals(eval1.httpResponse.status.value, 200)
            assertEquals(eval2.httpResponse.status.value, 304)
            assertEquals(2, mockEngine.requestHistory.size)
        }

    @Test
    fun shouldHaveIfNoneNullInTheHeaders(): Unit =
        runBlocking {
            val jsonString = getResourceAsString("ofrep/valid_api_response.json")
            val mockEngine =
                mockEngineWithTwoResponses(
                    firstContent = jsonString,
                    firstStatus = HttpStatusCode.fromValue(200),
                    firstAdditionalHeaders = headersOf(HttpHeaders.ETag, "123"),
                    secondContent = "",
                    secondStatus = HttpStatusCode.fromValue(304),
                    secondAdditionalHeaders = headersOf(HttpHeaders.ETag, "123"),
                )

            val ofrepApi = createOfrepApi(mockEngine)

            val ctx = ImmutableContext(targetingKey = "68cf565d-15cd-4e8b-95a6-9399987164cd")
            val eval1 = ofrepApi.postBulkEvaluateFlags(ctx)
            val eval2 = ofrepApi.postBulkEvaluateFlags(ctx)
            assertEquals(eval1.httpResponse.status.value, 200)
            assertEquals(eval2.httpResponse.status.value, 304)
            assertEquals(2, mockEngine.requestHistory.size)
        }
}
