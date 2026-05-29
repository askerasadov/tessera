# ADR-020: Camera reading architecture

**Status:** Accepted (may be refined as the feature doc develops)

---

## Context

`0.2.0` adds **headless live-camera MRZ reading** for Android and iOS. Several design choices must be settled before the API ships, because [ADR-007](0007-strict-backward-compat-from-0x.md) locks the public surface from `0.x`: how the consumer drives the camera, how forgiving parsing is of OCR noise, how image/parse *quality* is reported, and how capture failures are surfaced. Each one risks crossing from *reading* into *deciding* — the line [ADR-004](0004-reader-not-oracle.md) (reader, not oracle) draws.

## Decision

**A two-layer headless API, with parsing, quality, and errors all handled in the reader-not-oracle style: expose and surface, never gate or silently overwrite.**

- **Two layers, both headless (no UI):**
  - **Analyse-frame core** — frame in → result out. Platform/source-agnostic at the contract level; unit-testable with injected frames + mock OCR (no device). This is the **deliberate extension point** for any future frame source — a USB document reader, a webcam, the web, or desktop — all of which feed the same seam.
  - **Owns-the-camera-session convenience** — `start()` / `stop()`, running CameraX (Android) / AVCapture (iOS) internally and feeding the core. Built *on top of* the analyse-frame core. The public API hides platform plumbing (`bindToLifecycle`, `ImageAnalysis`, capture sessions) entirely.
- **Parsing modes — consumer-chosen, default strict, raw always exposed.** **Strict** (conform-or-error) and **lenient** (forgives benign format noise such as stray whitespace, without changing any data value). Live camera relies on **strict + next-frame retry** (a noisy frame fails and is dropped; a clean frame arrives within milliseconds), so no correction is needed. **Tolerant mode** (check-digit-guided OCR disambiguation) is **deferred to `0.3.0`** (still-image reading, where there is no "next frame").
- **Quality is exposed as metadata, never gated.** The result surfaces natural signals (MRZ-region found?, OCR confidence, parse outcome); the *consumer* sets any threshold and decides on a re-capture. A richer quality scorer (blur/glare) is **deferred to `0.3.0`**. The SDK never refuses to return data on quality grounds.
- **Capture errors are a separate `Camera…` typed family**, surfaced as a **sealed result** (not thrown, not crashing, never hanging), with stable English codes the consumer localises — distinct from the `mrz-core` parse/validation taxonomy. Permission *requests* and camera availability are the **consumer's** responsibility (`scope.md` "permission boundary"); the SDK reports clearly-typed errors when a capture cannot proceed.

**Reader-not-oracle throughout:** if a feature tempts the SDK to judge ("this is too blurry," "this `O` is probably a `0`"), the resolution is always to *surface the observation and let the consumer decide* — never to silently gate or overwrite. The raw reading is the source of truth.

## Consequences

**Positive:**
- The analyse-frame core is testable without a device and keeps the door open for USB / web / desktop frame sources at no extra design cost.
- Consumers get a one-call `start()` / `stop()` path without touching platform camera APIs.
- Strict-default + expose-not-gate keeps `0.2.0` firmly within reader-not-oracle; nothing auto-corrects.

**Negative:**
- Shipping two API layers is more surface to design and keep stable (under ADR-007) than a single entry point.
- Deferring tolerant mode means `0.2.0` live camera handles OCR noise only by retrying frames — fine for a live stream, but the still-image case waits for `0.3.0`.

**Neutral:**
- The common contract is designed Android-first but validated against iOS before it is finalised at the `0.2.0` tag, since coordinates lock only at the tag.

## Alternatives Considered

- **Silent OCR auto-correction (a naive "tolerant" mode).** Rejected: auto-correcting a misread is the SDK *deciding* what the document says — oracle behaviour, and a listed anti-pattern. If tolerant mode ships (`0.3.0`), it must *surface* candidates, not overwrite.
- **A quality *gate* (refuse to return data below a threshold).** Rejected: it makes the SDK decide and refuse to return data, violating reader-not-oracle and "validation never gates extraction." Quality is exposed, not enforced.
- **Analyse-frame API only (consumer always owns the camera).** Rejected: it pushes camera-session boilerplate onto every consumer. The convenience layer is the common case.
- **Owns-session API only.** Rejected: it would make testing require a real camera and shut out alternative frame sources (USB / web / desktop).

## Related Decisions

- [ADR-004](0004-reader-not-oracle.md) — the stance this whole design applies to camera / OCR.
- [ADR-006](0006-no-isvalid-boolean.md) — same anti-oracle reasoning (no verdicts).
- [ADR-013](0013-recognition-failures-are-warnings.md) — surface-don't-fail precedent for uncertain signals.
- [ADR-017](0017-mobile-targets-and-build-stack.md) — the targets this runs on.
- [ADR-019](0019-ios-distribution-via-spm.md) — the Swift-friendly surface the iOS API must present.

## Related Documents

- [`../features/mrz-camera-reading.md`](../features/mrz-camera-reading.md) — the feature doc this decision shapes.
- [`../features/mrz-error-taxonomy.md`](../features/mrz-error-taxonomy.md) — the `Camera…` capture-error family.
- [`../reading-risks.md`](../reading-risks.md) — live-camera risk profile.
- [`../scope.md`](../scope.md) — reading methods and capabilities.
