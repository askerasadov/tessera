plugins {
    alias(libs.plugins.kotlin.multiplatform)
    // dokka must be applied before maven.publish: signAllPublications() (root build.gradle.kts)
    // forces eager realization of the Dokka javadoc jar during maven.publish's apply, which looks
    // up the `dokkaGeneratePublicationHtml` task — so that task must already exist.
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

mavenPublishing {
    coordinates(group.toString(), "tessera-logging", version.toString())

    pom {
        name.set("tessera-logging")
        description.set(
            "Placeholder module reserving the tessera-logging artifactId for the Tessera logging " +
                "subsystem. Currently empty; do not depend on this module yet.",
        )
    }
}

kotlin {
    jvmToolchain(21)

    jvm()

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.property)
            }
        }
    }
}
