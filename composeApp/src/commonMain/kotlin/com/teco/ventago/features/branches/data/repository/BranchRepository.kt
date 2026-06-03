package com.teco.ventago.features.branches.data.repository

import com.teco.ventago.core.file.SharedFile
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.branches.data.provider.IBranchProvider
import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean


class BranchRepository(private val provider: IBranchProvider, private val logger: ILoggerService) :
    IBranchRepository {


    override suspend fun getBranches(businessId: Int): List<Branch> {
        try {
            val response = provider.getBranches(businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            val branches = mutableListOf<Branch>()
            if (response.data is JsonArray) {
                response.data.mapNotNull { item ->
                    if (item is JsonObject) {
                        branches.add(Branch(item))
                    } else {
                        logger.sendLog(
                            Log(
                                LogLevel.ERROR,
                                "BranchRepository::getBranches",
                                "Invalid branch item format: $item"
                            )
                        )
                        null
                    }
                }
            } else {
                throw BadRequestException(response.toJson())
            }

            return branches
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "BranchRepository::getBranches",
                    "Error getting branches. Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }

    }

    override suspend fun addBillingPoint(
        businessId: Int, branchCode: String, name: String, code: String, status: Int
    ): FiscalBillingPoint {
        return try {
            val response = provider.addBillingPoint(businessId, branchCode, name, code, status)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.successful) {
                FiscalBillingPoint(
                    billingPoint = code,
                    description = name,
                    status = status,
                )
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "BranchRepository::addBillingPoint",
                    "Error adding Billing point. Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }

    override suspend fun updateBillingPoint(
        businessId: Int, branchCode: String, billingPoint: String, name: String, status: Int
    ): Boolean {
        return try {
            val response = provider.updateBillingPoint(businessId, branchCode, billingPoint, name, status)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonPrimitive) {
                response.data.boolean
            } else {
                false
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "BranchRepository::updateBillingPoint",
                    "Error updating Billing point. Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }

    override suspend fun uploadBranchLogo(
        businessId: Int,
        branchCode: String,
        logo: SharedFile,
    ): Boolean {
        return try {
            val response = provider.uploadBranchLogo(businessId, branchCode, logo)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            response.successful
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "BranchRepository::uploadBranchLogo",
                    "Error uploading branch logo. businessId=$businessId branchCode=$branchCode Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }

    override suspend fun deleteBranchLogo(businessId: Int, branchCode: String): Boolean {
        return try {
            val response = provider.deleteBranchLogo(businessId, branchCode)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            response.successful
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "BranchRepository::deleteBranchLogo",
                    "Error deleting branch logo. businessId=$businessId branchCode=$branchCode Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }
}
