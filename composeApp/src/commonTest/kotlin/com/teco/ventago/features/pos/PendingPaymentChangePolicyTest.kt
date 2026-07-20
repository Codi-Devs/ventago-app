package com.teco.ventago.features.pos

import com.teco.ventago.features.pos.ui.viewmodel.PendingPaymentIntentMethod
import com.teco.ventago.features.pos.ui.viewmodel.PaymentFlowMode
import com.teco.ventago.features.pos.ui.viewmodel.shouldShowPendingPaymentChangeOption
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PendingPaymentChangePolicyTest {
    @Test
    fun paymentLinkReplacementHidesPaymentLinkAndDraft() {
        assertFalse(
            shouldShowPendingPaymentChangeOption(
                mode = PaymentFlowMode.PAYMENT_LINK,
                sourceMethod = PendingPaymentIntentMethod.PAYMENT_LINK,
                normallyVisible = true,
            )
        )
        assertFalse(
            shouldShowPendingPaymentChangeOption(
                mode = PaymentFlowMode.DRAFT,
                sourceMethod = PendingPaymentIntentMethod.PAYMENT_LINK,
                normallyVisible = true,
            )
        )
        assertTrue(
            shouldShowPendingPaymentChangeOption(
                mode = PaymentFlowMode.YAPPY_ONSITE,
                sourceMethod = PendingPaymentIntentMethod.PAYMENT_LINK,
                normallyVisible = true,
            )
        )
    }

    @Test
    fun yappyReplacementHidesYappyAndDraft() {
        assertFalse(
            shouldShowPendingPaymentChangeOption(
                mode = PaymentFlowMode.YAPPY_ONSITE,
                sourceMethod = PendingPaymentIntentMethod.YAPPY_ONSITE,
                normallyVisible = true,
            )
        )
        assertFalse(
            shouldShowPendingPaymentChangeOption(
                mode = PaymentFlowMode.DRAFT,
                sourceMethod = PendingPaymentIntentMethod.YAPPY_ONSITE,
                normallyVisible = true,
            )
        )
        assertTrue(
            shouldShowPendingPaymentChangeOption(
                mode = PaymentFlowMode.PAYMENT_LINK,
                sourceMethod = PendingPaymentIntentMethod.YAPPY_ONSITE,
                normallyVisible = true,
            )
        )
    }
}
