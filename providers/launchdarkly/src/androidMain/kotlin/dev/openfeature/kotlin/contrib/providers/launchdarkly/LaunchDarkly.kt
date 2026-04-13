package dev.openfeature.kotlin.contrib.providers.launchdarkly

import android.app.Application
import dev.openfeature.kotlin.sdk.FeatureProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Creates a [FeatureProvider] backed by the LaunchDarkly Android SDK. */
fun createLaunchDarklyFeatureProvider(
    application: Application,
    config: LaunchDarklyConfig,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
): LaunchDarklyFeatureProvider = LaunchDarklyFeatureProvider(
    AndroidLaunchDarklyEngine(application, config, ioDispatcher),
)
