package com.teco.ventago.features.home.data.repository

import com.teco.ventago.features.home.domain.model.HomeSummary

interface IHomeSummaryRepository {
    suspend fun getHomeSummary(businessId: Int): HomeSummary
}
