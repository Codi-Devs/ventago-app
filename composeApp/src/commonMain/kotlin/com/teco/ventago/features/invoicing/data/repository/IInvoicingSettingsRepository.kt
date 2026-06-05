package com.teco.ventago.features.invoicing.data.repository

import com.teco.ventago.features.invoicing.domain.models.BottomNoteSettings
import com.teco.ventago.features.invoicing.domain.models.BottomNoteSettingsRequest

interface IInvoicingSettingsRepository {
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
