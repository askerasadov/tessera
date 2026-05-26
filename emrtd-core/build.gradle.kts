plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
}

mavenPublishing {
    coordinates(group.toString(), "tessera-emrtd-core", version.toString())

    pom {
        name.set("tessera-emrtd-core")
        description.set(
            "Placeholder module reserving the tessera-emrtd-core artifactId for the eMRTD " +
                "(electronic Machine Readable Travel Document) chip-reading subsystem planned for the 0.6.0 " +
                "release. Currently empty; do not depend on this module yet.",
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
