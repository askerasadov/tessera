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
// Coordinates and publication scope locked under ADR-016. JavadocJar.Dokka points at
// Dokka 2's `dokkaGeneratePublicationHtml` task — Maven Central requires a `*-javadoc.jar`
// for non-snapshot releases, and the modern Dokka HTML output is what we ship inside it
// (browsable Kotlin-aware docs; consumers can still fetch the jar as a "javadoc" attachment
// the way IDEs and Maven Central UI expect). sourcesJar is enabled so consumers get source
// attachments automatically.
subprojects {
    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<MavenPublishBaseExtension> {
            configure(
                KotlinMultiplatform(
                    javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
                    sourcesJar = true,
                ),
            )

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
