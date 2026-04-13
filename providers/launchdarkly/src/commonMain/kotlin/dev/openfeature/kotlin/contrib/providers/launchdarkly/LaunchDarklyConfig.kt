package dev.openfeature.kotlin.contrib.providers.launchdarkly

/**
 * Configuration for [LaunchDarklyFeatureProvider] (mobile key and SDK tuning).
 * The application chooses which key (e.g. prod vs. UAT); this library does not read BuildConfig.
 *
 * iOS uses LaunchDarkly **11.x** (SwiftPM); minimum deployment **iOS 13** matches SDK 10+ requirements.
 */
data class LaunchDarklyConfig(
    val mobileKey: String,
    /** Max seconds to wait during [com.launchdarkly.sdk.android.LDClient.init]. */
    val initWaitSeconds: Int = 15,
    /** Timeout for [com.launchdarkly.sdk.android.LDClient.identify] (milliseconds). */
    val contextUpdateTimeoutMs: Long = 15_000L,
    val autoEnvAttributes: Boolean = true,
    val debugLogging: Boolean = false,
    /**
     * When false, disables sending analytics events to LaunchDarkly (flag streaming/polling unchanged).
     * See LaunchDarkly iOS SDK `LDConfig.sendEvents` / Android `LDConfig` (SDK 9.12+).
     */
    val sendEvents: Boolean = true,
)
