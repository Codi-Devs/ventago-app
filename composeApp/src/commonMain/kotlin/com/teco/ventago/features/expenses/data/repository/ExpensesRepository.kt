package com.teco.ventago.features.expenses.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.features.expenses.data.provider.IExpensesProvider
import com.teco.ventago.features.expenses.domain.models.CrawlJob
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpensePayment
import com.teco.ventago.features.expenses.domain.models.PagedCrawlJobs
import com.teco.ventago.features.expenses.domain.models.PagedExpenses
import com.teco.ventago.features.expenses.domain.models.requests.ListExpensesRequest
import com.teco.ventago.json
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class ExpensesRepository(
    private val provider: IExpensesProvider,
    private val logger: ILoggerService
) : IExpensesRepository {

    override suspend fun listExpenses(businessId: Int, request: ListExpensesRequest): PagedExpenses {
        val response = provider.listExpenses(businessId, request)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataObj = response.data?.jsonObject ?: JsonObject(emptyMap())
        val expensesArray = dataObj["expenses"]?.jsonArray
        val expenses = expensesArray?.map { item ->
            json.decodeFromJsonElement<Expense>(item)
        } ?: emptyList()
        val total = dataObj["total"]?.jsonPrimitive?.longOrNull
        val size = dataObj["size"]?.jsonPrimitive?.longOrNull?.toInt()
        val page = dataObj["page"]?.jsonPrimitive?.longOrNull?.toInt()
        return PagedExpenses(expenses = expenses, total = total, page = page, size = size)
    }

    override suspend fun getExpense(businessId: Int, expenseId: Long): Expense {
        val response = provider.getExpense(businessId, expenseId)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
        return json.decodeFromJsonElement(Expense.serializer(), dataObj)
    }

    override suspend fun createExpense(businessId: Int, payload: String): Expense {
        val response = provider.createExpense(businessId, payload)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
        return json.decodeFromJsonElement(Expense.serializer(), dataObj)
    }

    override suspend fun updateExpense(businessId: Int, expenseId: Long, payload: String): Expense {
        val response = provider.updateExpense(businessId, expenseId, payload)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
        return json.decodeFromJsonElement(Expense.serializer(), dataObj)
    }

    override suspend fun deleteExpense(businessId: Int, expenseId: Long): Boolean {
        val response = provider.deleteExpense(businessId, expenseId)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        return response.successful
    }

    // Payments

    override suspend fun createPayment(businessId: Int, expenseId: Long, payload: String): ExpensePayment {
        val response = provider.createPayment(businessId, expenseId, payload)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
        return json.decodeFromJsonElement(ExpensePayment.serializer(), dataObj)
    }

    override suspend fun listPayments(businessId: Int, expenseId: Long): List<ExpensePayment> {
        val response = provider.listPayments(businessId, expenseId)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataArray = response.data?.jsonArray ?: return emptyList()
        return dataArray.map { json.decodeFromJsonElement(ExpensePayment.serializer(), it) }
    }

    override suspend fun updatePayment(businessId: Int, expenseId: Long, paymentId: Long, payload: String): ExpensePayment {
        val response = provider.updatePayment(businessId, expenseId, paymentId, payload)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
        return json.decodeFromJsonElement(ExpensePayment.serializer(), dataObj)
    }

    override suspend fun deletePayment(businessId: Int, expenseId: Long, paymentId: Long): Boolean {
        val response = provider.deletePayment(businessId, expenseId, paymentId)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        return response.successful
    }

    // Crawl jobs

    override suspend fun crawlExpense(businessId: Int, payload: String): CrawlJob {
        val response = provider.crawlExpense(businessId, payload)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
        return json.decodeFromJsonElement(CrawlJob.serializer(), dataObj)
    }

    override suspend fun getCrawlJobStatus(businessId: Int, jobId: Long): CrawlJob {
        val response = provider.getCrawlJobStatus(businessId, jobId)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
        return json.decodeFromJsonElement(CrawlJob.serializer(), dataObj)
    }

    override suspend fun listCrawlJobs(businessId: Int, page: Int, pageSize: Int): PagedCrawlJobs {
        val response = provider.listCrawlJobs(businessId, page, pageSize)
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataObj = response.data?.jsonObject ?: return PagedCrawlJobs()
        return json.decodeFromJsonElement(PagedCrawlJobs.serializer(), dataObj)
    }
}
