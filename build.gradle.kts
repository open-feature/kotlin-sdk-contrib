plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.nexus.publish)
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.binary.compatibility.validator) apply false
}

allprojects {
    extra["groupId"] = "dev.openfeature.kotlin.contrib"
    ext["version"] = "0.1.0"
}
group = project.extra["groupId"].toString()
version = project.extra["version"].toString()

subprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
        apply(plugin = "org.jetbrains.kotlinx.binary-compatibility-validator")
    }
}

nexusPublishing {
    this.repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"),
            )
            username = System.getenv("OSSRH_USERNAME")
            password = System.getenv("OSSRH_PASSWORD")
        }
    }
}
