package com.teco.ventago.features.quotes.data.repository

import com.teco.ventago.features.quotes.domain.models.PagedQuotes
import com.teco.ventago.features.quotes.domain.models.Quote
import com.teco.ventago.features.quotes.domain.models.QuoteSettings
import com.teco.ventago.features.quotes.domain.models.requests.CancelQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.CreateQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.GetQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.ListQuotesRequest
import com.teco.ventago.features.quotes.domain.models.requests.SendQuoteEmailRequest
import com.teco.ventago.features.quotes.domain.models.requests.UpdateQuoteRequest

interface IQuotesRepository {
    suspend fun listQuotes(businessId: Int, request: ListQuotesRequest): PagedQuotes
    suspend fun getQuote(businessId: Int, request: GetQuoteRequest): Quote
    suspend fun getQuotePdf(businessId: Int, quoteId: Long): String
    suspend fun sendQuoteEmail(businessId: Int, request: SendQuoteEmailRequest): Boolean
    suspend fun cancelQuote(businessId: Int, request: CancelQuoteRequest): Boolean
    suspend fun createQuote(businessId: Int, request: CreateQuoteRequest): Quote
    suspend fun updateQuote(businessId: Int, request: UpdateQuoteRequest): Quote
    suspend fun getQuoteSettings(businessId: Int): QuoteSettings
    suspend fun updateQuoteSettings(businessId: Int, request: QuoteSettings): Boolean
}
