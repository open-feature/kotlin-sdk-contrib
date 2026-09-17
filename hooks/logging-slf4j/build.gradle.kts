import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("dev.openfeature.hook-conventions")
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

    sourceSets {
        jvmMain.dependencies {
            api(libs.openfeature.kotlin.sdk)
            compileOnly("org.slf4j:slf4j-api:2.0.9")
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("org.slf4j:slf4j-simple:2.0.9")
        }
    }
}

mavenPublishing {
    pom {
        name.set("OpenFeature SLF4J Logger Adapter")
        description.set(
            "SLF4J adapter for the OpenFeature Kotlin SDK. Allows developers to use their existing SLF4J setup with OpenFeature.",
        )
    }
}
