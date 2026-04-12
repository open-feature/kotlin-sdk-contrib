package dev.openfeature.kotlin.contrib.providers.launchdarkly

import android.app.Application
import dev.openfeature.kotlin.sdk.FeatureProvider

/**
 * Creates a [FeatureProvider] backed by the LaunchDarkly Android SDK.
 */
fun createLaunchDarklyFeatureProvider(
    application: Application,
    config: LaunchDarklyConfig,
): LaunchDarklyFeatureProvider = LaunchDarklyFeatureProvider(
    AndroidLaunchDarklyEngine(application, config),
)
