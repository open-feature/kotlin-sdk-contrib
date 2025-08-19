import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.dokka")
}

// Configure Dokka for documentation
dokka {
    dokkaPublications.html {
        suppressInheritedMembers.set(true)
        failOnWarning.set(true)
    }
    dokkaSourceSets.commonMain {
        sourceLink {
            localDirectory.set(file("src/"))
            remoteUrl("https://github.com/open-feature/kotlin-sdk/tree/main/kotlin-sdk/src")
            remoteLineSuffix.set("#L")
        }
    }
}

mavenPublishing {
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            sourcesJar = true,
            androidVariantsToPublish = listOf("release")
        )
    )
    signAllPublications()
    coordinates(
        groupId = "dev.openfeature.kotlin.contrib.providers",
        version = findProperty("version").toString()
    )
    pom {
        name.set("OpenFeature Kotlin SDK")
        description.set(
            "This is the Kotlin implementation of OpenFeature, a vendor-agnostic abstraction library for evaluating feature flags."
        )
        url.set("https://openfeature.dev")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("vahidlazio")
                name.set("Vahid Torkaman")
                email.set("vahidt@spotify.com")
            }
            developer {
                id.set("fabriziodemaria")
                name.set("Fabrizio Demaria")
                email.set("fdema@spotify.com")
            }
            developer {
                id.set("nicklasl")
                name.set("Nicklas Lundin")
                email.set("nicklasl@spotify.com")
            }
            developer {
                id.set("nickybondarenko")
                name.set("Nicky Bondarenko")
                email.set("nickyb@spotify.com")
            }
            developer {
                id.set("bencehornak")
                name.set("Bence Hornák")
                email.set("bence.hornak@gmail.com")
            }
        }
        scm {
            connection.set(
                "scm:git:https://github.com/open-feature/kotlin-sdk-contrib.git"
            )
            developerConnection.set(
                "scm:git:ssh://open-feature/kotlin-sdk-contrib.git"
            )
            url.set("https://github.com/open-feature/kotlin-sdk-contrib")
        }
    }
}

