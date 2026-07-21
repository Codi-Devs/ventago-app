package com.teco.ventago.features.quotes

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.features.quotes.data.provider.IQuotesProvider
import com.teco.ventago.features.quotes.data.repository.QuotesRepository
import com.teco.ventago.features.quotes.domain.models.QuoteSettings
import com.teco.ventago.features.quotes.domain.models.requests.CancelQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.CreateQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.GetQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.ListQuotesRequest
import com.teco.ventago.features.quotes.domain.models.requests.SendQuoteEmailRequest
import com.teco.ventago.features.quotes.domain.models.requests.UpdateQuoteRequest
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class QuotesRepositoryTest {

    @Test
    fun `listQuotes maps nested customer name from backend response`() = runTest {
        val responseBody = Json.parseToJsonElement(
            """
            {
              "total": 30,
              "page": 1,
              "size": 10,
              "items": [
                {
                  "id": 388,
                  "quote_number": "TEC-0000-2026-000031",
                  "display_number": "TEC-0000-2026-000031",
                  "branch_code": "0000",
                  "customer": {
                    "name": "Empresa de prueba",
                    "email": "ventas@example.com",
                    "phone": "6000-0000"
                  },
                  "status": 1,
                  "subtotal": "500",
                  "discount_total": "0",
                  "tax_total": "35",
                  "total_amount": "535",
                  "created_at": "2026-06-30T21:35:13.275141Z"
                }
              ]
            }
            """.trimIndent()
        ).jsonObject
        val repository = QuotesRepository(
            provider = FakeQuotesProvider(ApiResponse(true, responseBody, ApiError.NO_ERROR)),
            logger = NoopLogger
        )

        val result = repository.listQuotes(4, ListQuotesRequest())
        val quote = result.items.first()

        assertEquals("Empresa de prueba", quote.customerName)
        assertEquals("ventas@example.com", quote.customerEmail)
        assertEquals("6000-0000", quote.customerPhone)
        assertEquals(535.0, quote.totals?.total)
    }

    private class FakeQuotesProvider(
        private val listResponse: ApiResponse,
    ) : IQuotesProvider {
        override suspend fun listQuotes(businessId: Int, request: ListQuotesRequest): ApiResponse = listResponse
        override suspend fun getQuote(businessId: Int, request: GetQuoteRequest): ApiResponse = unsupported()
        override suspend fun getQuotePdf(businessId: Int, quoteId: Long): ApiResponse = unsupported()
        override suspend fun sendQuoteEmail(businessId: Int, request: SendQuoteEmailRequest): ApiResponse = unsupported()
        override suspend fun cancelQuote(businessId: Int, request: CancelQuoteRequest): ApiResponse = unsupported()
        override suspend fun createQuote(businessId: Int, request: CreateQuoteRequest): ApiResponse = unsupported()
        override suspend fun updateQuote(businessId: Int, request: UpdateQuoteRequest): ApiResponse = unsupported()
        override suspend fun getQuoteSettings(businessId: Int): ApiResponse = unsupported()
        override suspend fun updateQuoteSettings(businessId: Int, request: QuoteSettings): ApiResponse = unsupported()

        private fun unsupported(): ApiResponse {
            error("Not used in this test")
        }
    }

    private object NoopLogger : ILoggerService {
        override fun sendLog(log: Log) = Unit
    }
}
