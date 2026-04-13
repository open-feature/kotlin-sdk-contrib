package dev.openfeature.kotlin.contrib.providers.launchdarkly

import com.launchdarkly.sdk.android.LDClient
import com.launchdarkly.sdk.android.LDClientInterface

/**
 * Supplies the active [LDClientInterface] for evaluations. Production code uses [DefaultLdClientProvider];
 * tests may substitute a mock [LDClientInterface] or return null to simulate a client that is not ready.
 */
internal fun interface LdClientProvider {
    fun getClient(): LDClientInterface?
}

/**
 * Holds the [LDClient] set by [AndroidLaunchDarklyEngine.initialize] and cleared on [AndroidLaunchDarklyEngine.shutdown].
 */
internal class DefaultLdClientProvider : LdClientProvider {
    @Volatile
    var client: LDClient? = null

    override fun getClient(): LDClientInterface? = client
}
