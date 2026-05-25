plugins {
    alias(libs.plugins.kotlin.multiplatform)
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
