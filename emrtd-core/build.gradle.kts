plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(21)

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":domain"))
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
