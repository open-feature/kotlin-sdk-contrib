package dev.openfeature.kotlin.contrib.providers.launchdarkly

import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals

class IosLaunchDarklyEngineNotReadyTest {

    @Test
    fun getBooleanDetail_clientNull_returnsClientNotReady() {
        val engine = IosLaunchDarklyEngine(
            LaunchDarklyConfig(mobileKey = "test-key"),
            Dispatchers.Unconfined,
        )
        val detail = engine.getBooleanDetail("flag", false, null)
        assertEquals(false, detail.value)
        assertEquals(LdErrorKind.CLIENT_NOT_READY, detail.errorKind)
    }
}
