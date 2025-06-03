import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.ktlint)
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
