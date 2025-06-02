package dev.openfeature.kotlin.contrib.providers.envvar

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

actual fun platformSpecificEnvironmentGateway(): EnvironmentGateway =
    @OptIn(ExperimentalForeignApi::class)
    EnvironmentGateway { key ->
        getenv(key)?.toKString()
    }
