rootProject.name = "open-feature-kotlin-sdk-contrib"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            content {
                includeGroup("dev.openfeature")
            }
        }
    }
}

include(":providers:env-var")
include(":providers:ofrep")
include(":providers:launchdarkly")
