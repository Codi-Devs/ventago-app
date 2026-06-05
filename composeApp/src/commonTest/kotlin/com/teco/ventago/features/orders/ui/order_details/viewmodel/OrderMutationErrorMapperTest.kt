package com.teco.ventago.features.orders.ui.order_details.viewModel

import kotlin.test.Test
import kotlin.test.assertEquals

class OrderMutationErrorMapperTest {

    @Test
    fun messageForReturnsBackendMessageForInvoiceCancellationReasonTooShort() {
        val error = Exception(
            """
            {"successful":false,"data":{"message":"El motivo de anulación debe tener al menos 15 caracteres"},"error":"INV_003","errorMessage":"INV_003"}
            """.trimIndent()
        )

        val message = OrderMutationErrorMapper.messageFor(
            error = error,
            fallback = "No se pudo anular el pedido."
        )

        assertEquals("El motivo de anulación debe tener al menos 15 caracteres", message)
    }

    @Test
    fun messageForReturnsBackendMessageForInvoiceNotYetOnDgi() {
        val error = Exception(
            """
            {"successful":false,"data":{"message":"La factura no se puede anular todavía porque aún no está disponible en la DGI"},"error":"INV_004","errorMessage":"INV_004"}
            """.trimIndent()
        )

        val message = OrderMutationErrorMapper.messageFor(
            error = error,
            fallback = "No se pudo anular el pedido."
        )

        assertEquals("La factura no se puede anular todavía porque aún no está disponible en la DGI", message)
    }

    @Test
    fun messageForAllowsCancelFlowToOverrideGenericInternalFallback() {
        val error = Exception(
            """
            {"successful":false,"data":null,"error":"O_RP_001","errorMessage":"O_RP_001"}
            """.trimIndent()
        )

        val message = OrderMutationErrorMapper.messageFor(
            error = error,
            fallback = "No se pudo anular el pedido.",
            codeOverrides = mapOf("O_RP_001" to "No se pudo anular el pedido.")
        )

        assertEquals("No se pudo anular el pedido.", message)
    }
}
