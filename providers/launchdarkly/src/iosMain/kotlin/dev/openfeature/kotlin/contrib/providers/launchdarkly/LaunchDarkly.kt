package dev.openfeature.kotlin.contrib.providers.launchdarkly

import dev.openfeature.kotlin.sdk.FeatureProvider

/**
 * Creates a [FeatureProvider] backed by the LaunchDarkly iOS SDK (SwiftPM).
 */
fun createLaunchDarklyFeatureProvider(
    config: LaunchDarklyConfig,
): LaunchDarklyFeatureProvider = LaunchDarklyFeatureProvider(
    IosLaunchDarklyEngine(config),
)
