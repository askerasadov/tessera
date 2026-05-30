plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

// The iOS platform-I/O module: AVFoundation + Apple Vision on top of the platform-agnostic
// mrz-camera-core contract (ADR-021). iOS targets only — no jvm/android target, and (unlike the
// other camera modules) no Android Gradle plugin, since there is no Android target. Intentionally
// NOT published in this slice; iOS distribution is an XCFramework wrapped in a Swift package
// (ADR-019), built at the 0.2.0-release-slice and exporting mrz-camera-core into the framework.

kotlin {
    explicitApi()

    jvmToolchain(21)

    // iOS targets (ADR-017): device arm64 plus the Apple-Silicon and Intel simulators. These three
    // standard shortcuts activate Kotlin's default hierarchy template, providing the shared
    // iosMain/iosTest source sets where the AVFoundation/Vision code and its simulator tests live.
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain {
            dependencies {
                // The platform-agnostic contract: VisionMrzTextRecognizer implements
                // MrzTextRecognizer<CMSampleBufferRef> from mrz-camera-core, and its results expose
                // core types — so the core is `api`, seen transitively (it re-exports types / mrz-core
                // / telemetry / coroutines in turn).
                api(project(":mrz-camera-core"))
            }
        }
        // The tests are iOS-only (they drive Apple Vision on the Simulator), so the test framework
        // dependency goes on iosTest rather than commonTest — this module has no common test sources,
        // and configuring commonTest while leaving it empty trips an "unused source set" warning.
        iosTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
