package dev.openfeature.kotlin.contrib.providers.launchdarkly

import android.app.Application
import com.launchdarkly.sdk.EvaluationDetail
import com.launchdarkly.sdk.EvaluationReason
import com.launchdarkly.sdk.LDValue
import com.launchdarkly.sdk.android.LDClientInterface
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.openfeature.kotlin.sdk.Value
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AndroidLaunchDarklyEngineTest {

    private val config = LaunchDarklyConfig(mobileKey = "test-key")

    @Test
    fun getBooleanDetail_clientNull_returnsClientNotReady() {
        val engine = AndroidLaunchDarklyEngine(
            mock<Application>(),
            config,
            LdClientProvider { null },
        )
        val detail = engine.getBooleanDetail("flag", false, null)
        assertEquals(false, detail.value)
        assertEquals(LdErrorKind.CLIENT_NOT_READY, detail.errorKind)
    }

    @Test
    fun getStringDetail_clientNull_returnsClientNotReady() {
        val engine = AndroidLaunchDarklyEngine(mock<Application>(), config, LdClientProvider { null })
        val detail = engine.getStringDetail("flag", "default", null)
        assertEquals("default", detail.value)
        assertEquals(LdErrorKind.CLIENT_NOT_READY, detail.errorKind)
    }

    @Test
    fun getBooleanDetail_clientReturnsValue_mapsSdkDetail() {
        val mockClient = mock<LDClientInterface>()
        val sdkDetail = EvaluationDetail.fromValue(true, 0, EvaluationReason.ruleMatch(0, "r1"))
        every { mockClient.boolVariationDetail("flag", false) } returns sdkDetail
        val engine = AndroidLaunchDarklyEngine(mock<Application>(), config, LdClientProvider { mockClient })
        val detail = engine.getBooleanDetail("flag", false, null)
        assertEquals(true, detail.value)
        assertEquals(0, detail.variationIndex)
        assertEquals("RULE_MATCH", detail.reasonKind)
        assertNull(detail.errorKind)
    }

    @Test
    fun getIntegerDetail_clientReturnsValue_mapsSdkDetail() {
        val mockClient = mock<LDClientInterface>()
        val sdkDetail = EvaluationDetail.fromValue(42, 1, EvaluationReason.fallthrough())
        every { mockClient.intVariationDetail("n", 0) } returns sdkDetail
        val engine = AndroidLaunchDarklyEngine(mock<Application>(), config, LdClientProvider { mockClient })
        val detail = engine.getIntegerDetail("n", 0, null)
        assertEquals(42, detail.value)
        assertEquals(1, detail.variationIndex)
        assertEquals("FALLTHROUGH", detail.reasonKind)
        assertNull(detail.errorKind)
    }

    @Test
    fun getDoubleDetail_clientReturnsValue_mapsSdkDetail() {
        val mockClient = mock<LDClientInterface>()
        val sdkDetail = EvaluationDetail.fromValue(2.5, 0, EvaluationReason.off())
        every { mockClient.doubleVariationDetail("d", 1.0) } returns sdkDetail
        val engine = AndroidLaunchDarklyEngine(mock<Application>(), config, LdClientProvider { mockClient })
        val detail = engine.getDoubleDetail("d", 1.0, null)
        assertEquals(2.5, detail.value)
        assertNull(detail.errorKind)
    }

    @Test
    fun getObjectDetail_clientReturnsValue_mapsSdkDetail() {
        val mockClient = mock<LDClientInterface>()
        val ldJson = LDValue.parse("{\"a\":1}")
        val sdkDetail = EvaluationDetail.fromValue(ldJson, 0, EvaluationReason.off())
        every { mockClient.jsonValueVariationDetail("obj", any()) } returns sdkDetail
        val engine = AndroidLaunchDarklyEngine(mock<Application>(), config, LdClientProvider { mockClient })
        val detail = engine.getObjectDetail("obj", Value.Null, null)
        assertEquals(Value.Structure(mapOf("a" to Value.Integer(1))), detail.value)
        assertEquals("OFF", detail.reasonKind)
        assertNull(detail.errorKind)
    }
}
