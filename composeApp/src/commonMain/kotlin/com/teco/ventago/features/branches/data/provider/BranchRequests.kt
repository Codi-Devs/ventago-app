package com.teco.ventago.features.branches.data.provider


/**
 * Helper class to build business endpoints request body JSON
 */
object BranchRequests {
    /**
     * endpoint: add-billing-point
     */
    fun addBillingPoint(name: String, code: String, status: Int): String =
        """
            {
                "code": "$code",
                "description": "$name",
                "status": $status
            }
        """.trimIndent()

    /**
     * endpoint: update-billing-point
     */
    fun updateBillingPoint( name: String, status: Int): String =
        """
            {
                "description": "$name",
                "status": $status
            }
        """.trimIndent()



}