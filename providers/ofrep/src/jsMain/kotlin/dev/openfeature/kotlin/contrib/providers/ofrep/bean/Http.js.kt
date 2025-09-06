package dev.openfeature.kotlin.contrib.providers.ofrep.bean

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

internal actual fun defaultHttpEngine(options: OfrepOptions): HttpClientEngine =
    Js.create {
        // TODO: consider the following options:
        //   - options.maxIdleConnections
        //   - options.keepAliveDuration
        //   - options.timeout
    }
