# Known Pitfalls

This document captures specific failure modes that have surfaced during the project's design phase. They are not theoretical risks — they are mistakes that have actually been made and corrected. Each entry describes the pitfall, why it happens, and what to do instead.

The point is to surface these patterns so they are caught early in implementation rather than relived. Future contributors (human or AI) reading this should leave with concrete things to watch for.

---

## Drifting Toward Oracle Behavior

**The pitfall:** Adding "convenience" features that subtly cross from reading data to making trust decisions.

**What it looks like:**
- An `isValid` boolean on result types ("just to make it easier")
- Auto-correction of common OCR errors ("the user clearly meant `O` not `0`")
- Inferring a transliteration profile from the issuing state ("we can tell what they meant")
- Silently filtering out validation failures the SDK considers "not important"
- Returning `null` instead of data when something failed validation

Each of these *feels* helpful. Each of them violates Principle 1 (Reader, not oracle). The SDK does not decide what valid means, what the user meant, or what the consumer should do.

**What to do instead:** When tempted to add a "smart" behavior, ask: am I making a trust decision on the consumer's behalf? If yes, stop. The consumer has context the SDK does not have. The SDK reports observations; the consumer interprets them.

---

## Embedding Version Numbers in Prose

**The pitfall:** Writing "the 0.1.0 parser handles X" or "as of 0.2.0, this changes" in feature documentation body text.

**Why it happens:** It feels precise to anchor a description to a specific version. It signals "I know exactly what I'm describing."

**What's wrong with it:** Feature documentation describes features as they currently exist. The structural marker `Available since X.Y.Z` at the top of the document already conveys version information. Embedding versions in prose creates maintenance burden — every release that touches the feature requires text edits across the document — and makes the documentation read like a changelog rather than a specification.

**What to do instead:** Write in present tense. "The parser operates in strict mode." "The generator produces conformant output." Use structural markers (`Available since X.Y.Z`, `Deprecated in X.Y.Z`) for version information. Let scope.md and the changelog handle release-specific narratives.

---

## Inconsistent Citation Formats

**The pitfall:** Switching between "ICAO 9303" and "ICAO Doc 9303" interchangeably across documents.

**Why it happens:** Both forms are technically correct. Either feels natural in context. Without discipline, both end up in use.

**What's wrong with it:** Inconsistent citation makes documentation harder to scan. A reader wonders if "ICAO 9303" and "ICAO Doc 9303" refer to different things. Search and reference tools work better with consistent terminology.

**What to do instead:**
- Use **"ICAO Doc 9303"** for citations to the document, especially when citing specific Parts (`ICAO Doc 9303 Part 3 Section 5`)
- Use **"ICAO 9303"** as an adjective for formats and compliance (`ICAO 9303 TD3 format`, `ICAO 9303-compliant`)
- Verify before drafting: which form fits the grammatical context?

---

## Type Names Without the `Mrz` Prefix

**The pitfall:** Naming an MRZ-related error or warning type without the `Mrz` prefix (e.g., `CheckDigitMismatch` instead of `MrzCheckDigitMismatch`).

**Why it happens:** When drafting in isolation, the prefix feels redundant — the context is clear. When type names later appear alongside non-MRZ types (chip data errors, camera errors), the inconsistency becomes visible.

**What to do instead:** Every MRZ-related error, warning, or validation failure type starts with `Mrz`. This convention extends to other domain-specific prefixes that may emerge (`Nfc...`, `Camera...`, `Chip...`). The prefix is part of the public API — changing it later is breaking.

---

## Forward References That Become Stale

**The pitfall:** Writing "see `mrz-validation.md` when written" in `mrz-data-model.md`, then forgetting to update once `mrz-validation.md` exists.

**Why it happens:** Forward references are explicitly allowed by the project's conventions (better to point to intent than not to point at all). But they need to be revisited when the referenced document materializes.

**What to do instead:** Maintain a mental list (or, better, an explicit list in `open-questions.md`) of forward references. When the referenced document is written, update the source reference to remove the "when written" qualifier. A grep check at major milestones catches lingering qualifiers like "when written," "to be written," "will be written."

---

## Naming Specific Organizations

**The pitfall:** Mentioning specific organizations (whether intended customers, employer organizations, or anchor users) in documentation, code, or comments.

**Why it happens:** The project exists in a context. The user works for a specific agency. There are real anchor users in mind. It feels natural to mention them.

**What's wrong with it:** The project is presented as a vendor-neutral SDK. The moment a specific organization is named, the project becomes "that thing the agency is doing" rather than a community resource. This affects credibility, contribution potential, and the user's professional position (separating personal projects from government employment role).

**What to do instead:** Refer to organizations generically. "An issuing state where this character appears regularly" rather than naming the state. "The anchor consumer" rather than "the specific government agency." Standards bodies (ICAO, ISO/IEC) are an exception — they can be cited for accuracy. Specific governments and their agencies are not.

---

## Premature Modularization

**The pitfall:** Creating a new module for every concept rather than starting with internal packages within existing modules.

**Why it happens:** Modular thinking is encouraged by Principle 3, and modular structure looks clean in architecture diagrams. The temptation is to express modular thinking by creating modules.

**What's wrong with it:** Modules carry costs (build configuration, dependency management, public API surface, documentation). Creating modules for concepts that always travel together is paying these costs without benefit. Future-you (or future-Claude) will have to maintain the module-level overhead even when no module-level concern is at play.

**What to do instead:** Apply Principle 11 — internal packages first. A new feature starts as an internal package within an existing module, with a clean public API surface. Promote to a standalone module only when independent reuse, evolution, testing, ownership, shipping, or optional inclusion clearly applies. The internal-package boundary makes promotion mechanical when justified.

---

## Drafting Long Stretches Without Checking In

**The pitfall:** Working through a substantial piece of content in one go, then presenting it as a finished thing, only to discover that a misunderstanding in step one made everything since wrong.

**Why it happens:** Momentum. It feels productive to keep going. Stopping to check in feels like interruption.

**What's wrong with it:** Errors compound. A small misunderstanding in step one, undiscovered, shapes step two and step three. By step five, untangling requires revisiting everything. The user has explicitly said they prefer shorter cycles with verification over longer cycles with surprises.

**What to do instead:** Check in at natural milestones. After major decisions, before substantial drafting, when a question arises that the user might want to answer differently than your default. The cost of a check-in is low; the cost of redoing work after a missed check-in is high.

---

## Over-Specifying What Cannot Be Verified Yet

**The pitfall:** Including detailed specifications in documentation for things that have not been validated against real implementation, real data, or real consumer feedback.

**What it looks like:**
- Listing every error type the parser might produce (some will not exist; some will exist that we did not predict)
- Specifying exact threshold values without ever measuring (e.g., "130 years" for date plausibility)
- Describing API method signatures down to the parameter name (the implementation may diverge)

**What's wrong with it:** Premature specification creates documentation that disagrees with reality. Worse, it locks in choices that should be open until measurement or implementation tells us what works.

**What to do instead:** Specify the *structure* (the contract, the invariants, the principles) and acknowledge what the implementation will reveal. Phrases like "the catalog is discovered through implementation and testing" or "specific thresholds are tuned during implementation" honor Principle 4 (Honest about what we know).

---

## Treating Examples as Specifications

**The pitfall:** Example code in documentation gets read as if it were the actual API contract, leading to over-specification and inflexibility.

**Why it happens:** Examples are concrete and easy to read. Readers naturally treat them as authoritative.

**What's wrong with it:** Examples illustrate; they do not specify. The actual API may differ from the example in details that matter. If readers anchor too hard on examples, the implementation becomes constrained by accidentally-specified details.

**What to do instead:** When code blocks appear in documentation, mark them as illustrative explicitly. Use the phrase "illustrative shape" or "Kotlin-flavored example." Note that actual class names, method names, and parameter shapes are decided at implementation time. Update documentation with the final shapes once implementation lands.

---

## Maintaining This Document

This document grows when new pitfalls are observed during ongoing work. The bar for adding an entry: *has this mistake actually been made on this project, or is it close enough that it could be?* If yes, document it concretely with the specific pattern. If no, do not add speculative pitfalls — `reading-risks.md` and the principles already cover those.

Removed entries: when a pitfall has been so thoroughly internalized that it no longer surfaces, the entry can be moved to a historical note or archived. Most entries should stay — the discipline of remembering is the value.
