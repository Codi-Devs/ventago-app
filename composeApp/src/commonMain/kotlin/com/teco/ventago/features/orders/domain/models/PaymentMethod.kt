package com.teco.ventago.features.orders.domain.models//package com.teco.ventago.features.orders.domain.models


enum class ManualPaymentMethodOption(val id: Int, val displayName: String) {
    CREDIT_ACCOUNTS_RECEIVABLE(1, "Crédito (cuentas por cobrar)"),
    CASH(2, "Efectivo"),
    CREDIT_CARD(3, "Tarjeta crédito"),
    DEBIT_CARD(4, "Tarjeta débito"),
    LOYALTY_CARD(5, "Tarjeta fidelización"),
    VOUCHER(6, "Vale"),
    GIFT_CARD(7, "Tarjeta de regalo"),
    BANK_TRANSFER(8, "Transferencia bancaria"),
    CHECK(9, "Cheque"),
    PUNTO_PAGO(10, "Punto Pago"),
    OTHER_SPECIFY(99, "Otro (especificar)");

    companion object {
        fun fromId(id: Int): ManualPaymentMethodOption? {
            return entries.find { it.id == id }
        }

        fun getAllOptions(): List<ManualPaymentMethodOption> {
            return entries.toList()
        }

        fun getAllOptionsPairs(): List<Pair<Int, String>> {
            return listOf(
                1 to "Crédito (cuentas por cobrar) 2",
                2 to "Efectivo",
                3 to "Tarjeta crédito",
                4 to "Tarjeta débito",
                5 to "Tarjeta fidelización",
                6 to "Vale",
                7 to "Tarjeta de regalo",
                8 to "Transferencia bancaria",
                9 to "Cheque",
                10 to "Punto Pago",
                99 to "Otro (especificar)"
            )
        }
    }
}



