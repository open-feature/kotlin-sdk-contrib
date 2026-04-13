package dev.openfeature.kotlin.contrib.providers.launchdarkly

import android.app.Application
import com.launchdarkly.sdk.LDContext
import com.launchdarkly.sdk.android.LDClient
import com.launchdarkly.sdk.android.LDClientInterface
import com.launchdarkly.sdk.android.LDConfig

/**
 * Produces the [LDClientInterface] used for evaluations. Invoked once from [AndroidLaunchDarklyEngine.initialize].
 * [DefaultLdClientFactory] calls [LDClient.init]; tests may return a mock or null (client not ready).
 */
internal fun interface LdClientFactory {
    fun createClient(
        application: Application,
        ldConfig: LDConfig,
        ldContext: LDContext,
        initWaitSeconds: Int,
    ): LDClientInterface?
}

internal object DefaultLdClientFactory : LdClientFactory {
    override fun createClient(
        application: Application,
        ldConfig: LDConfig,
        ldContext: LDContext,
        initWaitSeconds: Int,
    ): LDClientInterface = LDClient.init(
        application,
        ldConfig,
        ldContext,
        initWaitSeconds,
    )
}
