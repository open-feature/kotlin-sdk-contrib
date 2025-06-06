plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.android.library") version "8.10.1"
}

kotlin {
    androidTarget()
}

kotlin {
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            api(libs.openfeature.kotlin.sdk)

            api(libs.kotlinx.coroutines.core)
            api(libs.okhttp)
            // TODO: replace with multiplatform JSON library
            api("com.google.code.gson:gson:2.12.1")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.okhttp.mockwebserver)
        }
    }
}

android {
    namespace = "dev.openfeature.kotlin.contrib.providers.ofrep"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        singleVariant("release") {
            withJavadocJar()
            withSourcesJar()
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
