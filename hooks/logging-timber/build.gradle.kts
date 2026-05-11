import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("dev.openfeature.hook-conventions")
    id("com.android.library")
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")

        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }

    sourceSets {
        androidMain.dependencies {
            api(libs.openfeature.kotlin.sdk)
            compileOnly("com.jakewharton.timber:timber:5.0.1")
        }
        androidUnitTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("com.jakewharton.timber:timber:5.0.1")
        }
    }
}

android {
    namespace = "dev.openfeature.kotlin.contrib.hooks.logging.timber"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    pom {
        name.set("OpenFeature Timber Logger Adapter")
        description.set(
            "Timber adapter for the OpenFeature Kotlin SDK. Allows developers to use their existing Timber setup with OpenFeature on Android.",
        )
    }
}
