# OpenFeature Timber Logger Adapter

This module provides an adapter for Timber loggers to work with the OpenFeature Kotlin SDK's logging infrastructure on Android.

## Installation

Add the dependency to your project:

```kotlin
dependencies {
    implementation("dev.openfeature.kotlin.contrib.hooks:logging-timber:VERSION")
}
```

## Usage

Use the `TimberLoggerAdapter` to integrate Timber with OpenFeature:

```kotlin
import dev.openfeature.kotlin.contrib.hooks.logging.timber.TimberLoggerAdapter
import dev.openfeature.kotlin.sdk.OpenFeatureAPI
import dev.openfeature.kotlin.sdk.hooks.LoggingHook
import timber.log.Timber

// Set up Timber (typically done in Application.onCreate())
Timber.plant(Timber.DebugTree())

// Create the adapter
val logger = TimberLoggerAdapter()

// Use with OpenFeature logging hook
OpenFeatureAPI.addHooks(listOf(LoggingHook<Any>(logger = logger)))
```

## Requirements

- Android SDK: API 21+
- Timber: 5.0.1 or compatible version

## License

Apache 2.0
