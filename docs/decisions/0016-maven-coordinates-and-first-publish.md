# ADR-016: Maven Central coordinates, lockstep versioning, and first publication at 0.1.1

**Status:** Accepted

---

## Context

With the `io.lightine` Sonatype namespace claimed and verified (banked in PR [#75](https://github.com/lightine-io/tessera/pull/75) as a partial resolution of the "Distribution channels" entry in [`open-questions.md`](../open-questions.md)), Maven Central is the confirmed distribution channel for the JVM target. Several follow-on decisions interlock and need a single coherent resolution:

1. **Coordinate shape** — what is the published groupId, and what convention do artifactIds follow?
2. **Versioning strategy** — do modules version independently or in lockstep?
3. **BOM** — does the project publish a Bill of Materials artifact alongside the modules?
4. **First-publish version** — does the first Maven Central release publish under the existing `v0.1.0` tag, or under a new `v0.1.1` tag?
5. **First-publish scope** — which of the current modules ship at first publish, and do snapshot builds ship alongside?

These decisions interlock — coordinate shape affects naming, lockstep vs independent affects BOM design, timing affects which modules exist at first publish. They also lock together under [ADR-007](0007-strict-backward-compat-from-0x.md) (strict backward compatibility from 0.x): once published under specific coordinates, those coordinates cannot change without breaking consumers. Capturing them in a single ADR keeps the rationale coherent and makes the lock-in explicit.

The roadmap in [`scope.md`](../scope.md) implies 10-15 published modules by 1.0.0 (camera at 0.2.0, image reading at 0.3.0, UI at 0.5.0, NFC stack at 0.6.0, plus the Beyond-1.0 capabilities). The convention chosen here has to scale through all of that without breaking.

---

## Decision

**1. groupId: `io.lightine.tessera`.**

Matches the project's already-committed package root from the "Project root namespace" entry in [`open-questions.md`](../open-questions.md). The Sonatype namespace verification at `io.lightine` permits any deeper groupId under that prefix; `io.lightine.tessera` preserves the package-root / groupId mirroring and is the natural shape.

**2. artifactId convention: `tessera-<module>`.**

Every published module gets the `tessera-` prefix. Examples at 0.1.1:

- `io.lightine.tessera:tessera-mrz-core:0.1.1`
- `io.lightine.tessera:tessera-emrtd-core:0.1.1`
- `io.lightine.tessera:tessera-types:0.1.1`
- `io.lightine.tessera:tessera-telemetry:0.1.1`
- `io.lightine.tessera:tessera-logging:0.1.1`

Future modules follow the same shape — `tessera-mrz-camera-android` at 0.2.0, `tessera-ui-android` at 0.5.0, `tessera-emrtd-bac` / `tessera-emrtd-pace` / `tessera-emrtd-nfc-android` at 0.6.0, and so on.

**3. Lockstep versioning.**

All modules share a single version, bumped together at each release. When 0.2.0 ships (adding camera reading), every published module bumps to 0.2.0 — including modules that had no internal changes between releases. The version number identifies the project release, not the per-module change history.

**4. BOM: `tessera-bom` ships alongside the modules.**

A separate `tessera-bom` artifact declares the version of every Tessera module. Consumers opting into the BOM can omit per-module versions in their `dependencies { }` block:

```kotlin
dependencies {
    implementation(platform("io.lightine.tessera:tessera-bom:0.2.0"))
    implementation("io.lightine.tessera:tessera-mrz-core")
    implementation("io.lightine.tessera:tessera-mrz-camera-android")
    // no version needed on the individual modules — the BOM aligns them
}
```

The BOM scales with the module count and prevents version mismatches as the project grows.

**5. First publication at 0.1.1.**

The currently-tagged `v0.1.0` commit does not include publishing infrastructure (the `maven-publish` Gradle plugin, PGP signing configuration, POM metadata, Dokka for Javadoc generation, the BOM module). Rather than retroactively backporting that infrastructure to the `v0.1.0` tag, the publishing setup lands as the normal work for `v0.1.1`, which becomes the first Maven Central artifact. `v0.1.0` remains the internal-milestone tag; `v0.1.1` is the first publicly-distributed version.

**6. First-publish scope: all current modules + BOM, no snapshots.**

At 0.1.1 the published set is:

- `tessera-mrz-core`
- `tessera-types` (was `tessera-domain` at ADR-016 acceptance time — renamed in the executed follow-up cleanup; see Follow-up Cleanup section below)
- `tessera-emrtd-core`
- `tessera-telemetry`
- `tessera-logging`
- `tessera-bom`

No module is held back. `tessera-types` is published even though it is a transitive-only dependency for almost all consumers (depended on by `tessera-mrz-core` and `tessera-emrtd-core` via Gradle `api`); it has to be published or those modules will not resolve.

Snapshot builds (`<version>-SNAPSHOT` to the Sonatype snapshots repository) are not published at 0.x. Snapshots can be added later if a real need emerges, with the cost of one additional `repository {}` block in the publishing configuration. At the project's current scale (single maintainer, no external integrators tracking work-in-progress), they add infrastructure without proportional value.

---

## Consequences

**Positive:**

- Self-identifying artifact names. `tessera-mrz-core` is unambiguous in any context — Maven Central search results, IDE dependency views, security scan reports, license audit tools, Dependabot PR titles. The groupId carries the same information but is not always shown alongside.
- Matches industry convention for multi-module Kotlin/Java libraries. Firebase (`com.google.firebase:firebase-auth`), Spring (`org.springframework:spring-core`), Ktor (`io.ktor:ktor-client-core`), Coroutines (`org.jetbrains.kotlinx:kotlinx-coroutines-core`), SLF4J (`org.slf4j:slf4j-api`), Jackson (`com.fasterxml.jackson.core:jackson-core`) all use the project-prefix-in-artifactId pattern. Consumers expect it.
- The package root (`io.lightine.tessera.*`) and the groupId (`io.lightine.tessera`) mirror each other cleanly. A class's package path translates directly to its module's coordinates.
- Lockstep versioning gives users a single mental model: "Tessera 0.2.0" means a coordinated set of artifacts, not a compatibility matrix. The BOM makes this trivial to express.
- The BOM scales naturally as the module count grows. By 1.0.0 a Tessera consumer might import 6-8 modules; the BOM keeps the import block concise and version-aligned.
- Pattern works cleanly for every planned future module per [`scope.md`](../scope.md). Verified during pre-decision analysis: camera modules, image-reading modules, UI modules, NFC modules, and post-1.0 capabilities all fit `tessera-<module>` without naming awkwardness.
- The Maven-side namespace mirrors the already-resolved "Project root namespace" decision, so there is no asymmetry between code references and dependency declarations.
- Starting at 0.1.1 keeps the `v0.1.0` tag's content semantically clean — the publishing infrastructure lands as deliberate normal work, not as backported retrofitting.

**Negative:**

- "tessera" appears twice in each coordinate: once in the groupId, once in the artifactId prefix. Mild visual redundancy. Firebase has the same redundancy (`com.google.firebase:firebase-auth`); it is the accepted cost of the pattern.
- Lockstep versioning means republishing modules that had no internal changes — `tessera-telemetry` may ship unchanged bytes at 0.2.0 with only a version bump. The storage cost on Maven Central is trivial; the conceptual cost (a "release" of an unchanged module) is small but real.
- All decisions in this ADR lock under [ADR-007](0007-strict-backward-compat-from-0x.md). The groupId, the artifactId names, the BOM coordinate, and the lockstep posture cannot change at any point in the 0.x or 1.x line without breaking every consumer.
- Consumers who want very fine-grained version control across modules cannot get it — pinning individual modules to different versions defeats the BOM. (No real consumer is expected to want this, but it is foreclosed by lockstep.)
- The shared-types module was originally named `domain` at this ADR's acceptance time. As anticipated in the Follow-up Cleanup section below, that name was renamed to `types` before first publish (executed as a separate PR) to ship under a more semantic name. The published artifactId is therefore `tessera-types`, not `tessera-domain`.

**Neutral:**

- KMP variants (`tessera-mrz-core-jvm`, future `tessera-mrz-core-android`, etc.) are auto-published by the Kotlin Gradle plugin. Users never reference the platform-suffixed variants directly — Gradle resolves the right one for their target. No additional decisions needed.
- The 0.1.0 vs 0.1.1 choice for first-publish version is cosmetic. Both publish the same API; 0.1.1 has cleaner tag-content semantics.
- Snapshot publishing remains an open option for the future. Skipping it at 0.x is reversible at any time with negligible cost.

---

## Alternatives Considered

**Pattern A: `io.lightine.tessera:<module>` (no `tessera-` prefix in artifactIds).**

Considered. This is the AWS SDK style (`software.amazon.awssdk:s3`, `software.amazon.awssdk:dynamodb`). Cleaner aesthetically — no doubled "tessera" — and the groupId already carries project context in any tool that shows full coordinates. Rejected because:

- The Kotlin/Java multi-module convention is overwhelmingly to use the prefix. Going against it imposes a small but persistent friction on consumers.
- Even semantic-but-short module names (`types`, also generic words like `core`) are more legible with the `tessera-` prefix. The shared-types module was originally named `domain` at ADR acceptance time — see Follow-up Cleanup — and the same reasoning applied to that name.
- Some dependency tools (notably Dependabot PR titles, certain security scanners, jar filename references in error messages) display artifactId without groupId. The prefix preserves identification in those contexts.
- The aesthetic redundancy is a small ongoing cost; the conventional pattern's familiarity is a small ongoing benefit. The benefit slightly wins.

**Pattern B: `io.lightine:tessera-<module>` (project name only in artifactId prefix, not in groupId).**

Considered. The groupId would be the brand-only `io.lightine`; the project name appears only in the artifactId. Rejected because:

- Breaks the symmetry between the published groupId and the already-committed package root (`io.lightine.tessera.*`). Code references and dependency declarations would live in different parts of the `io.lightine` namespace.
- Provides no real benefit over Pattern A* (the chosen pattern) — the `tessera-` prefix in artifactIds gives the same self-identification.

**Independent versioning per module.**

Considered. Each module would version independently; the BOM would become a "release train" pinning compatible combinations (similar to Spring Boot's `spring-boot-dependencies` model). Rejected because:

- Tessera's modules ship together as the SDK evolves. The roadmap is structured around coordinated capability additions (camera at 0.2.0, UI at 0.5.0, NFC at 0.6.0), not independent module lifecycles.
- Lockstep matches the project's release-cadence narrative — "Tessera 0.2.0" is meaningful in a way that "tessera-mrz-core 1.5.0 paired with tessera-emrtd-core 0.8.2" is not.
- Independent versioning adds significant complexity (BOM management, compatibility matrices, per-module changelogs) without proportional benefit. The complexity matters in projects where modules genuinely have independent users and lifecycles; Tessera does not.

**Single umbrella artifact** (e.g., `io.lightine.tessera:tessera:0.1.1` pulling everything in).

Considered. Simplest possible user experience — one import line. Rejected because:

- Throws away the modular architecture from [ADR-003](0003-modular-architecture.md). Users wanting only MRZ parsing would pay the dependency cost of NFC code, UI code, and image processing.
- By 0.6.0 with NFC and all platform-specific modules, the umbrella jar would be unwieldy (estimated 30-50MB with native NFC code and platform-specific UI dependencies bundled in).
- The BOM pattern achieves nearly the same single-import simplicity for users who want it, without forfeiting modular consumption for users who want a smaller dependency footprint.

**Backport publishing config to v0.1.0 commit and publish under 0.1.0.**

Considered. The first Maven Central artifact would carry the same version number as the already-shipped internal tag. Rejected because:

- The `v0.1.0` tag's content does not include publishing infrastructure; retroactively adding it would muddy the tag's semantics.
- 0.1.1 with publishing config landing as a normal PR is cleaner separation between the internal milestone (0.1.0) and the public-distribution milestone (0.1.1).
- No semantic difference for consumers — both publish the same API surface. The choice is purely about which version number's commit history is cleanest.

**Publishing snapshot builds (`<version>-SNAPSHOT`) at 0.x.**

Considered. Sonatype supports snapshot publishing to a separate repository; many libraries publish snapshots so users can opt into work-in-progress builds. Rejected for 0.x because:

- Adds infrastructure complexity (separate repository configuration, lifecycle management, documentation for opting in).
- Snapshots are most useful for projects where users actively track work-in-progress; Tessera's contributor base is currently the maintainer alone.
- Easily added later if a real need emerges (one `repository {}` block in the publishing config).

---

## Related Decisions

- **[ADR-003](0003-modular-architecture.md)** — Modular architecture from day one. This ADR preserves modularity by publishing one artifact per module rather than collapsing into a single umbrella jar.
- **[ADR-007](0007-strict-backward-compat-from-0x.md)** — Strict backward compatibility from 0.x. Once these coordinates are published, they lock under this commitment. The lock applies through the entire 0.x and 1.x line.
- **[ADR-010](0010-apache-2-license.md)** — Apache 2.0 license. Each published artifact's POM declares this license; the license file is included in each jar.
- **[ADR-011](0011-open-source-at-public-release.md)** — Open source at public release. ADR-011 framed 1.0.0 as the public-distribution milestone. ADR-016 starts public distribution at 0.1.1 — earlier than originally implied — but this does not change ADR-011's framing of 1.0.0 as the stability/community commitment. Pre-1.0.0 Maven Central artifacts ship under the same strict backward-compatibility commitment per ADR-007, just without the public-stability marker that 1.0.0 represents.
- **[ADR-015](0015-telemetry-contract-only-at-0-1-0.md)** — Telemetry interface ships as contract-only at 0.1.0. The `tessera-telemetry` artifact at 0.1.1 reflects this — the contract surface ships; emitters arrive in later releases.

---

## Related Documents

- [`architecture.md`](../architecture.md) — module structure that translates to published Maven artifacts.
- [`open-questions.md`](../open-questions.md) — "Distribution channels" entry is partially superseded by this ADR (coordinate-shape sub-question now resolved; iOS distribution channel still deferred). "Project root namespace" entry's `io.lightine.tessera` decision is aligned with this ADR's groupId choice.
- [`scope.md`](../scope.md) — release roadmap whose lockstep-shaped structure motivates the lockstep versioning choice.
- [`versioning.md`](../versioning.md) — versioning policy this ADR's approach aligns with.

---

## Follow-up Cleanup (Out of Scope for This ADR)

- **Rename the `domain` module — EXECUTED.** Renamed from `domain` to `types` in a focused follow-up PR before 0.1.1 ships, per this anticipation. The module folder, the package root (`io.lightine.tessera.domain.*` → `io.lightine.tessera.types.*`), all internal references, the Gradle `include(...)` declaration, the dependent modules' `api(project(...))` declarations, and the relevant living documentation (`architecture.md`, `mrz-error-taxonomy.md`) were updated together; the published artifactId at first publish is `tessera-types`. ADR-012 (which references the `domain` module throughout its reasoning) carries a one-line header note about the rename rather than being rewritten — its reasoning predates the rename and remains historically accurate as written. A separate small follow-up PR will land a path-scoped `.claude/rules/` discipline rule (`tessera-types is types-only; non-type shared code goes in a separate module like tessera-utils`) and cross-reference it from `docs/conventions.md`.

- **Publishing infrastructure work — IMPLEMENTED at 0.1.1.** This ADR locks the design decisions; the implementation (Gradle `maven-publish` configuration, PGP signing, Dokka for Javadoc, POM metadata, BOM scaffolding, Sonatype credential management, the publishing flow) shipped as separate slices — PRs [#80](https://github.com/lightine-io/tessera/pull/80)–[#82](https://github.com/lightine-io/tessera/pull/82) and [#88](https://github.com/lightine-io/tessera/pull/88)–[#90](https://github.com/lightine-io/tessera/pull/90) — via the [vanniktech `gradle-maven-publish-plugin`](https://github.com/vanniktech/gradle-maven-publish-plugin). **Publishing endpoint:** the Sonatype **Central Portal** (`central.sonatype.com`), invoked as `publishToMavenCentral(automaticRelease = false)` (staged deployment, released manually from the portal UI); the legacy OSSRH endpoint (deprecated mid-2024) is not used. See [`publishing-setup.md`](../publishing-setup.md).

- **iOS distribution channel — RESOLVED.** The choice (Swift Package Manager, not CocoaPods) is decided in [ADR-019](0019-ios-distribution-via-spm.md), within a one-channel-per-ecosystem model (Maven Central for JVM/Android/desktop; SPM for iOS; npm for web, future); execution lands with the iOS target in 0.2.0. Xcode is now available, lifting the prior gate.
