pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        // The Android Gradle Plugin — including the com.android.kotlin.multiplatform.library plugin
        // that adds the Android target to the core modules (ADR-017) — is published only to Google's
        // Maven repository, not to mavenCentral or the Gradle Plugin Portal.
        google()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        // Android target dependencies (AGP's own runtime artifacts, and androidx libraries pulled in
        // by later camera slices) resolve from Google's Maven repository.
        google()
    }
}

rootProject.name = "tessera"

include(":types")
include(":mrz-core")
include(":emrtd-core")
include(":telemetry")
include(":logging")
include(":bom")
