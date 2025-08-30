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
    }
}

include(":providers:env-var")
include(":providers:ofrep")
