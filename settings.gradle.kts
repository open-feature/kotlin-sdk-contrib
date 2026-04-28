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
        mavenCentral()
    }
}

include(":providers:env-var")
include(":providers:ofrep")
