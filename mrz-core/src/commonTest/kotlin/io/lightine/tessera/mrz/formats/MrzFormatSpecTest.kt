package io.lightine.tessera.mrz.formats

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class MrzFormatSpecTest {
    // --- Type relationships ---

    @Test
    fun td1_implements_MrzFormatSpecWithComposite() {
        assertIs<MrzFormatSpecWithComposite>(Td1FormatSpec)
        assertIs<MrzFormatSpec>(Td1FormatSpec)
    }

    @Test
    fun td2_implements_MrzFormatSpecWithComposite() {
        assertIs<MrzFormatSpecWithComposite>(Td2FormatSpec)
        assertIs<MrzFormatSpec>(Td2FormatSpec)
    }

    @Test
    fun td3_implements_MrzFormatSpecWithComposite() {
        assertIs<MrzFormatSpecWithComposite>(Td3FormatSpec)
        assertIs<MrzFormatSpec>(Td3FormatSpec)
    }

    @Test
    fun mrva_implements_base_MrzFormatSpec_but_not_composite_subinterface() {
        assertIs<MrzFormatSpec>(MrvAFormatSpec)
        // Type-system enforced: MrvAFormatSpec does NOT have compositeCheckDigit or
        // compositeInputFields in its API surface. Asserting !is on a sealed sub-interface
        // makes this invariant explicit at test time.
        @Suppress("USELESS_IS_CHECK")
        val isWithComposite = MrvAFormatSpec is MrzFormatSpecWithComposite
        assertEquals(false, isWithComposite)
    }

    @Test
    fun mrvb_implements_base_MrzFormatSpec_but_not_composite_subinterface() {
        assertIs<MrzFormatSpec>(MrvBFormatSpec)
        @Suppress("USELESS_IS_CHECK")
        val isWithComposite = MrvBFormatSpec is MrzFormatSpecWithComposite
        assertEquals(false, isWithComposite)
    }

    // --- Default globalPositionOf method works through the interface ---

    @Test
    fun globalPositionOf_default_method_returns_same_value_as_each_spec_used_to_compute_directly() {
        // The default method on MrzFormatSpec: field.line * lineLength + field.startInLine.
        // The five specs used to define this function inline (now removed in favor of the
        // default). Verify the default produces the same global positions for each spec's
        // documentType field (line 0, start 0 → global 0 on every format).
        val specs: List<MrzFormatSpec> =
            listOf(Td1FormatSpec, Td2FormatSpec, Td3FormatSpec, MrvAFormatSpec, MrvBFormatSpec)
        for (spec in specs) {
            assertEquals(0, spec.globalPositionOf(spec.documentType))
        }
    }

    @Test
    fun globalPositionOf_via_interface_returns_correct_value_for_each_spec() {
        // documentNumber position differs per format: TD1 puts it on line 0 at index 5
        // (global = 5), while TD2/TD3/MRV-A/MRV-B put it on line 1 at index 0 (global = lineLength).
        assertEquals(5, (Td1FormatSpec as MrzFormatSpec).globalPositionOf(Td1FormatSpec.documentNumber))
        assertEquals(36, (Td2FormatSpec as MrzFormatSpec).globalPositionOf(Td2FormatSpec.documentNumber))
        assertEquals(44, (Td3FormatSpec as MrzFormatSpec).globalPositionOf(Td3FormatSpec.documentNumber))
        assertEquals(44, (MrvAFormatSpec as MrzFormatSpec).globalPositionOf(MrvAFormatSpec.documentNumber))
        assertEquals(36, (MrvBFormatSpec as MrzFormatSpec).globalPositionOf(MrvBFormatSpec.documentNumber))
    }

    // --- Polymorphic access through the interface ---

    @Test
    fun base_interface_exposes_common_fields_for_every_format() {
        // A consumer that only needs the common fields can hold any spec as `MrzFormatSpec`
        // and access them uniformly. The five base-interface fields all exist on every spec.
        val specs: List<MrzFormatSpec> =
            listOf(Td1FormatSpec, Td2FormatSpec, Td3FormatSpec, MrvAFormatSpec, MrvBFormatSpec)
        for (spec in specs) {
            // Each access here would fail to compile if any spec failed to provide the field.
            assertSame(spec.documentType, spec.documentType)
            assertSame(spec.issuingState, spec.issuingState)
            assertSame(spec.rawNameField, spec.rawNameField)
            assertSame(spec.documentNumber, spec.documentNumber)
            assertSame(spec.documentNumberCheckDigit, spec.documentNumberCheckDigit)
            assertSame(spec.nationality, spec.nationality)
            assertSame(spec.dateOfBirth, spec.dateOfBirth)
            assertSame(spec.dateOfBirthCheckDigit, spec.dateOfBirthCheckDigit)
            assertSame(spec.sex, spec.sex)
            assertSame(spec.dateOfExpiry, spec.dateOfExpiry)
            assertSame(spec.dateOfExpiryCheckDigit, spec.dateOfExpiryCheckDigit)
        }
    }

    @Test
    fun composite_subinterface_exposes_composite_fields_for_td_formats() {
        val withComposite: List<MrzFormatSpecWithComposite> =
            listOf(Td1FormatSpec, Td2FormatSpec, Td3FormatSpec)
        for (spec in withComposite) {
            assertSame(spec.compositeCheckDigit, spec.compositeCheckDigit)
            // Every TD format defines at least one composite-input range.
            assertEquals(true, spec.compositeInputFields.isNotEmpty())
        }
    }
}
