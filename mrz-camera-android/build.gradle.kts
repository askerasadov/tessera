plugins {
    alias(libs.plugins.kotlin.multiplatform)
    // Android target via Google's KMP-library plugin (ADR-017). Applied right after the Kotlin
    // Multiplatform plugin so the `android {}` target is available inside the `kotlin {}` block.
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

// Intentionally NOT published in this slice. This module's Maven coordinates, BOM entry, and
// publishing wiring are a release-slice concern (locked at the 0.2.0 tag), so the vanniktech
// maven-publish and dokka plugins are deliberately not applied yet. The module builds and is
// host-tested without them; nothing here is published until 0.2.0 is cut.

kotlin {
    explicitApi()

    jvmToolchain(21)

    // The JVM target carries the host tests for the platform-agnostic analyse-frame core, so they run
    // on the existing JVM `check` CI runner with no Android SDK. It is a build/test target here, not a
    // declared publication shape — what this module publishes is decided at the 0.2.0 release slice.
    jvm()

    // Android target. compileSdk tracks the latest stable API (37); minSdk 23 per ADR-018. The
    // androidMain ML Kit recognizer compiles here and is verified by the separate Android-compile CI
    // job (a Linux `check` runner has no Android SDK, so it cannot compile this target).
    android {
        namespace = "io.lightine.tessera.mrz.camera"
        compileSdk = 37
        minSdk = 23
    }

    sourceSets {
        commonMain {
            dependencies {
                // ParseResult / MrzParser (mrz-core), the MrzFormat & error vocabulary (types), and
                // TelemetrySink / TelemetryEvent (telemetry) all surface in this module's public API,
                // so they are `api` dependencies — consumers see them transitively.
                api(project(":types"))
                api(project(":mrz-core"))
                api(project(":telemetry"))
            }
        }
        androidMain {
            dependencies {
                // CameraX camera-core supplies the ImageProxy frame type the Android recognizer reads;
                // ML Kit text-recognition is the bundled-model variant (no Play Services, no network);
                // coroutines-core bridges ML Kit's Task<Text> to the suspend OCR seam.
                implementation(libs.androidx.camera.core)
                implementation(libs.mlkit.text.recognition)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                // runTest — exercises the suspend analyse() function on the host with a mock recognizer.
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
