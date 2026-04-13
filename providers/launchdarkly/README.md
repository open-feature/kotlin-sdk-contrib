# LaunchDarkly Kotlin Provider

[`LaunchDarklyFeatureProvider`](src/commonMain/kotlin/dev/openfeature/kotlin/contrib/providers/launchdarkly/LaunchDarklyFeatureProvider.kt) wraps the LaunchDarkly Android and iOS SDKs behind the OpenFeature [`FeatureProvider`](https://openfeature.dev/specification/sections/providers) API.

Platform factories:

- **Android** — `createLaunchDarklyFeatureProvider(Application, LaunchDarklyConfig, …)`
- **iOS** — `createLaunchDarklyFeatureProvider(LaunchDarklyConfig, …)`

## Installation

**Gradle (Kotlin DSL)** — in a Kotlin Multiplatform module, add the dependency on the shared source set:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("dev.openfeature.kotlin.contrib.providers:launchdarkly:latest-version")
        }
    }
}
```

**Gradle (Groovy)** — equivalent:

```groovy
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation 'dev.openfeature.kotlin.contrib.providers:launchdarkly:latest-version'
            }
        }
    }
}
```

**Maven**

```xml
<dependency>
    <groupId>dev.openfeature.kotlin.contrib.providers</groupId>
    <artifactId>launchdarkly</artifactId>
    <version>latest-version</version>
</dependency>
```

Also add the [OpenFeature Kotlin SDK](https://github.com/open-feature/kotlin-sdk) (`dev.openfeature:kotlin-sdk`) on the same source sets, using the version your project already pins—for `OpenFeatureAPI`, evaluation types, and [`MultiProvider`](https://github.com/open-feature/kotlin-sdk/blob/main/kotlin-sdk/src/commonMain/kotlin/dev/openfeature/kotlin/sdk/multiprovider/MultiProvider.kt).

## Example: Metro + Kotlin Multiplatform

Below, `LaunchDarklyConfig` is provided from **commonMain**, platform `FeatureProvider` bindings differ only where the Android factory needs an `Application`, and startup wires a [`MultiProvider`](https://github.com/open-feature/kotlin-sdk/blob/main/kotlin-sdk/src/commonMain/kotlin/dev/openfeature/kotlin/sdk/multiprovider/MultiProvider.kt) into [`OpenFeatureAPI`](https://github.com/open-feature/kotlin-sdk/blob/main/kotlin-sdk/src/commonMain/kotlin/dev/openfeature/kotlin/sdk/OpenFeatureAPI.kt).

### `commonMain` — config

```kotlin
import dev.openfeature.kotlin.contrib.providers.launchdarkly.LaunchDarklyConfig

@Provides
fun launchDarklyConfig(): LaunchDarklyConfig =
    LaunchDarklyConfig(
        mobileKey = "<your-mobile-key>",
        debugLogging = false,
    )
```

### `androidMain` — register the provider

```kotlin
import android.app.Application
import dev.openfeature.kotlin.contrib.providers.launchdarkly.createLaunchDarklyFeatureProvider
import dev.openfeature.kotlin.sdk.FeatureProvider

@ContributesTo(ApplicationScope::class)
@BindingContainer
object OpenFeatureComponent {

    @Provides
    @IntoSet
    fun launchDarklyProvider(
        application: Application,
        launchDarklyConfig: LaunchDarklyConfig,
    ): FeatureProvider = createLaunchDarklyFeatureProvider(application, launchDarklyConfig)
}
```

### `iosMain` — register the provider

```kotlin
import dev.openfeature.kotlin.contrib.providers.launchdarkly.createLaunchDarklyFeatureProvider
import dev.openfeature.kotlin.sdk.FeatureProvider

@ContributesTo(ApplicationScope::class)
@BindingContainer
object OpenFeatureComponent {

    @Provides
    @IntoSet
    fun launchDarklyProvider(
        launchDarklyConfig: LaunchDarklyConfig,
    ): FeatureProvider = createLaunchDarklyFeatureProvider(launchDarklyConfig)
}
```

### `commonMain` — start OpenFeature with a `MultiProvider`

Inject every contributed `FeatureProvider` as a `Set<FeatureProvider>`, combine them with [`MultiProvider`](https://github.com/open-feature/kotlin-sdk/blob/main/kotlin-sdk/src/commonMain/kotlin/dev/openfeature/kotlin/sdk/multiprovider/MultiProvider.kt), then call [`OpenFeatureAPI.setProviderAndWait`](https://github.com/open-feature/kotlin-sdk/blob/main/kotlin-sdk/src/commonMain/kotlin/dev/openfeature/kotlin/sdk/OpenFeatureAPI.kt) (or `setProvider` if you prefer not to suspend).

```kotlin
import dev.openfeature.kotlin.sdk.EvaluationContext
import dev.openfeature.kotlin.sdk.FeatureProvider
import dev.openfeature.kotlin.sdk.ImmutableContext
import dev.openfeature.kotlin.sdk.OpenFeatureAPI
import dev.openfeature.kotlin.sdk.multiprovider.MultiProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class OpenFeatureInitializer(
    private val dispatcher: CoroutineDispatcher,
    private val featureFlagProviders: Set<FeatureProvider>,
) {
    suspend fun initialize() {
        if (featureFlagProviders.isEmpty()) return

        withContext(dispatcher) {
            val multiProvider = MultiProvider(featureFlagProviders.toList())
            val initialContext: EvaluationContext = ImmutableContext(targetingKey = "anonymous")
            OpenFeatureAPI.setProviderAndWait(
                provider = multiProvider,
                initialContext = initialContext,
                dispatcher = dispatcher,
            )
        }
    }
}
```

Replace `ImmutableContext(…)` and the targeting key with whatever evaluation context your app uses after login or identification.

## See also

- [`LaunchDarklyConfig`](src/commonMain/kotlin/dev/openfeature/kotlin/contrib/providers/launchdarkly/LaunchDarklyConfig.kt) — mobile key and SDK tuning options
