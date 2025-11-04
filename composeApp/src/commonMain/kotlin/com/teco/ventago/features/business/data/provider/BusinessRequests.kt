package com.teco.ventago.features.business.data.provider

import com.teco.ventago.features.business.domain.model.Country
import com.teco.ventago.features.business.domain.model.requests.RegisterBusinessRequest


/**
 * Helper class to build business endpoints request body JSON
 */
object BusinessRequests {

    /**
     * endpoint: set-colors
     */
    fun updateWebStyle(businessId: Int, styleId: Int, primaryColor: String, secondaryColor: String) : String =
        """
            {
                "id": $businessId,
                "style_id": $styleId,
                "primary_color": "$primaryColor",
                "secondary_color": "$secondaryColor"
            }
        """.trimIndent()


    /**
     * endpoint: reset-colors
     */
    fun resetColors(businessId: Int): String =
        """
            {
                "id": $businessId
            }
        """.trimIndent()


    /**
     * endpoint: register, currency and country hardcoded to Panama and PAB (338)
     */
    fun register(request: RegisterBusinessRequest): String =
        """
            {
                "name": "${request.name}",
                "desc": "${request.desc}",
                "ruc": "${request.ruc}",
                "web": "${request.web}",
                "business_phone": "${request.businessPhone}",
                "business_email": "${request.businessEmail}",
                "currency_id": 338,
                "country": "PA",
                "image_data": "${request.img}"
            }
        """.trimIndent()


    /**
     * endpoint: register
     */
    fun getBusinessById(businessId: Int): String =
        """
            {
                "id": $businessId
            }
        """.trimIndent()


    /**
     * endpoint: add-branch
     */
    fun addBranch(businessId: Int, name: String, province: String, district: String, corregimiento: String): String =
        """
            {
                "business_id": $businessId,
                "name": "$name",
                "province": "$province",
                "district": "$district",
                "corregimiento": "$corregimiento",
            }
        """.trimIndent()


    /**
     * endpoint: update-branch
     */
    fun updateBranch(businessId: Int, branchCode: String, name: String, province: String, district: String, corregimiento: String): String =
        """
            {
                "business_id": $businessId,
                "branch_code": "$branchCode",
                "name": "$name",
                "province": "$province",
                "district": "$district",
                "corregimiento": "$corregimiento",
            }
        """.trimIndent()
}