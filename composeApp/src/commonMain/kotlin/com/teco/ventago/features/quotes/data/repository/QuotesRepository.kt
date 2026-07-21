package com.teco.ventago.features.quotes.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.quotes.data.provider.IQuotesProvider
import com.teco.ventago.features.quotes.domain.models.PagedQuotes
import com.teco.ventago.features.quotes.domain.models.OtiTax
import com.teco.ventago.features.quotes.domain.models.Quote
import com.teco.ventago.features.quotes.domain.models.QuoteLine
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.contentOrNull
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
        val quotes = itemsArray?.map { item ->
            val obj = item.jsonObject
            val quote = json.decodeFromJsonElement<Quote>(item)
            val quoteWithCustomer = mergeNestedCustomer(quote, obj)
            if (quoteWithCustomer.totals != null) {
                quoteWithCustomer
            } else {
                val totals = buildTotalsFromFlatFields(obj)
                if (totals != null) quoteWithCustomer.copy(totals = totals) else quoteWithCustomer
            }
        } ?: emptyList()
        val total = dataObj["total"]?.jsonPrimitive?.longOrNull
        val size = dataObj["size"]?.jsonPrimitive?.longOrNull?.toInt()
        val page = dataObj["page"]?.jsonPrimitive?.longOrNull?.toInt()
        return PagedQuotes(items = quotes, total = total, size = size, page = page)
    }

    private fun buildTotalsFromFlatFields(obj: JsonObject): com.teco.ventago.features.quotes.domain.models.QuoteTotals? {
        val subtotal = obj["subtotal"].toDoubleOrNull()
        val discount = obj["discount_total"].toDoubleOrNull()
        val taxes = obj["tax_total"].toDoubleOrNull()
        val total = obj["total_amount"].toDoubleOrNull()
        val hasTotals = listOf(subtotal, discount, taxes, total).any { it != null }
        return if (hasTotals) {
            com.teco.ventago.features.quotes.domain.models.QuoteTotals(
                subtotal = subtotal,
                discount = discount,
                taxes = taxes,
                total = total
            )
        } else {
            null
        }
    }

    private fun kotlinx.serialization.json.JsonElement?.toDoubleOrNull(): Double? {
        val primitive = this as? JsonPrimitive ?: return null
        return if (primitive.isString) {
            primitive.content.toDoubleOrNull()
        } else {
            primitive.doubleOrNull
        }
    }

    private fun JsonElement?.toLongOrNull(): Long? {
        val primitive = this as? JsonPrimitive ?: return null
        return if (primitive.isString) {
            primitive.content.toLongOrNull()
        } else {
            primitive.longOrNull
        }
    }

    private fun JsonElement?.toIntOrNull(): Int? = toLongOrNull()?.toInt()

    private fun buildQuoteLines(element: JsonElement?): List<QuoteLine>? {
        val items = element?.jsonArray ?: return null
        return items.map { item ->
            val obj = item.jsonObject
            QuoteLine(
                itemId = obj["item_id"].toLongOrNull(),
                itemName = obj["item_name"]?.jsonPrimitive?.content,
                quantity = obj["quantity"].toDoubleOrNull(),
                unitPrice = obj["unit_price"].toDoubleOrNull(),
                discountMode = obj["discount_mode"].toIntOrNull(),
                discountValue = obj["discount_value"].toDoubleOrNull(),
                taxName = obj["tax_name"]?.jsonPrimitive?.content,
                taxRate = obj["tax_rate"]?.jsonPrimitive?.content,
                iscRate = obj["isc_rate"].toDoubleOrNull(),
                otiTaxes = obj["oti_taxes"]?.jsonArray?.mapNotNull { taxElement ->
                    val taxObj = taxElement.jsonObject
                    val code = taxObj["code"]?.jsonPrimitive?.content
                    val rate = taxObj["rate"].toDoubleOrNull()
                    if (code == null && rate == null) {
                        null
                    } else {
                        OtiTax(code = code, rate = rate)
                    }
                },
                lineTotal = obj["line_total"].toDoubleOrNull(),
                productType = obj["product_type"]?.jsonPrimitive?.content
            )
        }
    }

    override suspend fun getQuote(businessId: Int, request: GetQuoteRequest): Quote {
        val response = provider.getQuote(businessId, request)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
        val sanitized = JsonObject(dataObj.filterKeys { key -> key != "lines" })
        val baseQuote = json.decodeFromJsonElement(Quote.serializer(), sanitized)
        val totals = baseQuote.totals ?: buildTotalsFromFlatFields(dataObj)
        val lines = buildQuoteLines(dataObj["lines"])
        val quoteWithCustomer = mergeNestedCustomer(baseQuote, dataObj)

        return quoteWithCustomer.copy(
            totals = totals,
            lines = lines ?: quoteWithCustomer.lines
        )
    }

    private fun mergeNestedCustomer(quote: Quote, obj: JsonObject): Quote {
        val customerObj = obj["customer"]?.jsonObject
        if (customerObj == null) return quote

        val mergedCustomerName = quote.customerName
            ?.takeIf { it.isNotBlank() }
            ?: customerObj["name"]?.jsonPrimitive?.contentOrNull
        val mergedCustomerEmail = quote.customerEmail
            ?.takeIf { it.isNotBlank() }
            ?: customerObj["email"]?.jsonPrimitive?.contentOrNull
        val mergedCustomerPhone = quote.customerPhone
            ?.takeIf { it.isNotBlank() }
            ?: customerObj["phone"]?.jsonPrimitive?.contentOrNull
        val mergedCustomerRuc = quote.customerRuc
            ?.takeIf { it.isNotBlank() }
            ?: customerObj["ruc"]?.jsonPrimitive?.contentOrNull
        val mergedCustomerId = quote.customerId ?: customerObj["id"].toLongOrNull()

        return quote.copy(
            customerId = mergedCustomerId,
            customerName = mergedCustomerName,
            customerEmail = mergedCustomerEmail,
            customerPhone = mergedCustomerPhone,
            customerRuc = mergedCustomerRuc
        )
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
