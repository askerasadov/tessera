plugins {
    alias(libs.plugins.kotlin.multiplatform)
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
