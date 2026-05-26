plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
}

mavenPublishing {
    coordinates(group.toString(), "tessera-telemetry", version.toString())

    pom {
        name.set("tessera-telemetry")
        description.set(
            "Pluggable telemetry interface for Tessera SDK operations. Consumers wire in their own " +
                "telemetry sink; the SDK does no I/O of its own. Target-agnostic and dependency-free.",
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
