plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

mavenPublishing {
    coordinates(group.toString(), "tessera-emrtd-core", version.toString())

    pom {
        name.set("tessera-emrtd-core")
        description.set(
            "Placeholder module reserving the tessera-emrtd-core artifactId. Will contain pure logic " +
                "for electronic document data (data-group parsing, Security Object structural parsing, " +
                "BAC/PACE key derivation); NFC I/O lives in separate platform-specific modules " +
                "(tessera-emrtd-nfc-android, tessera-emrtd-nfc-ios) per ADR-016. Planned for the 0.6.0 " +
                "release; currently empty, do not depend on this module yet.",
        )
    }
}

kotlin {
    jvmToolchain(21)

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":types"))
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
