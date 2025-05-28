package dev.openfeature.kotlin.contrib.providers.envvar

@JsModule("process")
@JsNonModule
external val process: dynamic

actual fun platformSpecificEnvironmentGateway(): EnvironmentGateway =
    EnvironmentGateway { key ->
        process.env[key]?.toString()
    }
