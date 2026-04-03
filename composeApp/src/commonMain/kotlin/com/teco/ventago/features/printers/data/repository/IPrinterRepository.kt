package com.teco.ventago.features.printers.data.repository

import com.teco.ventago.features.printers.domain.model.CreatePrinterRequest
import com.teco.ventago.features.printers.domain.model.PrinterConfig
import com.teco.ventago.features.printers.domain.model.PrinterListFilters
import com.teco.ventago.features.printers.domain.model.TicketDocumentPayload
import com.teco.ventago.features.printers.domain.model.UpdatePrinterRequest

interface IPrinterRepository {
    suspend fun createPrinter(businessId: Int, request: CreatePrinterRequest): PrinterConfig
    suspend fun listPrinters(businessId: Int, filters: PrinterListFilters = PrinterListFilters()): List<PrinterConfig>
    suspend fun getPrinter(businessId: Int, branchCode: String, billingPointCode: String): PrinterConfig
    suspend fun updatePrinter(
        businessId: Int,
        branchCode: String,
        billingPointCode: String,
        request: UpdatePrinterRequest,
    ): PrinterConfig

    suspend fun deletePrinter(businessId: Int, branchCode: String, billingPointCode: String): Boolean
    suspend fun getOrderTicketLayout(businessId: Int, orderId: Int): TicketDocumentPayload
}

