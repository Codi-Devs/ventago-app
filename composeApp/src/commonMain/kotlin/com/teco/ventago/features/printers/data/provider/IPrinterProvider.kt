package com.teco.ventago.features.printers.data.provider

import com.teco.ventago.features.printers.domain.model.CreatePrinterRequest
import com.teco.ventago.features.printers.domain.model.PrinterListFilters
import com.teco.ventago.features.printers.domain.model.UpdatePrinterRequest
import com.teco.ventago.utils.ApiResponse

interface IPrinterProvider {
    suspend fun createPrinter(businessId: Int, request: CreatePrinterRequest): ApiResponse
    suspend fun listPrinters(businessId: Int, filters: PrinterListFilters = PrinterListFilters()): ApiResponse
    suspend fun getPrinter(
        businessId: Int,
        branchCode: String,
        billingPointCode: String,
    ): ApiResponse

    suspend fun updatePrinter(
        businessId: Int,
        branchCode: String,
        billingPointCode: String,
        request: UpdatePrinterRequest,
    ): ApiResponse

    suspend fun deletePrinter(
        businessId: Int,
        branchCode: String,
        billingPointCode: String,
    ): ApiResponse

    suspend fun getOrderTicketLayout(businessId: Int, orderId: Int): ApiResponse
}

