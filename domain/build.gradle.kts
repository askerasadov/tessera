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
