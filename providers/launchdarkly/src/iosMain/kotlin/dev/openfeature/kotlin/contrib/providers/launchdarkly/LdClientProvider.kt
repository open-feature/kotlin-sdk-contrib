@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.openfeature.kotlin.contrib.providers.launchdarkly

import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.LDClient

/**
 * Supplies the active [LDClient] for evaluations. Production code uses [DefaultLdClientProvider];
 * tests may return null to simulate a client that is not ready.
 */
internal fun interface LdClientProvider {
    fun getClient(): LDClient?
}

/**
 * Holds the [LDClient] set by [IosLaunchDarklyEngine.initialize] and cleared on [IosLaunchDarklyEngine.shutdown].
 */
internal class DefaultLdClientProvider : LdClientProvider {
    var client: LDClient? = null

    override fun getClient(): LDClient? = client
}
