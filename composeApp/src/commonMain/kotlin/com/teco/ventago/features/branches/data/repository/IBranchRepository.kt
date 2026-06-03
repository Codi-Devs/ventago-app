package com.teco.ventago.features.branches.data.repository

import com.teco.ventago.core.file.SharedFile
import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint

interface IBranchRepository {

    suspend fun getBranches(businessId: Int): List<Branch>

    suspend fun addBillingPoint(businessId: Int, branchCode: String, name: String, code: String, status: Int): FiscalBillingPoint
    suspend fun updateBillingPoint(businessId: Int, branchCode: String, billingPoint: String, name: String, status: Int): Boolean
    suspend fun uploadBranchLogo(businessId: Int, branchCode: String, logo: SharedFile): Boolean
    suspend fun deleteBranchLogo(businessId: Int, branchCode: String): Boolean
}
