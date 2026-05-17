package io.lightine.tessera.mrz.formats

/**
 * The position of a single field within an MRZ format. Specifies the [line] index
 * (zero-based) and the half-open character range `[startInLine, endInLineExclusive)` on
 * that line.
 *
 * Used by every [MrzFormatSpec] to name field positions, by the parser to slice fields
 * out of input, and by the validator and error types to report positions for diagnostics.
 *
 * The "exclusive end" convention matches Kotlin's `String.substring(startIndex, endIndex)`
 * and most other range-handling code in the standard library.
 */
public data class FieldSpec(
    public val line: Int,
    public val startInLine: Int,
    public val endInLineExclusive: Int,
) {
    /** The number of characters this field occupies on its line. */
    public val width: Int
        get() = endInLineExclusive - startInLine
}

/** Extract this field's value from [lines] as a string. */
public fun FieldSpec.extractFrom(lines: List<String>): String = lines[line].substring(startInLine, endInLineExclusive)

/**
 * Extract this field's first character from [lines]. Useful for single-character fields
 * such as check digits and the sex field, which avoids the wrapper allocation of
 * [extractFrom].
 */
public fun FieldSpec.extractCharFrom(lines: List<String>): Char = lines[line][startInLine]
