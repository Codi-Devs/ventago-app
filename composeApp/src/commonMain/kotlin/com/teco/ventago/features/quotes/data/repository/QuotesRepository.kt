package com.teco.ventago.features.quotes.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.quotes.data.provider.IQuotesProvider
import com.teco.ventago.features.quotes.domain.models.PagedQuotes
import com.teco.ventago.features.quotes.domain.models.Quote
import com.teco.ventago.features.quotes.domain.models.QuoteSettings
import com.teco.ventago.features.quotes.domain.models.requests.CancelQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.CreateQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.GetQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.ListQuotesRequest
import com.teco.ventago.features.quotes.domain.models.requests.SendQuoteEmailRequest
import com.teco.ventago.features.quotes.domain.models.requests.UpdateQuoteRequest
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import com.teco.ventago.json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class QuotesRepository(
    private val provider: IQuotesProvider,
    private val logger: ILoggerService
) : IQuotesRepository {

    override suspend fun listQuotes(businessId: Int, request: ListQuotesRequest): PagedQuotes {
        val response = provider.listQuotes(businessId, request)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataObj = response.data?.jsonObject ?: JsonObject(emptyMap())
        val itemsArray = dataObj["items"]?.jsonArray
        val quotes = itemsArray?.map { json.decodeFromJsonElement<Quote>(it) } ?: emptyList()
        val total = dataObj["total"]?.jsonPrimitive?.longOrNull
        val size = dataObj["size"]?.jsonPrimitive?.longOrNull?.toInt()
        val page = dataObj["page"]?.jsonPrimitive?.longOrNull?.toInt()
        return PagedQuotes(items = quotes, total = total, size = size, page = page)
    }

    override suspend fun getQuote(businessId: Int, request: GetQuoteRequest): Quote {
        val response = provider.getQuote(businessId, request)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
        return json.decodeFromJsonElement(Quote.serializer(), dataObj)
    }

    override suspend fun getQuotePdf(businessId: Int, quoteId: Long): String {
        val response = provider.getQuotePdf(businessId, quoteId)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
        return dataObj["pdf_base64"]?.jsonPrimitive?.content
            ?: throw BadRequestException("Missing pdf_base64")
    }

    override suspend fun sendQuoteEmail(businessId: Int, request: SendQuoteEmailRequest): Boolean {
        val response = provider.sendQuoteEmail(businessId, request)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        return response.successful
    }

    override suspend fun cancelQuote(businessId: Int, request: CancelQuoteRequest): Boolean {
        val response = provider.cancelQuote(businessId, request)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        return response.successful
    }

    override suspend fun createQuote(businessId: Int, request: CreateQuoteRequest): Quote {
        val response = provider.createQuote(businessId, request)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
        return json.decodeFromJsonElement(Quote.serializer(), dataObj)
    }

    override suspend fun updateQuote(businessId: Int, request: UpdateQuoteRequest): Quote {
        val response = provider.updateQuote(businessId, request)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
        return json.decodeFromJsonElement(Quote.serializer(), dataObj)
    }

    override suspend fun getQuoteSettings(businessId: Int): QuoteSettings {
        val response = provider.getQuoteSettings(businessId)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
        return json.decodeFromJsonElement(QuoteSettings.serializer(), dataObj)
    }

    override suspend fun updateQuoteSettings(businessId: Int, request: QuoteSettings): Boolean {
        val response = provider.updateQuoteSettings(businessId, request)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        return response.successful
    }
}
