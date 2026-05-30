# MRZ Camera Reading

Headless live-camera reading of the Machine Readable Zone: the SDK receives camera frames, locates and OCRs the MRZ region, and returns the same structured, validated result that string parsing produces — with no UI of its own. The consumer owns all presentation (camera preview, overlay, prompts); a ready-made scanner UI is a later release (0.5.0).

**Status:** Living
**Available since:** 0.2.0
**Platform availability:** Android (CameraX + ML Kit Text Recognition) and iOS (AVFoundation + Apple Vision). The analyse-frame core is frame-source-agnostic, so future sources (USB document readers, web, desktop) can feed the same API.

> The Kotlin-flavored API shapes below are **illustrative, not authoritative** — exact names, signatures, and visibility are finalized as the 0.2.0 slices land and are locked at the 0.2.0 tag (per [ADR-007](../decisions/0007-strict-backward-compat-from-0x.md)). The architectural decisions behind this feature are recorded in [ADR-020](../decisions/0020-camera-reading-architecture.md).

---

## What it does

Camera reading is an **I/O layer** on top of the pure `mrz-core` parser. Each frame travels: *camera frame → locate MRZ region → OCR (platform engine) → candidate string → `mrz-core` parse/validate → result*. The OCR step is platform-specific (ML Kit on Android, Apple Vision on iOS); everything from the candidate string onward is the existing, shared parsing/validation logic ([mrz-parsing.md](mrz-parsing.md), [mrz-validation.md](mrz-validation.md)). Camera reading adds **no new trust decisions** — it is a new way to *obtain* the string, not a new way to *judge* it (Principle 1).

Results carry [read-method metadata](mrz-data-model.md) identifying the source as live camera.

## Two integration layers

The public API ships in two layers; the convenience is built on the core (see [ADR-020](../decisions/0020-camera-reading-architecture.md)).

### 1. Analyse a frame — the core

The low-level entry point: hand it a platform frame, get a result. It owns no camera, so it is unit-testable with injected frames + mock OCR, and it is the seam any frame source feeds. The OCR engine is itself injected through a generic seam, `MrzTextRecognizer<F>`, whose frame type `F` is the extension point — Android binds `F = ImageProxy`; a future USB/desktop/web source binds its own.

```kotlin
// Illustrative — not authoritative. Shapes as of the analyse-frame slice; locked at the 0.2.0 tag.
fun interface MrzTextRecognizer<in F> {
    suspend fun recognize(frame: F): RecognizedText   // OCR only; throwing surfaces CameraError.OcrFailed
}

class MrzFrameAnalyzer<F>(
    recognizer: MrzTextRecognizer<F>,
    mode: ParsingMode = ParsingMode.STRICT,            // STRICT | LENIENT
    telemetry: TelemetrySink = NoOpTelemetrySink,      // emits one non-PII CameraFrameEvent per frame
) {
    suspend fun analyse(frame: F): MrzScanResult
}

// Android binds F = ImageProxy via the bundled ML Kit recognizer:
val analyzer = MrzFrameAnalyzer(MlKitMrzTextRecognizer())
```

`MrzScanResult` is a sealed result — `Decoded` (carrying the parser's `ParseResult` verdict and the raw `RecognizedText`), `NoMrzFound`, or `CaptureError` — and every variant exposes `ScanQuality` metadata (MRZ-region found?, OCR confidence, recognized-line count).

### 2. Own the camera session — the convenience

The common path: the SDK runs the platform camera internally and streams results. The consumer never touches `bindToLifecycle` / `ImageAnalysis` / `AVCaptureSession`.

```kotlin
// Illustrative — not authoritative.
interface MrzCameraScanner {
    val results: Flow<MrzScanResult>   // Compose-friendly; bridges cleanly to Swift
    fun start()
    fun stop()
}
```

Still headless: if the consumer wants a live preview, they attach their own preview surface (a "preview hook"); the SDK draws nothing.

## Parsing modes

Consumer-chosen; **strict is the default**, and the **raw OCR reading is always exposed** regardless of mode.

- **Strict** — accept only a conformant MRZ; otherwise report a typed error. For live camera this pairs with **next-frame retry**: a noisy frame fails and is dropped, and a clean frame arrives within milliseconds, so no correction is needed.
- **Lenient** — tolerate benign formatting noise (e.g. stray whitespace) without changing any data value.
- **Tolerant** (check-digit-guided OCR disambiguation) is **not** in 0.2.0 — it is deferred to 0.3.0 (still-image reading), and when added it will *surface* candidate corrections, never silently overwrite. See [open-questions.md](../open-questions.md) ("Lenient and tolerant parsing modes").

## Quality signals — exposed, never gated

The result surfaces the natural signals the pipeline produces — whether an MRZ region was found, OCR confidence, and the parse/validation outcome — as **metadata**. The SDK never refuses to return data on quality grounds; the consumer sets any threshold and decides whether to prompt for a re-capture (Principle 1, Principle 5). A richer quality scorer (blur/glare) is deferred to 0.3.0.

## Errors

Capture-layer failures are a **separate `Camera…` typed family**, distinct from the `mrz-core` parse/validation taxonomy ([mrz-error-taxonomy.md](mrz-error-taxonomy.md)). They are surfaced as a **sealed result** (not thrown, never crashing or hanging), with stable English codes the consumer localizes — e.g. camera unavailable, permission denied, camera in use. **Permission requests and camera availability are the consumer's responsibility** (scope.md "permission boundary"); the SDK only reports clearly-typed errors when a capture cannot proceed. The exact `Camera…` set grows through implementation (per the "new error → taxonomy + test" rule).

## Status of Implementation

| Capability | Status |
|---|---|
| Analyse-frame core (Android) | Implemented (0.2.0, `mrz-camera-android`) — host-tested with mock OCR |
| Android ML Kit recognizer (bundled model) | Implemented (0.2.0) — compiled on CI; device/emulator OCR verified in a later slice |
| Strict + lenient modes | Implemented (0.2.0) |
| Quality signals as metadata | Implemented (0.2.0) |
| `Camera…` error family | Seeded (0.2.0) — `CameraError.OcrFailed`; grows with the owns-session layer |
| Owns-camera-session convenience (Android) | Planned (0.2.0) |
| Analyse-frame core + convenience (iOS) | Planned (0.2.0, after Android) |
| Tolerant mode; richer quality scorer | Deferred (0.3.0) |
| Scanner UI | Deferred (0.5.0) |

## Behavioral commitments

- **Headless** — no UI; the consumer owns all presentation.
- **Reader, not oracle** — quality and OCR ambiguity are *surfaced*, never gated or silently corrected; the raw reading is the source of truth.
- **No new persistence or network** — frames are processed in memory and released promptly (memory hygiene); no frame or result is stored or transmitted by the SDK.
- **Same result type as string parsing** — a camera-sourced MRZ validates identically to a typed-in one; only the read-method metadata differs.

## Relationship to other features

- **[mrz-parsing.md](mrz-parsing.md) / [mrz-validation.md](mrz-validation.md)** — reused unchanged once a candidate string is produced.
- **[mrz-error-taxonomy.md](mrz-error-taxonomy.md)** — gains the `Camera…` capture-error family.
- **[reading-risks.md](../reading-risks.md)** — the live-camera risk profile.
- **[telemetry.md](telemetry.md)** — camera is the first real emitter of telemetry events (frame/OCR/parse).

## Related principles

- **Principle 1 (Reader, not oracle)** — surface, don't decide.
- **Principle 5 (Transparency)** — raw reading + quality signals exposed.
- **Principle 8 (Native fit)** — native camera/OCR per platform.

## Related documents

- [ADR-020](../decisions/0020-camera-reading-architecture.md) — camera reading architecture.
- [ADR-017](../decisions/0017-mobile-targets-and-build-stack.md) — the mobile targets this runs on.
- [scope.md](../scope.md) — reading methods.
- [development-setup.md](../development-setup.md) — building/running the camera modules.
