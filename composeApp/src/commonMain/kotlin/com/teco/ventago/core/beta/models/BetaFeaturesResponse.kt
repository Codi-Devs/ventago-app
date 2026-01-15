package com.teco.ventago.core.beta.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BetaFeatureDetail(
    val key: String,
    val name: String? = null,
    val description: String? = null,
    @SerialName("is_released") val isReleased: Boolean? = null
)

@Serializable
data class BetaFeaturesResponse(
    val features: List<String>? = null,
    @SerialName("feature_details") val featureDetails: List<BetaFeatureDetail>? = null
)

@Serializable
data class BetaAccessResponse(
    @SerialName("has_access") val hasAccess: Boolean = false
)
