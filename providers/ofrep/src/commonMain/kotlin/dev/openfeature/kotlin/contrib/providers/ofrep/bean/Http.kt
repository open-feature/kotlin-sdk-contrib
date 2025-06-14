package dev.openfeature.kotlin.contrib.providers.ofrep.bean

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

private fun defaultHttpEngine(options: OfrepOptions): HttpClientEngine =
    CIO.create {
        maxConnectionsCount = options.maxIdleConnections
        endpoint {
            keepAliveTime = options.keepAliveDuration.inWholeMilliseconds
            connectTimeout = options.timeout.inWholeMilliseconds
        }
    }

internal fun createHttpClient(options: OfrepOptions): HttpClient {
    val httpEngine = options.httpClientEngine ?: defaultHttpEngine(options)
    return HttpClient(httpEngine) {
        install(ContentNegotiation) {
            json()
        }
    }
}
