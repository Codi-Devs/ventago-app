package com.teco.ventago.features.branches.data.provider

import com.teco.ventago.utils.ApiResponse

interface IBranchProvider {

    suspend fun getBranches(businessId: Int): ApiResponse

    // Billing Point endpoints
    suspend fun addBillingPoint(businessId: Int, branchCode: String, name: String, code: String, status: Int): ApiResponse

    suspend fun updateBillingPoint(
        businessId: Int,
        branchCode: String,
        billingPoint: String,
        name: String,
        status: Int,
    ): ApiResponse
}

