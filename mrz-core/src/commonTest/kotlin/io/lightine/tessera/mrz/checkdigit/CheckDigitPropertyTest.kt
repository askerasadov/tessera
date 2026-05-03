package io.lightine.tessera.mrz.checkdigit

import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.next
import kotlin.test.Test
import kotlin.test.assertTrue

class CheckDigitPropertyTest {
    private val mrzAlphabet: List<Char> = ('A'..'Z').toList() + ('0'..'9').toList() + '<'
    private val mrzCharArb: Arb<Char> = Arb.element(mrzAlphabet)
    private val mrzStringArb: Arb<String> =
        Arb.list(mrzCharArb, 0..44).map { it.joinToString("") }

    @Test
    fun result_is_always_a_decimal_digit_for_any_valid_mrz_alphabet_input() {
        repeat(500) {
            val input = mrzStringArb.next()
            val digit = computeCheckDigit(input)
            assertTrue(
                digit in '0'..'9',
                "Expected '0'..'9' but got '$digit' for input '$input'",
            )
        }
    }
}
