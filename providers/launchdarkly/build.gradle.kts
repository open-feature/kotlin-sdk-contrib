plugins {
    id("com.android.library")
    id("dev.openfeature.provider-conventions")
    // Mocking in androidUnitTest; requires a Mokkery build compatible with the project's Kotlin version.
    alias(libs.plugins.mokkery)
}

group = "dev.openfeature.kotlin.contrib"

kotlin {
    jvmToolchain(17)

    applyDefaultHierarchyTemplate()

    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    swiftPMDependencies {
        // LaunchDarkly iOS SDK 10+ requires iOS 13+. See https://github.com/launchdarkly/ios-client-sdk/blob/v11/CHANGELOG.md
        iosMinimumDeploymentTarget.set("13.0")
        swiftPackage(
            url = "https://github.com/launchdarkly/ios-client-sdk.git",
            version = "11.1.2",
            products = listOf("LaunchDarkly"),
        )
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.openfeature.kotlin.sdk)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.launchdarkly.android.client.sdk)
        }
        androidUnitTest.dependencies {
            implementation(kotlin("test"))
            implementation(kotlin("test-junit"))
        }
        val iosTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "dev.openfeature.kotlin.contrib.providers.launchdarkly"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }

    publishing {
        singleVariant("release") {
            withJavadocJar()
            withSourcesJar()
        }
    }
}

mavenPublishing {
    pom {
        name.set("OpenFeature LaunchDarkly Kotlin Provider")
        description.set(
            "LaunchDarkly-backed OpenFeature FeatureProvider for Android and iOS. " +
                "Uses OpenFeature kotlin-sdk snapshot (iOS klibs) from Maven Central snapshots.",
        )
    }
}
