plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
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
