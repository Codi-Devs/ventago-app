package com.teco.ventago.features.product.data.provider.product

object ProductRequests {
    /**
     * endpoint: get-menu-by-business
     */
    fun getMenuByBusinessId(businessId: Int): String =
        """
            {
                "id_business": $businessId
            }
        """.trimIndent()
}