package dev.openfeature.kotlin.contrib.providers.ofrep.bean

import okhttp3.Headers
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class OfrepOptions(
    /**
     * The endpoint of the OFREP API.
     *
     * Example: `https://mydomain.com/gofeatureflagproxy/`
     */
    val endpoint: String,
    /**
     * Timeout of the OFREP API calls.
     *
     * Default: `10.seconds`
     */
    val timeout: Duration = 10.seconds,
    /**
     * MaxIdleConnections is the maximum number of connexions in the connexion pool.
     *
     * Default: `1000`
     */
    val maxIdleConnections: Int = 1000,
    /**
     * The time to keep the connection open.
     *
     * Default: `2.hours`
     */
    val keepAliveDuration: Duration = 2.hours,
    /**
     * Headers to add to the OFREP calls
     * Default: empty
     */
    val headers: Headers? = null,
    /**
     * Polling interval to refresh the flags
     * Default: `5.minutes`
     */
    val pollingInterval: Duration = 5.minutes,
)
