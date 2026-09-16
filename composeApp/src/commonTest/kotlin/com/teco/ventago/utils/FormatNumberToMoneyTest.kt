package com.teco.ventago.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatNumberToMoneyTest {

    @Test
    fun formatsUsdWithThousandsAndTwoDecimals() {
        assertEquals("$0.00", formatNumberToMoney("0"))
        assertEquals("$5.00", formatNumberToMoney("5"))
        assertEquals("$5,035.00", formatNumberToMoney("5035"))
        assertEquals("$5,035.50", formatNumberToMoney("5035.5"))
        assertEquals("$1,234,567.89", formatNumberToMoney("1234567.89"))
    }

    @Test
    fun putsMinusBeforeDollarSign() {
        assertEquals("-$1,234.56", formatNumberToMoney("-1234.56"))
        assertEquals("$0.00", formatNumberToMoney("-0"))
    }

    @Test
    fun ignoresInvalidInput() {
        assertEquals("$0.00", formatNumberToMoney(""))
        assertEquals("$0.00", formatNumberToMoney("abc"))
    }
}
