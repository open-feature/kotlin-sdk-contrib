plugins {
    alias(libs.plugins.nexus.publish)
}

allprojects {
    extra["groupId"] = "dev.openfeature.kotlin.contrib"
    ext["version"] = "0.1.0"
}
group = project.extra["groupId"].toString()
version = project.extra["version"].toString()

nexusPublishing {
    this.repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"),
            )
            username = System.getenv("OSSRH_USERNAME")
            password = System.getenv("OSSRH_PASSWORD")
        }
    }
}
