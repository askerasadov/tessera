package io.lightine.tessera.mrz.checkdigit

private val WEIGHTS = intArrayOf(7, 3, 1)

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
