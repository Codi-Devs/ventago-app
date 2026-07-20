package com.teco.ventago.features.printers

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.features.printers.data.provider.IPrinterProvider
import com.teco.ventago.features.printers.data.repository.PrinterRepository
import com.teco.ventago.features.printers.domain.model.CreatePrinterRequest
import com.teco.ventago.features.printers.domain.model.PrinterListFilters
import com.teco.ventago.features.printers.domain.model.UpdatePrinterRequest
import com.teco.ventago.utils.ApiResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking

class PrinterRepositoryTest {

    @Test
    fun cancellationIsRethrownWithoutErrorLog() = runBlocking {
        val logger = RecordingLogger()
        val repository = PrinterRepository(
            provider = CancellingPrinterProvider(),
            logger = logger,
        )

        assertFailsWith<CancellationException> {
            repository.getOrderTicketLayout(businessId = 4, orderId = 800)
        }
        assertEquals(0, logger.logs.size)
    }

    private class CancellingPrinterProvider : IPrinterProvider {
        override suspend fun createPrinter(businessId: Int, request: CreatePrinterRequest): ApiResponse {
            throw CancellationException("cancelled")
        }

        override suspend fun listPrinters(businessId: Int, filters: PrinterListFilters): ApiResponse {
            throw CancellationException("cancelled")
        }

        override suspend fun getPrinter(
            businessId: Int,
            branchCode: String,
            billingPointCode: String,
        ): ApiResponse {
            throw CancellationException("cancelled")
        }

        override suspend fun updatePrinter(
            businessId: Int,
            branchCode: String,
            billingPointCode: String,
            request: UpdatePrinterRequest,
        ): ApiResponse {
            throw CancellationException("cancelled")
        }

        override suspend fun deletePrinter(
            businessId: Int,
            branchCode: String,
            billingPointCode: String,
        ): ApiResponse {
            throw CancellationException("cancelled")
        }

        override suspend fun getOrderTicketLayout(businessId: Int, orderId: Int): ApiResponse {
            throw CancellationException("cancelled")
        }
    }

    private class RecordingLogger : ILoggerService {
        val logs = mutableListOf<Log>()

        override fun sendLog(log: Log) {
            logs += log
        }
    }
}
