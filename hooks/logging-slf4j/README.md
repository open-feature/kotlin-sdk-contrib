# OpenFeature SLF4J Logger Adapter

This module provides an adapter for SLF4J loggers to work with the OpenFeature Kotlin SDK's logging infrastructure.

## Installation

Add the dependency to your project:

```kotlin
dependencies {
    implementation("dev.openfeature.kotlin.contrib.hooks:logging-slf4j:VERSION")
}
```

## Usage

Use the `Slf4jLoggerAdapter` to wrap your existing SLF4J logger:

```kotlin
import dev.openfeature.kotlin.contrib.hooks.logging.slf4j.Slf4jLoggerAdapter
import dev.openfeature.kotlin.sdk.OpenFeatureAPI
import dev.openfeature.kotlin.sdk.hooks.LoggingHook

// Create an SLF4J logger
val slf4jLogger = org.slf4j.LoggerFactory.getLogger("FeatureFlags")

// Wrap it with the adapter
val logger = Slf4jLoggerAdapter(slf4jLogger)

// Use with OpenFeature logging hook
OpenFeatureAPI.addHooks(listOf(LoggingHook<Any>(logger = logger)))
```

### Convenience Factory Method

You can also use the convenience factory method:

```kotlin
val logger = Slf4jLoggerAdapter.getLogger("FeatureFlags")
OpenFeatureAPI.addHooks(listOf(LoggingHook<Any>(logger = logger)))
```

## Requirements

- JVM target: Java 11+
- SLF4J API: 2.0.9 or compatible version

## License

Apache 2.0
