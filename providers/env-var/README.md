# Environment Variables Kotlin Provider

Environment Variables provider allows you to read feature flags from the [process's environment](https://en.wikipedia.org/wiki/Environment_variable).

## Supported platforms

| Supported | Platform             | Supported versions                                                             |
|-----------|----------------------|--------------------------------------------------------------------------------|
| ❌         | Android              |                                                                                |
| ✅         | JVM                  | JDK 11+                                                                        |
| ✅         | Native               | Linux x64                                                                      |
| ❌         | Native               | [Other native targets](https://kotlinlang.org/docs/native-target-support.html) |
| ✅         | Javascript (Node.js) |                                                                                |
| ❌         | Javascript (Browser) |                                                                                |
| ❌         | Wasm                 |                                                                                |


## Installation

```xml
<dependency>
    <groupId>dev.openfeature.kotlin.contrib.providers</groupId>
    <artifactId>env-var</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Usage

To use the `EnvVarProvider` create an instance and use it as a provider:

```kotlin
    val provider = EnvVarProvider()
    OpenFeatureAPI.setProviderAndWait(provider)
```

### Configuring different methods for fetching environment variables

This provider defines an `EnvironmentGateway` interface, which is used to access the actual environment variables.
The method [`platformSpecificEnvironmentGateway`][platformSpecificEnvironmentGateway], which is implemented for each supported platform, returns a default implementation.

```kotlin
    val testFake = EnvironmentGateway { arg -> "true" } // always returns true
    
    val provider = EnvVarProvider(testFake)
    OpenFeatureAPI.getInstance().setProvider(provider)
```

### Key transformation

This provider supports transformation of keys to support different patterns used for naming feature flags and for
naming environment variables, e.g. SCREAMING_SNAKE_CASE env variables vs. hyphen-case keys for feature flags.
It supports chaining/combining different transformers incl. self-written ones by providing a transforming function in the constructor.
Currently, the following transformations are supported out of the box:

- converting to lower case (e.g. `Feature.Flag` => `feature.flag`)
- converting to UPPER CASE (e.g. `Feature.Flag` => `FEATURE.FLAG`)
- converting hyphen-case to SCREAMING_SNAKE_CASE (e.g. `Feature-Flag` => `FEATURE_FLAG`)
- convert to camelCase (e.g. `FEATURE_FLAG` => `featureFlag`)
- replace '_' with '.' (e.g. `feature_flag` => `feature.flag`)
- replace '.' with '_' (e.g. `feature.flag` => `feature_flag`)

**Examples:**

1. hyphen-case feature flag names to screaming snake-case environment variables:

   ```kotlin
       // Definition of the EnvVarProvider:
       val provider = EnvVarProvider(EnvironmentKeyTransformer.hyphenCaseToScreamingSnake())
   ```

2. chained/composed transformations:

   ```kotlin
       // Definition of the EnvVarProvider:
       val keyTransformer = EnvironmentKeyTransformer
           .toLowerCaseTransformer()
           .andThen(EnvironmentKeyTransformer.replaceUnderscoreWithDotTransformer())
   
       val provider = EnvVarProvider(keyTransformer)
   ```

3. freely defined transformation function:

   ```kotlin
       // Definition of the EnvVarProvider:   
       val keyTransformer = EnvironmentKeyTransformer { key -> key.substring(1) }
       val provider = EnvVarProvider(keyTransformer)
   ```

<!-- links -->

[platformSpecificEnvironmentGateway]: src/commonMain/kotlin/dev/openfeature/kotlin/contrib/providers/envvar/PlatformSpecificEnvironmentGateway.kt
