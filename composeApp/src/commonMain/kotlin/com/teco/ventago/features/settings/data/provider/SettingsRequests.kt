package com.teco.ventago.features.settings.data.provider

import com.teco.ventago.features.business.domain.model.BusinessAddress
import com.teco.ventago.features.business.domain.model.BusinessSocialNetwork
import com.teco.ventago.utils.formatted

/**
 * Helper class to build settings endpoints request body JSON
 */
object SettingsRequests {

    /**
     * endpoint: allow-whatsapp-orders
     */
    fun allowWhatsapp(businessId: Int, allow: Boolean): String =
        """
            {
                "id": $businessId,
                "allow": $allow
            }
        """.trimIndent()

    /**
     * endpoint: update-business-social-networks
     */
    fun updateBusinessSocialNetworks(businessId: Int, social: BusinessSocialNetwork): String =
        """
            {
                "id": $businessId,
                "social_networks": {
                    "facebook": "${social.facebook}",
                    "instagram": "${social.instagram}",
                    "youtube": "${social.youtube}",
                    "whatsapp": "${social.whatsapp}"
                }
            }
        """.trimIndent()

    /**
     * endpoint: change-name
     */
    fun changeBusinessName(businessId: Int, name: String): String =
        """
            {
                "id": $businessId,
                "name": "$name"
            }
        """.trimIndent()


    /**
     * endpoint: change-info
     */
    fun changeBusinessInfo(name: String, phone: String, ruc: String, website: String, businessEmail: String, businessId: Int): String =
        """
            {
                "id": $businessId,
                "name": "$name",
                "phone": "$phone",
                "ruc": "$ruc",
                "web": "$website",
                "business_email": "$businessEmail"
            }
        """.trimIndent()

    fun changeBusinessPhone(businessId: Int, phone: String): String =
        """
            {
                "id": $businessId,
                "phone":"$phone"
            }
        """.trimIndent()

    fun changeBusinessAddress(address: BusinessAddress, businessId: Int): String =
        """
            {
                "id": $businessId,
                "place_id": "${address.placeId}",
                "place_address": "${address.placeAddress}",
                "place_lat": "${address.placeLat}",
                "place_lon": "${address.placeLng}"
            }
        """.trimIndent()

    fun updateBusinessLogo(businessId: Int, logo: String): String =
        """
            {
                "id": $businessId,
                "logo":"$logo"
            }
        """.trimIndent()


}
