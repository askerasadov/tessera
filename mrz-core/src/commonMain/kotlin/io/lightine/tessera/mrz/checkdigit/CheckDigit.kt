package io.lightine.tessera.mrz.checkdigit

private val WEIGHTS = intArrayOf(7, 3, 1)

/**
 * Computes the ICAO Doc 9303 Part 3 check digit for an MRZ field.
 *
 * The algorithm: for each character in [input], multiply its numeric value by the
 * repeating weight sequence `7, 3, 1, 7, 3, 1, …`, sum the products, and return
 * `sum mod 10` as a single ASCII digit character (`'0'`–`'9'`).
 *
 * Character values:
 * - `'0'`–`'9'` are their face value (`0`–`9`)
 * - `'A'`–`'Z'` map to `10`–`35`
 * - `'<'` (the MRZ filler) is `0`
 *
 * @throws IllegalArgumentException if [input] contains any character outside the MRZ
 *   alphabet (`A`–`Z`, `0`–`9`, `<`). Callers receiving raw consumer input should
 *   validate the alphabet first; the parser does this and translates the throw into
 *   [`MrzCharacterSetViolation`][io.lightine.tessera.types.errors.MrzCharacterSetViolation].
 */
public fun computeCheckDigit(input: String): Char {
    var sum = 0
    for (index in input.indices) {
        sum += characterValue(input[index]) * WEIGHTS[index % 3]
    }
    return '0' + (sum % 10)
}

private fun characterValue(c: Char): Int =
    when (c) {
        in '0'..'9' -> c - '0'

        in 'A'..'Z' -> c - 'A' + 10

        '<' -> 0

        else -> throw IllegalArgumentException(
            "Character '$c' is outside the MRZ alphabet (A-Z, 0-9, <)",
        )
    }
