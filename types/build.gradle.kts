plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

mavenPublishing {
    coordinates(group.toString(), "tessera-types", version.toString())

    pom {
        name.set("tessera-types")
        description.set(
            "Core type definitions and error taxonomy shared across the Tessera identity-document SDK. " +
                "Contains the cross-cutting vocabulary (document type codes, sex characters, unmapped-character " +
                "representations) and the MRZ error hierarchy. Typically pulled in transitively via tessera-mrz-core.",
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
                api(libs.kotlinx.datetime)
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
