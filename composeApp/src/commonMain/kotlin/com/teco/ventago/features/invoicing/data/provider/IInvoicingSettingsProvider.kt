package com.teco.ventago.features.invoicing.data.provider

import com.teco.ventago.features.invoicing.domain.models.BottomNoteSettingsRequest
import com.teco.ventago.utils.ApiResponse

interface IInvoicingSettingsProvider {
    suspend fun getBottomNoteSettings(businessId: Int): ApiResponse
    suspend fun createBottomNoteSettings(businessId: Int, request: BottomNoteSettingsRequest): ApiResponse
    suspend fun updateBottomNoteSettings(businessId: Int, request: BottomNoteSettingsRequest): ApiResponse
    suspend fun deleteBottomNoteSettings(businessId: Int): ApiResponse
}
