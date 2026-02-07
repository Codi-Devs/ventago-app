package com.teco.ventago.features.expenses.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.expenses.data.provider.IExpensesProvider
import com.teco.ventago.features.expenses.domain.models.CrawlJob
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpensePayment
import com.teco.ventago.features.expenses.domain.models.PagedCrawlJobs
import com.teco.ventago.features.expenses.domain.models.PagedExpenses
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseProofFile
import com.teco.ventago.features.expenses.domain.models.requests.ListExpensesRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpensePaymentRequest
import com.teco.ventago.json
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class ExpensesRepository(
    private val provider: IExpensesProvider,
    private val logger: ILoggerService
) : IExpensesRepository {

    override suspend fun listExpenses(businessId: Int, request: ListExpensesRequest): PagedExpenses {
        return try {
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
            PagedExpenses(expenses = expenses, total = total, page = page, size = size)
        } catch (e: Exception) {
            logAndThrow(
                flow = "listExpenses",
                context = "Error listing expenses. businessId: $businessId, page: ${request.page}, pageSize: ${request.pageSize}",
                error = e
            )
        }
    }

    override suspend fun getExpense(businessId: Int, expenseId: Long): Expense {
        return try {
            val response = provider.getExpense(businessId, expenseId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
            json.decodeFromJsonElement(Expense.serializer(), dataObj)
        } catch (e: Exception) {
            logAndThrow(
                flow = "getExpense",
                context = "Error getting expense. businessId: $businessId, expenseId: $expenseId",
                error = e
            )
        }
    }

    override suspend fun createExpense(businessId: Int, payload: String): Expense {
        return try {
            val response = provider.createExpense(businessId, payload)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
            json.decodeFromJsonElement(Expense.serializer(), dataObj)
        } catch (e: Exception) {
            logAndThrow(
                flow = "createExpense",
                context = "Error creating expense. businessId: $businessId",
                error = e
            )
        }
    }

    override suspend fun updateExpense(businessId: Int, expenseId: Long, payload: String): Expense {
        return try {
            val response = provider.updateExpense(businessId, expenseId, payload)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
            json.decodeFromJsonElement(Expense.serializer(), dataObj)
        } catch (e: Exception) {
            logAndThrow(
                flow = "updateExpense",
                context = "Error updating expense. businessId: $businessId, expenseId: $expenseId",
                error = e
            )
        }
    }

    override suspend fun deleteExpense(businessId: Int, expenseId: Long): Boolean {
        return try {
            val response = provider.deleteExpense(businessId, expenseId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            response.successful
        } catch (e: Exception) {
            logAndThrow(
                flow = "deleteExpense",
                context = "Error deleting expense. businessId: $businessId, expenseId: $expenseId",
                error = e
            )
        }
    }

    // Payments

    override suspend fun createPayment(
        businessId: Int,
        expenseId: Long,
        request: UpsertExpensePaymentRequest,
        proofFile: ExpenseProofFile?
    ): ExpensePayment {
        return try {
            val response = provider.createPayment(businessId, expenseId, request, proofFile)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            val dataObj = response.data as? JsonObject
            dataObj?.let(::mapExpensePayment) ?: ExpensePayment(expenseId = expenseId)
        } catch (e: Exception) {
            logAndThrow(
                flow = "createPayment",
                context = "Error creating payment. businessId: $businessId, expenseId: $expenseId",
                error = e
            )
        }
    }

    override suspend fun listPayments(businessId: Int, expenseId: Long): List<ExpensePayment> {
        return try {
            val response = provider.listPayments(businessId, expenseId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            val dataArray = response.data as? JsonArray ?: return emptyList()
            dataArray.mapNotNull { (it as? JsonObject)?.let(::mapExpensePayment) }
        } catch (e: Exception) {
            logAndThrow(
                flow = "listPayments",
                context = "Error listing payments. businessId: $businessId, expenseId: $expenseId",
                error = e
            )
        }
    }

    override suspend fun updatePayment(
        businessId: Int,
        expenseId: Long,
        paymentId: Long,
        request: UpsertExpensePaymentRequest,
        proofFile: ExpenseProofFile?
    ): ExpensePayment {
        return try {
            val response = provider.updatePayment(businessId, expenseId, paymentId, request, proofFile)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            val dataObj = response.data as? JsonObject
            dataObj?.let(::mapExpensePayment) ?: ExpensePayment(id = paymentId, expenseId = expenseId)
        } catch (e: Exception) {
            logAndThrow(
                flow = "updatePayment",
                context = "Error updating payment. businessId: $businessId, expenseId: $expenseId, paymentId: $paymentId",
                error = e
            )
        }
    }

    override suspend fun deletePayment(businessId: Int, expenseId: Long, paymentId: Long): Boolean {
        return try {
            val response = provider.deletePayment(businessId, expenseId, paymentId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            response.successful
        } catch (e: Exception) {
            logAndThrow(
                flow = "deletePayment",
                context = "Error deleting payment. businessId: $businessId, expenseId: $expenseId, paymentId: $paymentId",
                error = e
            )
        }
    }

    // Crawl jobs

    override suspend fun crawlExpense(businessId: Int, payload: String): CrawlJob {
        return try {
            val response = provider.crawlExpense(businessId, payload)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
            json.decodeFromJsonElement(CrawlJob.serializer(), dataObj)
        } catch (e: Exception) {
            logAndThrow(
                flow = "crawlExpense",
                context = "Error creating crawl expense job. businessId: $businessId",
                error = e
            )
        }
    }

    override suspend fun getCrawlJobStatus(businessId: Int, jobId: Long): CrawlJob {
        return try {
            val response = provider.getCrawlJobStatus(businessId, jobId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
            json.decodeFromJsonElement(CrawlJob.serializer(), dataObj)
        } catch (e: Exception) {
            logAndThrow(
                flow = "getCrawlJobStatus",
                context = "Error getting crawl job status. businessId: $businessId, jobId: $jobId",
                error = e
            )
        }
    }

    override suspend fun listCrawlJobs(businessId: Int, page: Int, pageSize: Int): PagedCrawlJobs {
        return try {
            val response = provider.listCrawlJobs(businessId, page, pageSize)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            val dataObj = response.data?.jsonObject ?: return PagedCrawlJobs()
            json.decodeFromJsonElement(PagedCrawlJobs.serializer(), dataObj)
        } catch (e: Exception) {
            logAndThrow(
                flow = "listCrawlJobs",
                context = "Error listing crawl jobs. businessId: $businessId, page: $page, pageSize: $pageSize",
                error = e
            )
        }
    }

    private fun logAndThrow(flow: String, context: String, error: Exception): Nothing {
        logger.sendLog(
            Log(
                level = LogLevel.ERROR,
                flow = flow,
                message = "$context. Error: ${error.message ?: "UNKNOWN"}"
            )
        )
        throw error
    }

    private fun mapExpensePayment(dataObj: JsonObject): ExpensePayment {
        return ExpensePayment(
            id = dataObj.longValue("id"),
            expenseId = dataObj.longValue("expense_id"),
            paymentMethod = dataObj.stringValue("payment_method"),
            paymentStatus = dataObj.stringValue("payment_status"),
            amountPaid = dataObj.doubleValue("amount_paid"),
            reference = dataObj.stringValue("reference"),
            proofFileUrl = dataObj.stringValue("proof_file_url"),
            proofFileName = dataObj.stringValue("proof_file_name"),
            notes = dataObj.stringValue("notes"),
            paymentDate = dataObj.stringValue("payment_date"),
            dueDate = dataObj.stringValue("due_date"),
            isOverdue = dataObj.booleanValue("is_overdue"),
            daysOverdue = dataObj.intValue("days_overdue"),
            createdAt = dataObj.stringValue("created_at"),
            updatedAt = dataObj.stringValue("updated_at")
        )
    }

    private fun JsonObject.stringValue(key: String): String? {
        return (this[key] as? JsonPrimitive)?.contentOrNull
    }

    private fun JsonObject.longValue(key: String): Long? {
        val primitive = this[key] as? JsonPrimitive ?: return null
        return primitive.longOrNull ?: primitive.contentOrNull?.toLongOrNull()
    }

    private fun JsonObject.intValue(key: String): Int? {
        return longValue(key)?.toInt()
    }

    private fun JsonObject.doubleValue(key: String): Double? {
        val primitive = this[key] as? JsonPrimitive ?: return null
        return primitive.doubleOrNull ?: primitive.contentOrNull?.toDoubleOrNull()
    }

    private fun JsonObject.booleanValue(key: String): Boolean? {
        return stringValue(key)?.lowercase()?.let {
            when (it) {
                "true" -> true
                "false" -> false
                else -> null
            }
        }
    }
}
