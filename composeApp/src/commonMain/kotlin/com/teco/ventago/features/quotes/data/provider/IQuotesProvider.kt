package com.teco.ventago.features.quotes.data.provider

import com.teco.ventago.features.quotes.domain.models.requests.CancelQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.CreateQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.GetQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.ListQuotesRequest
import com.teco.ventago.features.quotes.domain.models.requests.SendQuoteEmailRequest
import com.teco.ventago.features.quotes.domain.models.requests.UpdateQuoteRequest
import com.teco.ventago.features.quotes.domain.models.QuoteSettings
import com.teco.ventago.utils.ApiResponse

interface IQuotesProvider {
    suspend fun listQuotes(businessId: Int, request: ListQuotesRequest): ApiResponse
    suspend fun getQuote(businessId: Int, request: GetQuoteRequest): ApiResponse
    suspend fun getQuotePdf(businessId: Int, quoteId: Long): ApiResponse
    suspend fun sendQuoteEmail(businessId: Int, request: SendQuoteEmailRequest): ApiResponse
    suspend fun cancelQuote(businessId: Int, request: CancelQuoteRequest): ApiResponse
    suspend fun createQuote(businessId: Int, request: CreateQuoteRequest): ApiResponse
    suspend fun updateQuote(businessId: Int, request: UpdateQuoteRequest): ApiResponse
    suspend fun getQuoteSettings(businessId: Int): ApiResponse
    suspend fun updateQuoteSettings(businessId: Int, request: QuoteSettings): ApiResponse
}
