package dev.openfeature.kotlin.contrib.providers.ofrep.bean

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

internal expect fun defaultHttpEngine(options: OfrepOptions): HttpClientEngine

internal fun createHttpClient(options: OfrepOptions): HttpClient {
    val httpEngine = options.httpClientEngine ?: defaultHttpEngine(options)
    return HttpClient(httpEngine) {
        install(ContentNegotiation) {
            json()
        }
    }
}
