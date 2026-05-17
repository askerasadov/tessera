package io.lightine.tessera.mrz.formats

public data class FieldSpec(
    public val line: Int,
    public val startInLine: Int,
    public val endInLineExclusive: Int,
) {
    public val width: Int
        get() = endInLineExclusive - startInLine
}

public fun FieldSpec.extractFrom(lines: List<String>): String = lines[line].substring(startInLine, endInLineExclusive)

public fun FieldSpec.extractCharFrom(lines: List<String>): Char = lines[line][startInLine]
