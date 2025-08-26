import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
    id("dev.openfeature.provider-conventions")
    // Needed for the JS coroutine support for the tests
    alias(libs.plugins.kotlinx.atomicfu)
}

kotlin {
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
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.openfeature.kotlin.sdk)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

mavenPublishing {
    pom {
        name.set("OpenFeature Environment Variables Kotlin Provider")
        description.set(
            "The Environment Variables provider allows you to read feature flags from the process's environment.",
        )
    }
}

// Set test environment variable for tests
// Used in ./commonTest/kotlin/dev/openfeature/kotlin/contrib/providers/envvar/PlatformSpecificEnvironmentGatewayTest.kt
val testEnvironmentVariable = "TEST_ENVIRONMENT_VARIABLE" to "foo"
tasks.withType(Test::class).configureEach {
    environment(testEnvironmentVariable)
}
tasks.withType(KotlinJsTest::class).configureEach {
    environment(testEnvironmentVariable.first, testEnvironmentVariable.second)
}
tasks.withType(KotlinNativeTest::class).configureEach {
    environment(testEnvironmentVariable.first, testEnvironmentVariable.second)
}
