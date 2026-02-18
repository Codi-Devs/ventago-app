package com.teco.ventago.features.home.data.provider

import com.teco.ventago.utils.ApiResponse

interface IHomeSummaryProvider {
    suspend fun getHomeSummary(businessId: Int): ApiResponse
}
