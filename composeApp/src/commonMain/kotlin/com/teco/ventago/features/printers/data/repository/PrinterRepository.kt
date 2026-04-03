package com.teco.ventago.features.printers.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.printers.data.provider.IPrinterProvider
import com.teco.ventago.features.printers.domain.model.CreatePrinterRequest
import com.teco.ventago.features.printers.domain.model.PrinterConfig
import com.teco.ventago.features.printers.domain.model.PrinterListFilters
import com.teco.ventago.features.printers.domain.model.TicketDocumentPayload
import com.teco.ventago.features.printers.domain.model.TicketUnavailableException
import com.teco.ventago.features.printers.domain.model.UpdatePrinterRequest
import com.teco.ventago.json
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class PrinterRepository(
    private val provider: IPrinterProvider,
    private val logger: ILoggerService,
) : IPrinterRepository {
    override suspend fun createPrinter(businessId: Int, request: CreatePrinterRequest): PrinterConfig {
        return runRepositoryCall("createPrinter", businessId) {
            val response = provider.createPrinter(businessId, request)
            if (response.error.isError()) throw BadRequestException(response.toJson())
            json.decodeFromJsonElement(response.data ?: throw BadRequestException(response.toJson()))
        }
    }

    override suspend fun listPrinters(businessId: Int, filters: PrinterListFilters): List<PrinterConfig> {
        return runRepositoryCall("listPrinters", businessId) {
            val response = provider.listPrinters(businessId, filters)
            if (response.error.isError()) throw BadRequestException(response.toJson())
            when (val data = response.data) {
                is JsonArray -> data.map { json.decodeFromJsonElement<PrinterConfig>(it) }
                is JsonObject -> {
                    val items = data["items"] ?: data["printers"] ?: throw BadRequestException(response.toJson())
                    json.decodeFromJsonElement(items)
                }
                else -> throw BadRequestException(response.toJson())
            }
        }
    }

    override suspend fun getPrinter(businessId: Int, branchCode: String, billingPointCode: String): PrinterConfig {
        return runRepositoryCall("getPrinter", businessId) {
            val response = provider.getPrinter(businessId, branchCode, billingPointCode)
            if (response.error.isError()) throw BadRequestException(response.toJson())
            json.decodeFromJsonElement(response.data ?: throw BadRequestException(response.toJson()))
        }
    }

    override suspend fun updatePrinter(
        businessId: Int,
        branchCode: String,
        billingPointCode: String,
        request: UpdatePrinterRequest,
    ): PrinterConfig {
        return runRepositoryCall("updatePrinter", businessId) {
            val response = provider.updatePrinter(businessId, branchCode, billingPointCode, request)
            if (response.error.isError()) throw BadRequestException(response.toJson())
            json.decodeFromJsonElement(response.data ?: throw BadRequestException(response.toJson()))
        }
    }

    override suspend fun deletePrinter(businessId: Int, branchCode: String, billingPointCode: String): Boolean {
        return runRepositoryCall("deletePrinter", businessId) {
            val response = provider.deletePrinter(businessId, branchCode, billingPointCode)
            if (response.error.isError()) throw BadRequestException(response.toJson())
            response.successful
        }
    }

    override suspend fun getOrderTicketLayout(businessId: Int, orderId: Int): TicketDocumentPayload {
        return runRepositoryCall("getOrderTicketLayout", businessId) {
            val response = provider.getOrderTicketLayout(businessId, orderId)
            if (response.errorCode == "INV_002") {
                throw TicketUnavailableException(response.errorMessage ?: "El ticket no está disponible para este pedido")
            }
            if (response.error.isError()) throw BadRequestException(response.toJson())
            json.decodeFromJsonElement(response.data ?: throw BadRequestException(response.toJson()))
        }
    }

    private suspend fun <T> runRepositoryCall(
        flow: String,
        businessId: Int,
        block: suspend () -> T,
    ): T {
        return try {
            block()
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    flow,
                    "Printer repository error. businessId=$businessId error=${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }
}

