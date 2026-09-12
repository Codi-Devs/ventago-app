package com.teco.ventago.features.expenses.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.expenses.data.provider.IExpensesProvider
import com.teco.ventago.features.expenses.domain.ExpensesErrorMapper
import com.teco.ventago.features.expenses.domain.models.CrawlJob
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpenseAccount
import com.teco.ventago.features.expenses.domain.models.ExpenseItem
import com.teco.ventago.features.expenses.domain.models.ExpenseParty
import com.teco.ventago.features.expenses.domain.models.ExpensePayment
import com.teco.ventago.features.expenses.domain.models.ExpensePaymentDeleteResult
import com.teco.ventago.features.expenses.domain.models.ExpensePaymentMutationResult
import com.teco.ventago.features.expenses.domain.models.ExpensePaymentSnapshot
import com.teco.ventago.features.expenses.domain.models.PagedCrawlJobs
import com.teco.ventago.features.expenses.domain.models.ExpenseMerchant
import com.teco.ventago.features.expenses.domain.models.PagedExpenses
import com.teco.ventago.features.expenses.domain.models.PagedMerchants
import com.teco.ventago.features.expenses.domain.models.PaymentSummary
import com.teco.ventago.features.expenses.domain.models.requests.CategorizeExpenseRequest
import com.teco.ventago.features.expenses.domain.models.requests.CreateExpenseAccountRequest
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseProofFile
import com.teco.ventago.features.expenses.domain.models.requests.ListExpensesRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpdateExpenseAccountRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpensePaymentRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpenseRequest
import com.teco.ventago.features.expenses.domain.models.requests.ListMerchantsRequest
import com.teco.ventago.features.expenses.domain.models.requests.CreateMerchantRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpdateMerchantRequest
import com.teco.ventago.json
import com.teco.ventago.utils.BadRequestException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.contentOrNull

class ExpensesRepository(
    private val provider: IExpensesProvider,
    private val logger: ILoggerService
) : IExpensesRepository {

    override suspend fun listExpenses(businessId: Int, request: ListExpensesRequest): PagedExpenses {
        return try {
            val response = provider.listExpenses(businessId, request)
            ensureSuccess(
                response = response,
                userMessage = ExpensesErrorMapper.mapCreateOrEditExpenseError(response)
            )
            val dataObj = response.data as? JsonObject ?: return PagedExpenses(
                expenses = emptyList(),
                total = 0L,
                page = request.page,
                size = request.pageSize
            )
            val expensesArray = dataObj["expenses"]?.jsonArray
            val expenses = expensesArray?.map { item ->
                mapExpense(item.jsonObject)
            } ?: emptyList()
            val total = dataObj["total"]?.jsonPrimitive?.longOrNull
            val size = dataObj["size"]?.jsonPrimitive?.longOrNull?.toInt()
                ?: dataObj["page_size"]?.jsonPrimitive?.longOrNull?.toInt()
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
            ensureSuccess(response, ExpensesErrorMapper.mapCreateOrEditExpenseError(response))
            val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
            mapExpense(dataObj)
        } catch (e: Exception) {
            logAndThrow(
                flow = "getExpense",
                context = "Error getting expense. businessId: $businessId, expenseId: $expenseId",
                error = e
            )
        }
    }

    override suspend fun createExpense(
        businessId: Int,
        request: UpsertExpenseRequest,
        file: ExpenseProofFile?,
        paymentProofFiles: List<ExpenseProofFile>
    ): Expense {
        return try {
            val response = provider.createExpense(businessId, request, file, paymentProofFiles)
            ensureSuccess(response, ExpensesErrorMapper.mapCreateOrEditExpenseError(response))
            val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
            mapExpense(dataObj)
        } catch (e: Exception) {
            logAndThrow(
                flow = "createExpense",
                context = "Error creating expense. businessId: $businessId",
                error = e
            )
        }
    }

    override suspend fun updateExpense(
        businessId: Int,
        expenseId: Long,
        request: UpsertExpenseRequest,
        file: ExpenseProofFile?
    ): Expense {
        return try {
            val response = provider.updateExpense(businessId, expenseId, request, file)
            ensureSuccess(response, ExpensesErrorMapper.mapCreateOrEditExpenseError(response))
            val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
            mapExpense(dataObj)
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
            ensureSuccess(response, ExpensesErrorMapper.mapCreateOrEditExpenseError(response))
            response.successful
        } catch (e: Exception) {
            logAndThrow(
                flow = "deleteExpense",
                context = "Error deleting expense. businessId: $businessId, expenseId: $expenseId",
                error = e
            )
        }
    }

    override suspend fun categorizeExpense(
        businessId: Int,
        expenseId: Long,
        request: CategorizeExpenseRequest
    ): Expense {
        return try {
            val response = provider.categorizeExpense(businessId, expenseId, request)
            ensureSuccess(response, ExpensesErrorMapper.mapCategorizationError(response))
            val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
            mapExpense(dataObj)
        } catch (e: Exception) {
            logAndThrow(
                flow = "categorizeExpense",
                context = "Error categorizing expense. businessId: $businessId, expenseId: $expenseId",
                error = e
            )
        }
    }

    override suspend fun getExpenseAccounts(businessId: Int, includeInactive: Boolean): List<ExpenseAccount> {
        return try {
            val response = provider.getExpenseAccounts(businessId, includeInactive)
            ensureSuccess(response, ExpensesErrorMapper.mapCatalogError(response))
            val dataArray = response.data as? JsonArray ?: return emptyList()
            dataArray.map { json.decodeFromJsonElement<ExpenseAccount>(it) }
        } catch (e: Exception) {
            logAndThrow(
                flow = "getExpenseAccounts",
                context = "Error getting expense accounts. businessId: $businessId, includeInactive: $includeInactive",
                error = e
            )
        }
    }

    override suspend fun createExpenseAccount(
        businessId: Int,
        request: CreateExpenseAccountRequest
    ): ExpenseAccount {
        return try {
            val response = provider.createExpenseAccount(businessId, request)
            ensureSuccess(response, ExpensesErrorMapper.mapCatalogError(response))
            val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
            json.decodeFromJsonElement(ExpenseAccount.serializer(), dataObj)
        } catch (e: Exception) {
            logAndThrow(
                flow = "createExpenseAccount",
                context = "Error creating expense account. businessId: $businessId",
                error = e
            )
        }
    }

    override suspend fun updateExpenseAccount(
        businessId: Int,
        accountId: Long,
        request: UpdateExpenseAccountRequest
    ): ExpenseAccount {
        return try {
            val response = provider.updateExpenseAccount(businessId, accountId, request)
            ensureSuccess(response, ExpensesErrorMapper.mapCatalogError(response))
            val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
            json.decodeFromJsonElement(ExpenseAccount.serializer(), dataObj)
        } catch (e: Exception) {
            logAndThrow(
                flow = "updateExpenseAccount",
                context = "Error updating expense account. businessId: $businessId, accountId: $accountId",
                error = e
            )
        }
    }

    override suspend fun deactivateExpenseAccount(businessId: Int, accountId: Long): Boolean {
        return try {
            val response = provider.deactivateExpenseAccount(businessId, accountId)
            ensureSuccess(response, ExpensesErrorMapper.mapCatalogError(response))
            response.successful
        } catch (e: Exception) {
            logAndThrow(
                flow = "deactivateExpenseAccount",
                context = "Error deactivating expense account. businessId: $businessId, accountId: $accountId",
                error = e
            )
        }
    }

    override suspend fun createPayment(
        businessId: Int,
        expenseId: Long,
        request: UpsertExpensePaymentRequest,
        proofFile: ExpenseProofFile?
    ): ExpensePaymentMutationResult {
        return try {
            val response = provider.createPayment(businessId, expenseId, request, proofFile)
            ensureSuccess(response, ExpensesErrorMapper.mapCreateOrEditExpenseError(response))
            val dataObj = response.data as? JsonObject
            parseExpensePaymentMutation(dataObj, expenseId)
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
            ensureSuccess(response, ExpensesErrorMapper.mapCreateOrEditExpenseError(response))
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
    ): ExpensePaymentMutationResult {
        return try {
            val response = provider.updatePayment(businessId, expenseId, paymentId, request, proofFile)
            ensureSuccess(response, ExpensesErrorMapper.mapCreateOrEditExpenseError(response))
            val dataObj = response.data as? JsonObject
            parseExpensePaymentMutation(dataObj, expenseId, paymentId)
        } catch (e: Exception) {
            logAndThrow(
                flow = "updatePayment",
                context = "Error updating payment. businessId: $businessId, expenseId: $expenseId, paymentId: $paymentId",
                error = e
            )
        }
    }

    override suspend fun deletePayment(businessId: Int, expenseId: Long, paymentId: Long): ExpensePaymentDeleteResult {
        return try {
            val response = provider.deletePayment(businessId, expenseId, paymentId)
            ensureSuccess(response, ExpensesErrorMapper.mapCreateOrEditExpenseError(response))
            val dataObj = response.data as? JsonObject
            val summary = (dataObj?.get("expense_summary") as? JsonObject)?.let(::mapExpensePaymentSnapshot)
            ExpensePaymentDeleteResult(success = response.successful, summary = summary)
        } catch (e: Exception) {
            logAndThrow(
                flow = "deletePayment",
                context = "Error deleting payment. businessId: $businessId, expenseId: $expenseId, paymentId: $paymentId",
                error = e
            )
        }
    }

    // Merchants

    override suspend fun listMerchants(businessId: Int, request: ListMerchantsRequest): PagedMerchants {
        return try {
            val response = provider.listMerchants(businessId, request)
            ensureSuccess(response, ExpensesErrorMapper.mapMerchantError(response))
            val dataObj = response.data?.jsonObject ?: return PagedMerchants()
            json.decodeFromJsonElement(PagedMerchants.serializer(), dataObj)
        } catch (e: Exception) {
            logAndThrow(
                flow = "listMerchants",
                context = "Error listing merchants. businessId: $businessId",
                error = e
            )
        }
    }

    override suspend fun getMerchant(businessId: Int, merchantId: Long): ExpenseMerchant {
        return try {
            val response = provider.getMerchant(businessId, merchantId)
            ensureSuccess(response, ExpensesErrorMapper.mapMerchantError(response))
            val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
            json.decodeFromJsonElement(ExpenseMerchant.serializer(), dataObj)
        } catch (e: Exception) {
            logAndThrow(
                flow = "getMerchant",
                context = "Error getting merchant. businessId: $businessId, merchantId: $merchantId",
                error = e
            )
        }
    }

    override suspend fun createMerchant(businessId: Int, request: CreateMerchantRequest): ExpenseMerchant {
        return try {
            val response = provider.createMerchant(businessId, request)
            ensureSuccess(response, ExpensesErrorMapper.mapMerchantError(response))
            val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
            json.decodeFromJsonElement(ExpenseMerchant.serializer(), dataObj)
        } catch (e: Exception) {
            logAndThrow(
                flow = "createMerchant",
                context = "Error creating merchant. businessId: $businessId",
                error = e
            )
        }
    }

    override suspend fun updateMerchant(businessId: Int, merchantId: Long, request: UpdateMerchantRequest): ExpenseMerchant {
        return try {
            val response = provider.updateMerchant(businessId, merchantId, request)
            ensureSuccess(response, ExpensesErrorMapper.mapMerchantError(response))
            val dataObj = response.data?.jsonObject ?: throw BadRequestException("Missing data")
            json.decodeFromJsonElement(ExpenseMerchant.serializer(), dataObj)
        } catch (e: Exception) {
            logAndThrow(
                flow = "updateMerchant",
                context = "Error updating merchant. businessId: $businessId, merchantId: $merchantId",
                error = e
            )
        }
    }

    override suspend fun deactivateMerchant(businessId: Int, merchantId: Long): Boolean {
        return try {
            val response = provider.deactivateMerchant(businessId, merchantId)
            ensureSuccess(response, ExpensesErrorMapper.mapMerchantError(response))
            response.successful
        } catch (e: Exception) {
            logAndThrow(
                flow = "deactivateMerchant",
                context = "Error deactivating merchant. businessId: $businessId, merchantId: $merchantId",
                error = e
            )
        }
    }

    override suspend fun crawlExpense(businessId: Int, payload: String): CrawlJob {
        return try {
            val response = provider.crawlExpense(businessId, payload)
            ensureSuccess(response, ExpensesErrorMapper.mapCreateOrEditExpenseError(response))
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
            ensureSuccess(response, ExpensesErrorMapper.mapCreateOrEditExpenseError(response))
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
            ensureSuccess(response, ExpensesErrorMapper.mapCreateOrEditExpenseError(response))
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

    private fun ensureSuccess(response: com.teco.ventago.utils.ApiResponse, userMessage: String) {
        if (!response.successful || !response.errorCode.isNullOrBlank()) {
            throw BadRequestException(userMessage)
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

    private fun parseExpensePaymentMutation(
        dataObj: JsonObject?,
        expenseId: Long,
        paymentId: Long? = null
    ): ExpensePaymentMutationResult {
        val summary = (dataObj?.get("expense_summary") as? JsonObject)?.let(::mapExpensePaymentSnapshot)
        val paymentPayload = when {
            dataObj == null -> null
            dataObj["id"] != null -> dataObj
            else -> dataObj["payment"] as? JsonObject
        }
        val payment = paymentPayload?.let(::mapExpensePayment)
            ?: ExpensePayment(id = paymentId, expenseId = expenseId)
        return ExpensePaymentMutationResult(payment = payment, summary = summary)
    }

    private fun mapExpensePaymentSnapshot(dataObj: JsonObject): ExpensePaymentSnapshot {
        val payments = (dataObj["payments"] as? JsonArray)?.mapNotNull { element ->
            (element as? JsonObject)?.let(::mapExpensePayment)
        } ?: emptyList()
        return ExpensePaymentSnapshot(
            totalPaid = dataObj.doubleValue("total_paid"),
            registeredAmount = dataObj.doubleValue("registered_amount"),
            remaining = dataObj.doubleValue("remaining"),
            paymentStatus = dataObj.stringValue("payment_status"),
            payments = payments
        )
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
        val primitive = this[key] as? JsonPrimitive ?: return null
        primitive.booleanOrNull?.let { return it }
        return primitive.contentOrNull?.lowercase()?.let {
            when (it) {
                "true" -> true
                "false" -> false
                else -> null
            }
        }
    }

    private fun mapExpense(dataObj: JsonObject): Expense {
        return Expense(
            id = dataObj.longValue("id"),
            businessId = dataObj.intValue("business_id"),
            invoiceNumber = dataObj.stringValue("invoice_number"),
            cufe = dataObj.stringValue("cufe"),
            emissionDate = dataObj.stringValue("emission_date"),
            issuer = (dataObj["issuer"] as? JsonObject)?.let(::mapExpenseParty),
            receiver = (dataObj["receiver"] as? JsonObject)?.let(::mapExpenseParty),
            subtotal = dataObj.doubleValue("subtotal"),
            itbmsTotal = dataObj.doubleValue("itbms_total"),
            totalAmount = dataObj.doubleValue("total_amount"),
            currencyCode = dataObj.stringValue("currency_code"),
            paymentMethod = dataObj.stringValue("payment_method"),
            notes = dataObj.stringValue("notes"),
            authorizationProtocol = dataObj.stringValue("authorization_protocol"),
            authorizationDate = dataObj.stringValue("authorization_date"),
            fileUrl = dataObj.stringValue("file_url"),
            source = dataObj.stringValue("source"),
            defaultAccountId = dataObj.longValue("default_account_id")
                ?: (dataObj["default_account"] as? JsonObject)?.longValue("id"),
            defaultAccount = (dataObj["default_account"] as? JsonObject)?.let(::mapExpenseAccount),
            categorizationStatus = dataObj.stringValue("categorization_status"),
            categorizedItemsCount = dataObj.intValue("categorized_items_count"),
            totalItemsCount = dataObj.intValue("total_items_count"),
            items = (dataObj["items"] as? JsonArray)?.mapNotNull {
                (it as? JsonObject)?.let(::mapExpenseItem)
            },
            payments = (dataObj["payments"] as? JsonArray)?.mapNotNull {
                (it as? JsonObject)?.let(::mapExpensePayment)
            },
            paymentStatus = dataObj.stringValue("payment_status"),
            paymentSummary = (dataObj["payment_summary"] as? JsonObject)?.let(::mapPaymentSummary),
            totalPaid = dataObj.doubleValue("total_paid"),
            merchant = (dataObj["merchant"] as? JsonObject)?.let { merchantObj ->
                runCatching { json.decodeFromJsonElement(ExpenseMerchant.serializer(), merchantObj) }.getOrNull()
            },
            createdAt = dataObj.stringValue("created_at"),
            updatedAt = dataObj.stringValue("updated_at")
        )
    }

    private fun mapExpenseItem(dataObj: JsonObject): ExpenseItem {
        val expenseAccount = (dataObj["expense_account"] as? JsonObject)?.let(::mapExpenseAccount)
        return ExpenseItem(
            id = dataObj.longValue("id"),
            lineNumber = dataObj.intValue("line_number"),
            itemCode = dataObj.stringValue("item_code"),
            description = dataObj.stringValue("description"),
            quantity = dataObj.doubleValue("quantity"),
            unitPrice = dataObj.doubleValue("unit_price"),
            discountAmount = dataObj.doubleValue("discount_amount"),
            subtotal = dataObj.doubleValue("subtotal"),
            itbmsAmount = dataObj.doubleValue("itbms_amount"),
            total = dataObj.doubleValue("total"),
            expenseAccountId = dataObj.longValue("expense_account_id") ?: expenseAccount?.id,
            expenseAccount = expenseAccount
        )
    }

    private fun mapExpenseAccount(dataObj: JsonObject): ExpenseAccount {
        return ExpenseAccount(
            id = dataObj.longValue("id") ?: 0L,
            businessId = dataObj.longValue("business_id"),
            parentId = dataObj.longValue("parent_id"),
            code = dataObj.stringValue("code").orEmpty(),
            name = dataObj.stringValue("name").orEmpty(),
            kind = dataObj.stringValue("kind") ?: "expense",
            isSystem = dataObj.booleanValue("is_system") ?: false,
            isActive = dataObj.booleanValue("is_active") ?: true,
            createdAt = dataObj.stringValue("created_at"),
            updatedAt = dataObj.stringValue("updated_at")
        )
    }

    private fun mapExpenseParty(dataObj: JsonObject): ExpenseParty {
        return ExpenseParty(
            name = dataObj.stringValue("name"),
            ruc = dataObj.stringValue("ruc"),
            dv = dataObj.stringValue("dv"),
            type = dataObj.stringValue("type"),
            address = dataObj.stringValue("address"),
            phone = dataObj.stringValue("phone")
        )
    }

    private fun mapPaymentSummary(dataObj: JsonObject): PaymentSummary {
        return PaymentSummary(
            totalPaid = dataObj.doubleValue("total_paid"),
            remaining = dataObj.doubleValue("remaining"),
            status = dataObj.stringValue("status")
        )
    }
}
