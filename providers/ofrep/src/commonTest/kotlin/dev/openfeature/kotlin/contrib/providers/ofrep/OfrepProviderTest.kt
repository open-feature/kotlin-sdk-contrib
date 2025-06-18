@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.openfeature.kotlin.contrib.providers.ofrep

import dev.openfeature.kotlin.contrib.providers.ofrep.bean.OfrepOptions
import dev.openfeature.kotlin.sdk.Client
import dev.openfeature.kotlin.sdk.EvaluationContext
import dev.openfeature.kotlin.sdk.EvaluationMetadata
import dev.openfeature.kotlin.sdk.FeatureProvider
import dev.openfeature.kotlin.sdk.FlagEvaluationDetails
import dev.openfeature.kotlin.sdk.ImmutableContext
import dev.openfeature.kotlin.sdk.OpenFeatureAPI
import dev.openfeature.kotlin.sdk.Value
import dev.openfeature.kotlin.sdk.events.OpenFeatureProviderEvents
import dev.openfeature.kotlin.sdk.exceptions.ErrorCode
import dev.openfeature.kotlin.sdk.exceptions.OpenFeatureError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private fun createOfrepProvider(mockEngine: MockEngine) =
    OfrepProvider(
        OfrepOptions(endpoint = FAKE_ENDPOINT, httpClientEngine = mockEngine),
    )

private suspend fun withClient(
    provider: FeatureProvider,
    initialContext: EvaluationContext,
    dispatcher: CoroutineDispatcher,
    body: (client: Client) -> Unit,
) {
    OpenFeatureAPI.setProviderAndWait(provider, initialContext, dispatcher)
    try {
        val client = OpenFeatureAPI.getClient()
        body(client)
    } finally {
        OpenFeatureAPI.shutdown()
    }
}

@OptIn(ExperimentalUuidApi::class)
class OfrepProviderTest {
    private val defaultEvalCtx: EvaluationContext =
        ImmutableContext(targetingKey = Uuid.random().toHexString())

    @AfterTest
    fun after() =
        runTest {
            OpenFeatureAPI.shutdown()
        }

    @Test
    fun `should have a provider metadata`() {
        val provider = OfrepProvider(OfrepOptions(endpoint = "http://localhost:1031"))
        assertEquals("OFREP Provider", provider.metadata.name)
    }

    @Test
    fun `should be in Fatal status if 401 error during initialise`(): Unit =
        runTest {
            val mockEngine =
                mockEngineWithOneResponse(getResourceAsString("ofrep/valid_api_response.json"), status = HttpStatusCode.fromValue(401))

            val provider = createOfrepProvider(mockEngine)
            var providerErrorReceived = false

            launch {
                provider.observe().filterIsInstance<OpenFeatureProviderEvents.ProviderError>().take(1).collect {
                    providerErrorReceived = true
                }
            }
            runCurrent()
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                runCurrent()
                assert(providerErrorReceived) { "ProviderError event was not received" }
            }
        }

    @Test
    fun `should be in Fatal status if 403 error during initialise`(): Unit =
        runTest {
            val mockEngine =
                mockEngineWithOneResponse(getResourceAsString("ofrep/valid_api_response.json"), status = HttpStatusCode.fromValue(403))

            val provider = createOfrepProvider(mockEngine)
            var providerErrorReceived = false

            launch {
                provider.observe().filterIsInstance<OpenFeatureProviderEvents.ProviderError>().take(1).collect {
                    providerErrorReceived = true
                }
            }
            runCurrent()
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                runCurrent()
                assert(providerErrorReceived) { "ProviderError event was not received" }
            }
        }

    @Test
    fun `should be in Error status if 429 error during initialise`(): Unit =
        runTest {
            val mockEngine =
                mockEngineWithOneResponse(
                    getResourceAsString("ofrep/valid_api_response.json"),
                    status = HttpStatusCode.fromValue(429),
                    additionalHeaders = headersOf(HttpHeaders.RetryAfter, "3"),
                )

            val provider = createOfrepProvider(mockEngine)
            var providerErrorReceived = false
            var exceptionReceived: Throwable? = null

            launch {
                provider.observe().filterIsInstance<OpenFeatureProviderEvents.ProviderError>().take(1).collect {
                    providerErrorReceived = true
                    exceptionReceived = it.error
                }
            }
            runCurrent()
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                runCurrent()
                assert(providerErrorReceived) { "ProviderError event was not received" }
                assert(exceptionReceived is OpenFeatureError.GeneralError) { "The exception is not of type GeneralError" }
                assert(exceptionReceived?.message == "Rate limited") { "The exception's message is not correct" }
            }
        }

    @Test
    fun `should be in Error status if error targeting key is empty`(): Unit =
        runTest {
            val mockEngine = mockEngineWithOneResponse(getResourceAsString("ofrep/valid_api_response.json"))

            val provider = createOfrepProvider(mockEngine)
            var providerErrorReceived = false
            var exceptionReceived: Throwable? = null

            launch {
                provider.observe().filterIsInstance<OpenFeatureProviderEvents.ProviderError>().take(1).collect {
                    providerErrorReceived = true
                    exceptionReceived = it.error
                }
            }
            runCurrent()
            val evalCtx = ImmutableContext(targetingKey = "")
            withClient(provider, evalCtx, Dispatchers.IO) { client ->
                runCurrent()
                assert(providerErrorReceived) { "ProviderError event was not received" }
                assert(
                    exceptionReceived is OpenFeatureError.TargetingKeyMissingError,
                ) { "The exception is not of type TargetingKeyMissingError" }
            }
        }

    @Test
    fun `should be in Error status if error targeting key is missing`(): Unit =
        runTest {
            val mockEngine = mockEngineWithOneResponse(getResourceAsString("ofrep/valid_api_response.json"))

            val provider = createOfrepProvider(mockEngine)
            var providerErrorReceived = false
            var exceptionReceived: Throwable? = null

            launch {
                provider.observe().filterIsInstance<OpenFeatureProviderEvents.ProviderError>().take(1).collect {
                    providerErrorReceived = true
                    exceptionReceived = it.error
                }
            }
            runCurrent()
            val evalCtx = ImmutableContext()
            withClient(provider, evalCtx, Dispatchers.IO) { client ->
                runCurrent()
                assert(providerErrorReceived) { "ProviderError event was not received" }
                assert(
                    exceptionReceived is OpenFeatureError.TargetingKeyMissingError,
                ) { "The exception is not of type TargetingKeyMissingError" }
            }
        }

    @Test
    fun `should be in error status if error invalid context`(): Unit =
        runTest {
            val mockEngine =
                mockEngineWithOneResponse(getResourceAsString("ofrep/invalid_context.json"), status = HttpStatusCode.fromValue(400))
            val provider = createOfrepProvider(mockEngine)
            var providerErrorReceived = false
            var exceptionReceived: Throwable? = null

            launch {
                provider.observe().filterIsInstance<OpenFeatureProviderEvents.ProviderError>().take(1).collect {
                    providerErrorReceived = true
                    exceptionReceived = it.error
                }
            }
            runCurrent()
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                runCurrent()
                assert(providerErrorReceived) { "ProviderError event was not received" }
                assert(exceptionReceived is OpenFeatureError.InvalidContextError) { "The exception is not of type InvalidContextError" }
            }
        }

    @Test
    fun `should be in error status if error parse error`(): Unit =
        runTest {
            val mockEngine =
                mockEngineWithOneResponse(getResourceAsString("ofrep/parse_error.json"), status = HttpStatusCode.fromValue(400))

            val provider = createOfrepProvider(mockEngine)
            var providerErrorReceived = false
            var exceptionReceived: Throwable? = null

            launch {
                provider.observe().filterIsInstance<OpenFeatureProviderEvents.ProviderError>().take(1).collect {
                    providerErrorReceived = true
                    exceptionReceived = it.error
                }
            }
            runCurrent()
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                runCurrent()
                assert(providerErrorReceived) { "ProviderError event was not received" }
                assert(exceptionReceived is OpenFeatureError.ParseError) { "The exception is not of type ParseError" }
            }
        }

    @Test
    fun `should return a flag not found error if the flag does not exist`(): Unit =
        runTest {
            val mockEngine = mockEngineWithOneResponse(getResourceAsString("ofrep/valid_api_response.json"))
            val provider = createOfrepProvider(mockEngine)
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                val got = client.getBooleanDetails("non-existent-flag", false)
                val want =
                    FlagEvaluationDetails<Boolean>(
                        flagKey = "non-existent-flag",
                        value = false,
                        variant = null,
                        reason = "ERROR",
                        errorCode = ErrorCode.FLAG_NOT_FOUND,
                        errorMessage = "Could not find flag named: non-existent-flag",
                    )
                assertEquals(want, got)
            }
        }

    @Test
    fun `should return evaluation details if the flag exists`(): Unit =
        runTest {
            val mockEngine =
                mockEngineWithOneResponse(
                    getResourceAsString("ofrep/valid_api_short_response.json"),
                )
            val provider = createOfrepProvider(mockEngine)
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                val got = client.getStringDetails("title-flag", "default")
                val want =
                    FlagEvaluationDetails<String>(
                        flagKey = "title-flag",
                        value = "GO Feature Flag",
                        variant = "default_title",
                        reason = "DEFAULT",
                        errorCode = null,
                        errorMessage = null,
                        metadata =
                            EvaluationMetadata
                                .builder()
                                .putString("description", "This flag controls the title of the feature flag")
                                .putString("title", "Feature Flag Title")
                                .build(),
                    )
                assertEquals(want, got)
            }
        }

    @Test
    fun `should return parse error if the API returns the error`(): Unit =
        runTest {
            val mockEngine =
                mockEngineWithOneResponse(
                    getResourceAsString("ofrep/valid_1_flag_in_parse_error.json"),
                )
            val provider = createOfrepProvider(mockEngine)
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                val got = client.getStringDetails("my-other-flag", "default")
                val want =
                    FlagEvaluationDetails<String>(
                        flagKey = "my-other-flag",
                        value = "default",
                        variant = null,
                        reason = "ERROR",
                        errorCode = ErrorCode.PARSE_ERROR,
                        errorMessage = "Error details about PARSE_ERROR",
                    )
                assertEquals(want, got)
            }
        }

    @Test
    fun `should send a context changed event if context has changed`(): Unit =
        runTest {
            val mockEngine =
                mockEngineWithTwoResponses(
                    firstContent = getResourceAsString("ofrep/valid_api_response.json"),
                    secondContent = getResourceAsString("ofrep/valid_api_response_2.json"),
                )
            val provider = createOfrepProvider(mockEngine)
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->

                // TODO: should change when we have a way to observe context changes event
                //       check issue https://github.com/open-feature/kotlin-sdk/issues/107
                var providerStaleEventReceived = false
                var providerReadyEventReceived = false

                launch {
                    provider.observe().filterIsInstance<OpenFeatureProviderEvents.ProviderStale>().take(1).collect {
                        providerStaleEventReceived = true
                    }
                    provider.observe().filterIsInstance<OpenFeatureProviderEvents.ProviderReady>().take(1).collect {
                        providerReadyEventReceived = true
                    }
                }
                runCurrent()
                Thread.sleep(1000) // waiting to be sure that setEvaluationContext has been processed
                val newEvalCtx = ImmutableContext(targetingKey = Uuid.random().toHexString())
                OpenFeatureAPI.setEvaluationContext(newEvalCtx)
                Thread.sleep(1000) // waiting to be sure that setEvaluationContext has been processed
                runCurrent()
                assert(providerStaleEventReceived) { "ProviderStale event was not received" }
                assert(providerReadyEventReceived) { "ProviderReady event was not received" }
            }
        }

    @Test
    fun `should not try to call the API before Retry-After header`(): Unit =
        runTest {
            val mockEngine =
                mockEngineWithOneResponse(
                    status = HttpStatusCode.fromValue(429),
                    additionalHeaders = headersOf("Retry-After", "3"),
                )
            val provider =
                OfrepProvider(
                    OfrepOptions(
                        pollingInterval = 100.milliseconds,
                        endpoint = FAKE_ENDPOINT,
                        httpClientEngine = mockEngine,
                    ),
                )
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                client.getStringDetails("my-other-flag", "default")
                client.getStringDetails("my-other-flag", "default")
                Thread.sleep(2000) // we wait 2 seconds to let the polling loop run
                assertEquals(1, mockEngine.requestHistory.size)
            }
        }

    @Test
    fun `should return a valid evaluation for Boolean`(): Unit =
        runTest {
            val mockEngine =
                mockEngineWithOneResponse(getResourceAsString("ofrep/valid_api_response.json"))
            val provider = createOfrepProvider(mockEngine)
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                val got = client.getBooleanDetails("bool-flag", false)
                val want =
                    FlagEvaluationDetails<Boolean>(
                        flagKey = "bool-flag",
                        value = true,
                        variant = "variantA",
                        reason = "TARGETING_MATCH",
                        errorCode = null,
                        errorMessage = null,
                        metadata =
                            EvaluationMetadata
                                .builder()
                                .putBoolean("additionalProp1", true)
                                .putString("additionalProp2", "value")
                                .putInt("additionalProp3", 123)
                                .build(),
                    )
                assertEquals(want, got)
            }
        }

    @Test
    fun `should return a valid evaluation for Int`(): Unit =
        runTest {
            val mockEngine =
                mockEngineWithOneResponse(getResourceAsString("ofrep/valid_api_response.json"))
            val provider = createOfrepProvider(mockEngine)
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                val got = client.getIntegerDetails("int-flag", 1)
                val want =
                    FlagEvaluationDetails<Int>(
                        flagKey = "int-flag",
                        value = 1234,
                        variant = "variantA",
                        reason = "TARGETING_MATCH",
                        errorCode = null,
                        errorMessage = null,
                        metadata =
                            EvaluationMetadata
                                .builder()
                                .putBoolean("additionalProp1", true)
                                .putString("additionalProp2", "value")
                                .putInt("additionalProp3", 123)
                                .build(),
                    )
                assertEquals(want, got)
            }
        }

    @Test
    fun `should return a valid evaluation for Double`(): Unit =
        runTest {
            val mockEngine = mockEngineWithOneResponse(getResourceAsString("ofrep/valid_api_response.json"))
            val provider = createOfrepProvider(mockEngine)
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                val got = client.getDoubleDetails("double-flag", 1.1)
                val want =
                    FlagEvaluationDetails<Double>(
                        flagKey = "double-flag",
                        value = 12.34,
                        variant = "variantA",
                        reason = "TARGETING_MATCH",
                        errorCode = null,
                        errorMessage = null,
                        metadata =
                            EvaluationMetadata
                                .builder()
                                .putBoolean("additionalProp1", true)
                                .putString("additionalProp2", "value")
                                .putInt("additionalProp3", 123)
                                .build(),
                    )
                assertEquals(want, got)
            }
        }

    @Test
    fun `should return a valid evaluation for String`(): Unit =
        runTest {
            val mockEngine = mockEngineWithOneResponse(getResourceAsString("ofrep/valid_api_response.json"))
            val provider = createOfrepProvider(mockEngine)
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                val got = client.getStringDetails("string-flag", "default")
                val want =
                    FlagEvaluationDetails<String>(
                        flagKey = "string-flag",
                        value = "1234value",
                        variant = "variantA",
                        reason = "TARGETING_MATCH",
                        errorCode = null,
                        errorMessage = null,
                        metadata =
                            EvaluationMetadata
                                .builder()
                                .putBoolean("additionalProp1", true)
                                .putString("additionalProp2", "value")
                                .putInt("additionalProp3", 123)
                                .build(),
                    )
                assertEquals(want, got)
            }
        }

    @Test
    fun `should return a valid evaluation for List`(): Unit =
        runTest {
            val mockEngine = mockEngineWithOneResponse(getResourceAsString("ofrep/valid_api_response.json"))
            val provider = createOfrepProvider(mockEngine)
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                val got =
                    client.getObjectDetails(
                        "array-flag",
                        Value.List(MutableList(1) { Value.Integer(1234567890) }),
                    )

                val want =
                    FlagEvaluationDetails<Value>(
                        flagKey = "array-flag",
                        value = Value.List(listOf(Value.Integer(1234), Value.Integer(5678))),
                        variant = "variantA",
                        reason = "TARGETING_MATCH",
                        errorCode = null,
                        errorMessage = null,
                        metadata =
                            EvaluationMetadata
                                .builder()
                                .putBoolean("additionalProp1", true)
                                .putString("additionalProp2", "value")
                                .putInt("additionalProp3", 123)
                                .build(),
                    )
                assertEquals(want, got)
            }
        }

    @Test
    fun `should return a valid evaluation for Map`(): Unit =
        runTest {
            val mockEngine = mockEngineWithOneResponse(getResourceAsString("ofrep/valid_api_response.json"))
            val provider = createOfrepProvider(mockEngine)
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                val got =
                    client.getObjectDetails(
                        "object-flag",
                        Value.Structure(
                            mapOf(
                                "default" to Value.Boolean(true),
                            ),
                        ),
                    )

                val want =
                    FlagEvaluationDetails<Value>(
                        flagKey = "object-flag",
                        value =
                            Value.Structure(
                                mapOf(
                                    "testValue" to
                                        Value.Structure(
                                            mapOf(
                                                "toto" to Value.Integer(1234),
                                            ),
                                        ),
                                ),
                            ),
                        variant = "variantA",
                        reason = "TARGETING_MATCH",
                        errorCode = null,
                        errorMessage = null,
                        metadata =
                            EvaluationMetadata
                                .builder()
                                .putBoolean("additionalProp1", true)
                                .putString("additionalProp2", "value")
                                .putInt("additionalProp3", 123)
                                .build(),
                    )
                assertEquals(want, got)
            }
        }

    @Test
    fun `should return TypeMismatch Bool`(): Unit =
        runTest {
            val mockEngine = mockEngineWithOneResponse(getResourceAsString("ofrep/valid_api_response.json"))
            val provider = createOfrepProvider(mockEngine)
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                val got = client.getBooleanDetails("object-flag", false)
                val want =
                    FlagEvaluationDetails<Boolean>(
                        flagKey = "object-flag",
                        value = false,
                        variant = null,
                        reason = "ERROR",
                        errorCode = ErrorCode.TYPE_MISMATCH,
                        errorMessage =
                            "Type mismatch: expect Boolean - Unsupported type for: " +
                                "Structure(structure={testValue=Structure(structure={toto=Integer(integer=1234)})})",
                    )
                assertEquals(want, got)
            }
        }

    @Test
    fun `should return TypeMismatch String`(): Unit =
        runTest {
            val mockEngine = mockEngineWithOneResponse(getResourceAsString("ofrep/valid_api_response.json"))
            val provider = createOfrepProvider(mockEngine)
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                val got = client.getStringDetails("object-flag", "default")
                val want =
                    FlagEvaluationDetails<String>(
                        flagKey = "object-flag",
                        value = "default",
                        variant = null,
                        reason = "ERROR",
                        errorCode = ErrorCode.TYPE_MISMATCH,
                        errorMessage =
                            "Type mismatch: expect String - Unsupported type for: " +
                                "Structure(structure={testValue=Structure(structure={toto=Integer(integer=1234)})})",
                    )
                assertEquals(want, got)
            }
        }

    @Test
    fun `should return TypeMismatch Double`(): Unit =
        runTest {
            val mockEngine = mockEngineWithOneResponse(getResourceAsString("ofrep/valid_api_response.json"))
            val provider = createOfrepProvider(mockEngine)
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                val got = client.getDoubleDetails("object-flag", 1.233)
                val want =
                    FlagEvaluationDetails<Double>(
                        flagKey = "object-flag",
                        value = 1.233,
                        variant = null,
                        reason = "ERROR",
                        errorCode = ErrorCode.TYPE_MISMATCH,
                        errorMessage =
                            "Type mismatch: expect Double - Unsupported type for: " +
                                "Structure(structure={testValue=Structure(structure={toto=Integer(integer=1234)})})",
                    )
                assertEquals(want, got)
            }
        }

    @Test
    fun `should have different result if waiting for next polling interval`(): Unit =
        runTest {
            val mockEngine =
                mockEngineWithTwoResponses(
                    firstContent = getResourceAsString("ofrep/valid_api_short_response.json"),
                    secondContent = getResourceAsString("ofrep/valid_api_response_2.json"),
                )

            val provider =
                OfrepProvider(
                    OfrepOptions(
                        pollingInterval = 100.milliseconds,
                        endpoint = FAKE_ENDPOINT,
                        httpClientEngine = mockEngine,
                    ),
                )
            withClient(provider, defaultEvalCtx, Dispatchers.IO) { client ->
                val got = client.getStringDetails("badge-class2", "default")
                val want =
                    FlagEvaluationDetails<String>(
                        flagKey = "badge-class2",
                        value = "green",
                        variant = "nocolor",
                        reason = "DEFAULT",
                        errorCode = null,
                        errorMessage = null,
                    )
                assertEquals(want, got)
                Thread.sleep(1000)
                val got2 = client.getStringDetails("badge-class2", "default")
                val want2 =
                    FlagEvaluationDetails<String>(
                        flagKey = "badge-class2",
                        value = "blue",
                        variant = "xxxx",
                        reason = "TARGETING_MATCH",
                        errorCode = null,
                        errorMessage = null,
                    )
                assertEquals(want2, got2)
            }
        }
}
