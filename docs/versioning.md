# Versioning

This document defines how the project handles versions, releases, and changes over time. It applies from the first internal release onward, including the 0.x development phase. The intent is that consumers can rely on the project's version numbers to convey accurate information about what changed and what to expect.

This document is living. The policy may evolve, but changes are themselves recorded transparently — a versioning policy that changes silently undermines its own purpose.

---

## Versioning Scheme

The project follows [Semantic Versioning 2.0.0](https://semver.org/). Every release has a version number of the form `MAJOR.MINOR.PATCH`, where each component conveys specific information:

- **MAJOR** — incremented for changes that break backward compatibility. Existing consumer code may need to be modified to work with the new version.
- **MINOR** — incremented for changes that add new functionality without breaking existing functionality. Existing consumer code continues to work without modification.
- **PATCH** — incremented for backward-compatible bug fixes, performance improvements, or internal changes. Existing consumer code is unaffected.

The version is a contract. A consumer reading "this is a MINOR update" can trust that their code will still work. A consumer reading "this is a MAJOR update" can plan for the work involved in adapting.

---

## Backward Compatibility in 0.x

Pre-1.0 versions in this project follow the same backward-compatibility rules as post-1.0 versions. Within a single MAJOR version (including the 0.x line), public APIs do not break.

This is stricter than the convention in many open-source projects, where 0.x signals "API may change without notice." The choice is deliberate:

- It forces careful API design from the first release, rather than deferring difficult choices to a hypothetical 1.0
- It builds consumer trust early, including for the internal-only phase
- It makes the 0.x → 1.0 transition meaningful as a stability and maturity milestone, not as the moment compatibility starts mattering
- It is consistent with Principle 9 (Forward-compatible API)

Breaking changes during the 0.x phase, when they occur, are MAJOR version bumps. A breaking change in version 0.3.0 produces version 0.4.0 only if compatible additions are also being made; otherwise, a true breaking change in the 0.x line produces a new MAJOR version (1.0.0 if the API is otherwise stable, or a re-baseline of the 0.x line if not).

In practice, the expected pattern is: 0.x development proceeds through MINOR additions only. Breaking changes are deferred and bundled into the 1.0.0 release, which marks the public API stabilization commitment. After 1.0.0, breaking changes follow the deprecation cycle described below.

---

## What Constitutes a Breaking Change

A breaking change is one that requires existing consumer code to be modified in order to compile or function correctly with the new version. Specifically:

- Removing a public type, function, property, or constant
- Renaming a public type, function, property, or constant
- Changing the signature of a public function (parameters, return type, or throws clause) in a way that existing call sites cannot satisfy
- Changing the observable behavior of an existing public function such that consumer code relying on the old behavior fails or produces wrong results
- Removing an enum case or sealed type variant that consumers may have referenced
- Changing required platform versions (raising minimum Android API level or iOS version) in a way that drops support for previously supported platforms
- Removing a target platform that the SDK previously supported
- Changing the meaning of a data class field or making a previously optional field required

Some changes are not breaking, even though they may seem to be:

- Adding a new public type, function, property, or constant
- Adding a new optional parameter with a safe default
- Adding a new enum case or sealed type variant (consumers using exhaustive matching will see a warning, not an error, when this is done correctly)
- Lowering required platform versions (broadening support)
- Adding new target platforms
- Changing internal implementation details that are not observable through public APIs
- Performance improvements that do not change observable behavior

The boundary between breaking and non-breaking is occasionally subtle. When uncertain, the project errs on the side of treating a change as breaking — a false positive (calling something breaking when it isn't) is harmless; a false negative (treating a breaking change as non-breaking) damages consumer trust.

**Platform minimums — the managed-raise policy.** Raising a minimum platform version is breaking (above), but external forces — a vendor end-of-life, an unpatched security issue at the old floor, or a toolchain floor (such as Kotlin/Native's minimum iOS deployment target) rising past ours — can compel one. Such a raise is handled as a **documented breaking release**: release-noted, with prior artifacts left published so existing consumers are not retroactively broken. It is done *only when forced*, not as a routine per-release bump; lowering a minimum stays non-breaking. The committed minimums and the full rationale are in [ADR-018](decisions/0018-platform-minimums-and-managed-raise.md).

---

## Deprecation Policy

When a public API needs to be removed or replaced, it is deprecated before being removed. The cycle is:

1. **Deprecation** — In version N, the API is marked deprecated using the platform's standard mechanism (`@Deprecated` in Kotlin, `@available(*, deprecated:)` in Swift, etc.). The deprecation notice indicates the version of deprecation and, where possible, the replacement.
2. **Persistence** — The deprecated API continues to function for at least two MINOR versions. It must remain in version N, version N+1, and version N+2.
3. **Removal** — The deprecated API may be removed no earlier than version N+2, and only as part of a MAJOR version bump.

This means that a deprecation in version 1.3.0 cannot be followed by removal until at least version 2.0.0, and even then only if at least one MINOR version has elapsed since deprecation. Consumers always have multiple releases to migrate.

Deprecations include actionable guidance:

- What replaces the deprecated API
- How the migration is performed
- Why the deprecation occurred (if non-obvious)

A deprecation without a replacement is rare and treated with extra care; it implies the SDK is dropping a capability rather than evolving it.

---

## Pre-Release Versions

Pre-release versions use suffixes appended to the SemVer base version, separated by a hyphen:

- `1.0.0-alpha.1`, `1.0.0-alpha.2` — early development of an upcoming version, unstable, may change in any way
- `1.0.0-beta.1`, `1.0.0-beta.2` — feature-complete previews, API surface considered close to final, breaking changes possible but discouraged
- `1.0.0-rc.1`, `1.0.0-rc.2` — release candidates, no further changes expected unless blocking issues are found

Pre-release versions sort according to SemVer rules: `1.0.0-alpha.1 < 1.0.0-beta.1 < 1.0.0-rc.1 < 1.0.0`. Consumers using pre-release versions accept that they are not stable; the backward-compatibility rules above apply only to non-pre-release versions.

The project may use pre-release versions for major milestones (notably the 1.0.0 release) but is not required to. Releases without a pre-release phase are also acceptable for incremental updates.

---

## Per-Release Artifacts

Every release produces:

- A tagged commit in the version control system, named exactly with the version (e.g., `v0.1.0`, `v1.0.0`, `v1.0.0-beta.1`)
- A `CHANGELOG.md` entry describing what changed in the release, following the [Keep a Changelog](https://keepachangelog.com/) format
- Release notes published alongside the artifact, summarizing notable changes for consumers

The `CHANGELOG.md` entry distinguishes between:

- **Added** — new features and capabilities
- **Changed** — changes to existing functionality (non-breaking)
- **Deprecated** — APIs marked for future removal
- **Removed** — APIs that have been removed in this version (only on MAJOR bumps)
- **Fixed** — bug fixes
- **Security** — vulnerabilities addressed

Each entry is written for consumers, not maintainers. The reader is someone integrating the SDK who needs to understand the impact of upgrading.

---

## Inline "Available Since" Markers

Public APIs in documentation indicate the version in which they first appeared. The format is brief and consistent:

> *Available since 0.2.0.*

When an API has been deprecated, the deprecation notice indicates the version of deprecation and the replacement:

> *Deprecated in 1.3.0. Use `newApi()` instead. Will be removed in a future major version.*

These markers help consumers reading the documentation understand which features are available in the version they are using, without consulting the changelog separately.

---

## Backward Compatibility Commitments

Within a single MAJOR version, the project makes the following commitments to consumers:

- Public APIs continue to work as documented
- Behavior of public APIs does not change in ways that break correct consumer code
- Bug fixes that change observable behavior are evaluated case-by-case; if the previous behavior was clearly wrong, the fix is treated as non-breaking, but the change is documented prominently
- Internal implementations may change freely without notice — consumers relying on undocumented behavior do so at their own risk

These commitments apply from the first release of a MAJOR version through its final release, including all MINOR and PATCH updates within that line.

When a new MAJOR version ships, the previous MAJOR version may continue to receive critical bug fixes for a period determined per-release. The duration is announced in the release notes for each MAJOR version.

---

## How This Document Relates to Principles

Versioning policy implements Principle 9 (Forward-compatible API, opinionated implementation), defined in `principles.md`. The principle says public APIs evolve without breaking consumers; this document specifies how that commitment is operationalized.

It also relates to Principle 4 (Honest about what we know): version numbers convey real information about what changed. Inflating a version (calling a small change a MAJOR bump) or deflating one (slipping a breaking change into a MINOR release) damages the contract.

When this document conflicts with a principle, the principle wins and this document is revised.
