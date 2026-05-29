import com.vanniktech.maven.publish.JavaPlatform
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.dokka) apply false
}

allprojects {
    group = "io.lightine.tessera"
}

// Shared Maven Central POM metadata (license, url, developer, scm) applied to every
// subproject that has the vanniktech maven-publish plugin applied. Per-module identity
// (artifactId, pom name, pom description) is set in each module's build.gradle.kts.
//
// Two publication shapes are supported:
//   - KMP modules (types, mrz-core, emrtd-core, telemetry, logging) get
//     `configure(KotlinMultiplatform(...))` with Dokka-backed javadoc jars and sources jars.
//     JavadocJar.Dokka points at Dokka 2's `dokkaGeneratePublicationHtml` task — Maven Central
//     requires a `*-javadoc.jar` for non-snapshot releases.
//   - The BOM module (java-platform) gets `configure(JavaPlatform())` — no jar, no sources,
//     no javadoc, just a POM with `<dependencyManagement>` per ADR-016's BOM decision.
//
// Coordinates and publication scope locked under ADR-016.
subprojects {
    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<MavenPublishBaseExtension> {
            // Sign every published artifact (jar, sources, javadoc, pom, module, and the BOM
            // pom) with the maintainer's PGP key. Maven Central rejects non-snapshot releases
            // that lack a `.asc` signature for each artifact. vanniktech wires up the Gradle
            // signing plugin automatically when this is called; credentials are read from
            // `signingInMemoryKey` / `signingInMemoryKeyId` / `signingInMemoryKeyPassword`
            // (set per-machine in `~/.gradle/gradle.properties`, never committed — see
            // `docs/publishing-setup.md`).
            signAllPublications()

            pom {
                url.set("https://github.com/askerasadov/tessera")
                inceptionYear.set("2026")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("askerasadov")
                        name.set("Asker Asadov")
                        email.set("asker.asadov@gmail.com")
                    }
                }

                scm {
                    url.set("https://github.com/askerasadov/tessera")
                    connection.set("scm:git:git://github.com/askerasadov/tessera.git")
                    developerConnection.set("scm:git:ssh://git@github.com/askerasadov/tessera.git")
                }

                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/askerasadov/tessera/issues")
                }
            }
        }

        plugins.withId("org.jetbrains.kotlin.multiplatform") {
            extensions.configure<MavenPublishBaseExtension> {
                configure(
                    KotlinMultiplatform(
                        javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
                        sourcesJar = true,
                    ),
                )
            }
        }

        plugins.withId("java-platform") {
            extensions.configure<MavenPublishBaseExtension> {
                configure(JavaPlatform())
            }
        }
    }
}

spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "**/.gradle/**")
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**", "**/.gradle/**")
        ktlint(ktlintVersion)
    }
}
