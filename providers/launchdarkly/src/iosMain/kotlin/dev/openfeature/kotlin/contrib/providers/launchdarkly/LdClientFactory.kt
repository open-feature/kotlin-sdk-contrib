@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.openfeature.kotlin.contrib.providers.launchdarkly

import kotlinx.coroutines.suspendCancellableCoroutine
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.LDClient
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.LDConfig
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.LDContext
import kotlin.coroutines.resume

/**
 * Produces the [LDClient] used for evaluations. Invoked once from [IosLaunchDarklyEngine.initialize].
 * [DefaultLdClientFactory] runs [LDClient.startWithConfiguration] then [LDClient.get]; tests may return a mock or null.
 */
internal fun interface LdClientFactory {
    suspend fun createClient(
        ldConfig: LDConfig,
        ldContext: LDContext,
        initWaitSeconds: Double,
    ): LDClient?
}

internal object DefaultLdClientFactory : LdClientFactory {
    override suspend fun createClient(
        ldConfig: LDConfig,
        ldContext: LDContext,
        initWaitSeconds: Double,
    ): LDClient? {
        suspendCancellableCoroutine { continuation ->
            LDClient.startWithConfiguration(
                configuration = ldConfig,
                context = ldContext,
                startWaitSeconds = initWaitSeconds,
                completion = {
                    continuation.resume(Unit)
                },
            )
        }
        return LDClient.get()
    }
}
