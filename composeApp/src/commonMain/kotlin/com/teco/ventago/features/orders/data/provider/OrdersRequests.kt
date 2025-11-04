package com.teco.ventago.features.orders.data.provider

import com.teco.ventago.utils.escapeJsonString


object OrdersRequests {



    fun isBusinessRegistered(businessId: Int): String =
        """
            {
                "business_id": $businessId
            }
        """.trimIndent()

    fun findOrderByOrderNumber(businessId: Int, orderNumber: String): String =
        """
            {
                "order_number": "${escapeJsonString(orderNumber)}",
                "business_id": $businessId
            }
        """.trimIndent()

    fun findOrderByCUFE(businessId: Int, cufe: String): String =
        """
            {
                "cufe": "${escapeJsonString(cufe)}",
                "business_id": $businessId
            }
        """.trimIndent()

    fun loadOrders(businessId: Int, pageSize: Int, page: Int): String =
        """
            {
                "page": $page,
                "page_size": $pageSize
            }
        """.trimIndent()

    fun changeOrderStatus(orderId: Int, status: Int, businessId: Int): String =
        """
            {
                "order_id": $orderId,
                "status_id": $status,
                "business_id": $businessId
            }
        """.trimIndent()

    fun rejectOrder(orderId: Int, reason: String, businessId: Int): String =
        """
            {
                "order_id": $orderId,
                "reject_reason": "$reason",
                "business_id": $businessId
            }
        """.trimIndent()

    fun changeOrdersEnabled(businessId: Int, enabled: Boolean): String =
        """
            {
                "business_id": $businessId,
                "orders_enabled": $enabled
            }
        """.trimIndent()



    fun registerManualTransference(businessId: Int, orderNumber: String, paymentReference: String, description: String): String =
        """
            {
                "business_id": $businessId,
                "order_number": "$orderNumber",
                "payment_method_id": 10,
                "payment_reference": "$paymentReference",
                "description": "${escapeJsonString(description)}"
            }
        """.trimIndent()
}