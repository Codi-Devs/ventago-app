package com.teco.ventago.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleHelperTest {

    @Test
    fun appLocaleIsAlwaysSpanish() {
        assertEquals("ES", LocaleHelper.getLocale())
    }

    @Test
    fun moneySeparatorsStayEnUs() {
        assertEquals('.', LocaleHelper.getDecimalSeparator())
        assertEquals(',', LocaleHelper.getGroupingSeparator())
        assertEquals('0', LocaleHelper.getZeroDigit())
    }
}
