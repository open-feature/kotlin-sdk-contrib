package dev.openfeature.kotlin.contrib.providers.envvar

actual fun platformSpecificEnvironmentGateway(): EnvironmentGateway =
    EnvironmentGateway { key ->
        System.getenv(key)
    }
