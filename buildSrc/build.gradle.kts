plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(plugin(libs.plugins.binary.compatibility.validator))
    implementation(plugin(libs.plugins.ktlint))
    implementation(plugin(libs.plugins.kotlin.multiplatform))
    implementation(plugin(libs.plugins.dokka))
    implementation(plugin(libs.plugins.vanniktech.maven.publish))
}

/**
 * Helper function that transforms a Gradle Plugin alias from a Version Catalog into a valid
 * dependency notation for buildSrc. Taken from
 * https://docs.gradle.org/current/userguide/version_catalogs.html#sec:buildsrc-version-catalog
 */
private fun DependencyHandlerScope.plugin(plugin: Provider<PluginDependency>) =
    plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }