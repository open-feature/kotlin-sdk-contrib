import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

plugins {
    id("com.android.library")
    id("dev.openfeature.provider-conventions")
    alias(libs.plugins.kotlinx.serialization)
    // Needed for the JS coroutine support for the tests
    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.docker.compose)
}

kotlin {
    androidTarget()
    jvm {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }
    linuxX64 {}
    js {
        nodejs {}
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.openfeature.kotlin.sdk)

                api(libs.kotlinx.coroutines.core)
                api(libs.ktor.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        val nonJsMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.cio)
            }
        }
        androidMain.get().dependsOn(nonJsMain)
        jvmMain.get().dependsOn(nonJsMain)
        linuxX64Main.get().dependsOn(nonJsMain)

        jsMain.dependencies {
            implementation(libs.ktor.js)
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

// Launch test container for IntegrationTest.kt
dockerCompose {
    useComposeFiles = listOf("src/integrationTest/docker-compose.yaml")
}
// Note: some Kotlin test targets extend the Test (e.g. JVM), some others the KotlinTest class (e.g. Native, JS)
tasks.withType(Test::class) {
    dependsOn(tasks.named("composeUp"))
    finalizedBy(tasks.named("composeDown"))
}
tasks.withType(KotlinTest::class) {
    dependsOn(tasks.named("composeUp"))
    finalizedBy(tasks.named("composeDown"))
}
