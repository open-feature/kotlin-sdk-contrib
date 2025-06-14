package dev.openfeature.kotlin.contrib.providers.envvar

/**
 * This is an abstraction to fetch environment variables. It can be used to support
 * environment-specific access or provide additional functionality, like prefixes, casing and even
 * sources like spring configurations which come from different sources. Also, a test double could
 * implement this interface, making the tests independent of the actual environment.
 */
fun interface EnvironmentGateway {
    fun getEnvironmentVariable(key: String): String?
}
