package com.teco.ventago.features.expenses.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PaymentMethod(val value: String, val label: String) {
    @SerialName("cash")
    CASH("cash", "Efectivo"),

    @SerialName("bank_transfer")
    BANK_TRANSFER("bank_transfer", "Transferencia bancaria"),

    @SerialName("credit_card")
    CREDIT_CARD("credit_card", "Tarjeta de crédito"),

    @SerialName("debit_card")
    DEBIT_CARD("debit_card", "Tarjeta de débito"),

    @SerialName("credit")
    CREDIT("credit", "Crédito"),

    @SerialName("check")
    CHECK("check", "Cheque"),

    @SerialName("yappy")
    YAPPY("yappy", "Yappy"),

    @SerialName("paypal")
    PAYPAL("paypal", "PayPal");

    companion object {
        fun fromValue(value: String?): PaymentMethod? {
            return entries.find { it.value == value }
        }

        fun getLabel(value: String?): String {
            return fromValue(value)?.label ?: value ?: "-"
        }

        fun getAllMethods(): List<PaymentMethod> = entries
    }
}
