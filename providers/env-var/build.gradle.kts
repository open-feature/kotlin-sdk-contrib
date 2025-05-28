import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_21)
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
        }
    }
}
