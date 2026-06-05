package com.teco.ventago.features.pos.ui.customer.add.viewmodel

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AddCustomerValidatorsTest {

    @Test
    fun addressValidationRejectsNullBlankAndShortNonBlankValues() {
        assertFalse(hasMinimumAddressCharacters(null))
        assertFalse(hasMinimumAddressCharacters(""))
        assertFalse(hasMinimumAddressCharacters("     "))
        assertFalse(hasMinimumAddressCharacters("ab cd"))
    }

    @Test
    fun addressValidationAcceptsFiveNonBlankCharactersEvenWithSpaces() {
        assertTrue(hasMinimumAddressCharacters("abcde"))
        assertTrue(hasMinimumAddressCharacters("ab cd e"))
    }
}
