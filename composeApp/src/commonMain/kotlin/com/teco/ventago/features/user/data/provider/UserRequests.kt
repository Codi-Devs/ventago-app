package com.teco.ventago.features.user.data.provider

object UserRequests {

    /**
     * endpoint: set-premium
     */
    fun setPremium(premium: Boolean) : String =
        """
            {
                "premium": $premium
            }
        """.trimIndent()

}