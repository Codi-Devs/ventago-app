package com.teco.ventago.features.invoicing.data.repository

import com.teco.ventago.features.invoicing.domain.models.BottomNoteSettings
import com.teco.ventago.features.invoicing.domain.models.BottomNoteSettingsRequest
import com.teco.ventago.features.invoicing.domain.models.InvoicingSettings
import com.teco.ventago.features.invoicing.domain.models.IncludeAddressOnInvoiceRequest

interface IInvoicingSettingsRepository {
    suspend fun getInvoicingSettings(businessId: Int): InvoicingSettings
    suspend fun updateIncludeAddressOnInvoice(
        businessId: Int,
        request: IncludeAddressOnInvoiceRequest,
    ): Boolean

    suspend fun getBottomNoteSettings(businessId: Int): BottomNoteSettings?
    suspend fun createBottomNoteSettings(
        businessId: Int,
        request: BottomNoteSettingsRequest,
    ): BottomNoteSettings

    suspend fun updateBottomNoteSettings(
        businessId: Int,
        request: BottomNoteSettingsRequest,
    ): BottomNoteSettings

    suspend fun deleteBottomNoteSettings(businessId: Int): Boolean
}
