# Kotlin OFREP Provider

This provider is designed to use the [OpenFeature Remote Evaluation Protocol (OFREP)](https://openfeature.dev/specification/appendix-c).

## Supported platforms

| Supported | Platform             | Supported versions |
|-----------|----------------------|--------------------|
| ✅         | Android              | SDK 21+            |
| ✅         | JVM                  | JDK 11+            |
| ❌         | Native               |                    |
| ❌         | Javascript (Node.js) |                    |
| ❌         | Javascript (Browser) |                    |
| ❌         | Wasm                 |                    |


## Installation

```kotlin
implementation("dev.openfeature.kotlin.contrib.providers:ofrep:0.1.0")
```

## Usage

To use the `OfrepProvider` create an instance and use it as a provider:

```kotlin
val options = OfrepOptions(endpoint="https://localhost:8080")
val provider = OfrepProvider(options)
OpenFeatureAPI.setProviderAndWait(provider)
```
