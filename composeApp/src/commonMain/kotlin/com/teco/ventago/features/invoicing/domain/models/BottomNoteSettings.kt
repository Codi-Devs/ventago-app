package com.teco.ventago.features.invoicing.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BottomNoteSettings(
    val id: Long? = null,
    @SerialName("business_id") val businessId: Int? = null,
    val title: String = "",
    val body: String = "",
    @SerialName("include_on_invoice") val includeOnInvoice: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class BottomNoteSettingsRequest(
    val title: String,
    val body: String,
    @SerialName("include_on_invoice") val includeOnInvoice: Boolean,
)

data class BottomNoteSettingsState(
    val settings: BottomNoteSettings? = null,
    val refreshFailed: Boolean = false,
)

@Serializable
data class InvoicingSettings(
    @SerialName("include_address_on_invoice") val includeAddressOnInvoice: Boolean = false,
)

@Serializable
data class IncludeAddressOnInvoiceRequest(
    @SerialName("include_address_on_invoice") val includeAddressOnInvoice: Boolean,
)

data class InvoicingSettingsState(
    val settings: InvoicingSettings = InvoicingSettings(),
    val refreshFailed: Boolean = false,
)
