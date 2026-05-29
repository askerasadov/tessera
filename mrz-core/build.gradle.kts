plugins {
    alias(libs.plugins.kotlin.multiplatform)
    // dokka must be applied before maven.publish: signAllPublications() (root build.gradle.kts)
    // forces eager realization of the Dokka javadoc jar during maven.publish's apply, which looks
    // up the `dokkaGeneratePublicationHtml` task — so that task must already exist.
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

mavenPublishing {
    coordinates(group.toString(), "tessera-mrz-core", version.toString())

    pom {
        name.set("tessera-mrz-core")
        description.set(
            "MRZ (Machine Readable Zone) parsing, validation, and generation for all ICAO Doc 9303 " +
                "document formats — TD1, TD2, TD3, MRV-A, and MRV-B. Reader-not-oracle: extracts data " +
                "verbatim and reports observations; consumers make trust decisions.",
        )
    }
}

kotlin {
    explicitApi()

    jvmToolchain(21)

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":types"))
                api(libs.kotlinx.datetime)
                implementation(project(":logging"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.property)
            }
        }
    }
}
