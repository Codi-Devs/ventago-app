package com.teco.ventago.features.customers.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValidateRucResponse(
    @SerialName("ruc_type")
    val rucType: String,

    @SerialName("ruc")
    val ruc: String,

    @SerialName("dv")
    val dv: String,

    @SerialName("legal_name")
    val legalName: String,

    @SerialName("fe_affiliated")
    val feAffiliated: Boolean
)