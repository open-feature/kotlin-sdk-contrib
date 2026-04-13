package dev.openfeature.kotlin.contrib.providers.launchdarkly

import dev.openfeature.kotlin.sdk.FeatureProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Creates a [FeatureProvider] backed by the LaunchDarkly iOS SDK (SwiftPM). */
fun createLaunchDarklyFeatureProvider(
    config: LaunchDarklyConfig,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
): LaunchDarklyFeatureProvider = LaunchDarklyFeatureProvider(
    IosLaunchDarklyEngine(config, mainDispatcher),
)
