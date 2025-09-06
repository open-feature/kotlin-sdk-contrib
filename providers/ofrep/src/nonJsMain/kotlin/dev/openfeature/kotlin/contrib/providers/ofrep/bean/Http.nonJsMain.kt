package dev.openfeature.kotlin.contrib.providers.ofrep.bean

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint

internal actual fun defaultHttpEngine(options: OfrepOptions): HttpClientEngine =
    CIO.create {
        maxConnectionsCount = options.maxIdleConnections
        endpoint {
            keepAliveTime = options.keepAliveDuration.inWholeMilliseconds
            connectTimeout = options.timeout.inWholeMilliseconds
        }
    }
