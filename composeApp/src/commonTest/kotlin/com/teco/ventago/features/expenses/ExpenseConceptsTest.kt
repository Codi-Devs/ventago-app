package com.teco.ventago.features.expenses

import com.teco.ventago.features.expenses.data.provider.normalizeExpensesApiResponse
import com.teco.ventago.features.expenses.data.provider.buildCategorizationPayload
import com.teco.ventago.features.expenses.domain.ExpenseConceptMode
import com.teco.ventago.features.expenses.domain.ExpenseItemConceptSelection
import com.teco.ventago.features.expenses.domain.ExpensesErrorMapper
import com.teco.ventago.features.expenses.domain.buildExpenseAccountsTree
import com.teco.ventago.features.expenses.domain.buildExpenseCategorizationPayload
import com.teco.ventago.features.expenses.domain.buildExpenseConceptLabel
import com.teco.ventago.features.expenses.domain.buildExpenseCreatePayload
import com.teco.ventago.features.expenses.domain.inferDefaultExpenseAccount
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpenseAccount
import com.teco.ventago.features.expenses.domain.models.ExpenseItem
import com.teco.ventago.features.expenses.domain.models.requests.CategorizeExpenseItemRequest
import com.teco.ventago.features.expenses.domain.models.requests.CategorizeExpenseRequest
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseItemRequest
import com.teco.ventago.features.expenses.domain.models.requests.ListExpensesRequest
import com.teco.ventago.features.expenses.domain.models.requests.ExpensePartyRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpenseRequest
import com.teco.ventago.features.expenses.domain.resolveExpenseConceptMode
import com.teco.ventago.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ExpenseConceptsTest {

    @Test
    fun normalizeWrappedResponseWithObjectError() {
        val body = Json.parseToJsonElement(
            """
            {
              "success": false,
              "data": null,
              "error": {
                "code": "O_RP_002",
                "message": "invalid"
              }
            }
            """.trimIndent()
        ) as JsonObject

        val response = normalizeExpensesApiResponse(body)

        assertFalse(response.successful)
        assertEquals("O_RP_002", response.errorCode)
        assertEquals("invalid", response.errorMessage)
    }

    @Test
    fun normalizeDirectPayloadResponse() {
        val body = Json.parseToJsonElement(
            """
            {
              "id": 9001,
              "invoice_number": "FAC-1001"
            }
            """.trimIndent()
        ) as JsonObject

        val response = normalizeExpensesApiResponse(body)

        assertTrue(response.successful)
        assertEquals(body, response.data)
    }

    @Test
    fun resolveConceptModeUsesGlobalWhenSingleAccountMatchesDefault() {
        val mode = resolveExpenseConceptMode(
            defaultAccountId = 101L,
            items = listOf(
                ExpenseItemConceptSelection(itemId = 1, accountId = 101L),
                ExpenseItemConceptSelection(itemId = 2, accountId = 101L)
            )
        )

        assertEquals(ExpenseConceptMode.GLOBAL, mode)
    }

    @Test
    fun resolveConceptModeUsesPerItemWhenMixedAccounts() {
        val mode = resolveExpenseConceptMode(
            defaultAccountId = 101L,
            items = listOf(
                ExpenseItemConceptSelection(itemId = 1, accountId = 140L),
                ExpenseItemConceptSelection(itemId = 2, accountId = 141L)
            )
        )

        assertEquals(ExpenseConceptMode.PER_ITEM, mode)
    }

    @Test
    fun inferDefaultAccountUsesSingleAssignedItemConcept() {
        assertEquals(
            140L,
            inferDefaultExpenseAccount(
                defaultAccountId = null,
                items = listOf(
                    ExpenseItemConceptSelection(itemId = 1, accountId = 140L),
                    ExpenseItemConceptSelection(itemId = 2, accountId = 140L)
                )
            )
        )
    }

    @Test
    fun buildAccountsTreeTreatsMissingParentAsRootAndMarksLeafs() {
        val accounts = listOf(
            ExpenseAccount(id = 3, parentId = 99, code = "z", name = "Zeta"),
            ExpenseAccount(id = 1, parentId = null, code = "a", name = "Alpha"),
            ExpenseAccount(id = 2, parentId = 1, code = "b", name = "Beta")
        )

        val tree = buildExpenseAccountsTree(accounts)

        assertEquals(listOf("Alpha", "Zeta"), tree.map { it.account.name })
        assertFalse(tree.first().isLeaf)
        assertTrue(tree.first().children.first().isLeaf)
        assertTrue(tree.last().isLeaf)
    }

    @Test
    fun buildExpenseConceptLabelPrefersMainItemConceptAndOthersSuffix() {
        val accountA = ExpenseAccount(id = 140, code = "a", name = "Papeleria")
        val accountB = ExpenseAccount(id = 141, code = "b", name = "Parqueo")
        val expense = Expense(
            items = listOf(
                ExpenseItem(id = 1, lineNumber = 1, expenseAccountId = 140, expenseAccount = accountA),
                ExpenseItem(id = 2, lineNumber = 2, expenseAccountId = 140, expenseAccount = accountA),
                ExpenseItem(id = 3, lineNumber = 3, expenseAccountId = 141, expenseAccount = accountB)
            )
        )

        assertEquals("Papeleria y otros", buildExpenseConceptLabel(expense))
    }

    @Test
    fun buildExpenseCreatePayloadRemovesItemConceptsInGlobalMode() {
        val request = UpsertExpenseRequest(
            businessId = 45,
            issuer = ExpensePartyRequest(name = "A"),
            receiver = ExpensePartyRequest(name = "B"),
            items = listOf(
                ExpenseItemRequest(
                    lineNumber = 1,
                    description = "Item",
                    quantity = 1.0,
                    unitPrice = 5.0,
                    discountAmount = 0.0,
                    subtotal = 5.0,
                    itbmsAmount = 0.35,
                    total = 5.35,
                    expenseAccountId = 140L
                )
            ),
            subtotal = 5.0,
            itbmsTotal = 0.35,
            totalAmount = 5.35
        )

        val payload = buildExpenseCreatePayload(
            baseRequest = request,
            defaultAccountId = 101L,
            applyConceptPerItem = false
        )

        assertEquals(101L, payload.defaultAccountId)
        assertNull(payload.items.first().expenseAccountId)
    }

    @Test
    fun buildExpenseCategorizationPayloadUsesLineNumberFallback() {
        val payload = buildExpenseCategorizationPayload(
            defaultAccountId = 101L,
            mode = ExpenseConceptMode.PER_ITEM,
            items = listOf(
                ExpenseItemConceptSelection(itemId = 33010L, lineNumber = 1, accountId = 140L),
                ExpenseItemConceptSelection(itemId = null, lineNumber = 2, accountId = 141L),
                ExpenseItemConceptSelection(itemId = null, lineNumber = 3, accountId = null)
            )
        )

        assertEquals(false, payload.onlyUncategorized)
        assertEquals(2, payload.items.size)
        assertEquals(33010L, payload.items.first().itemId)
        assertNull(payload.items.first().lineNumber)
        assertEquals(2, payload.items.last().lineNumber)
    }

    @Test
    fun buildExpenseCategorizationPayloadFallsBackToGlobalItemsWhenPerItemHasOnlyDefault() {
        val payload = buildExpenseCategorizationPayload(
            defaultAccountId = 39L,
            mode = ExpenseConceptMode.PER_ITEM,
            items = listOf(
                ExpenseItemConceptSelection(itemId = 163L, lineNumber = 1, accountId = null)
            )
        )

        assertEquals(39L, payload.defaultAccountId)
        assertEquals(1, payload.items.size)
        assertEquals(163L, payload.items.first().itemId)
        assertEquals(39L, payload.items.first().accountId)
    }

    @Test
    fun buildExpenseCategorizationPayloadTreatsZeroItemIdAsMissingAndUsesLineNumber() {
        val payload = buildExpenseCategorizationPayload(
            defaultAccountId = 101L,
            mode = ExpenseConceptMode.GLOBAL,
            items = listOf(
                ExpenseItemConceptSelection(itemId = 0L, lineNumber = 2, accountId = null)
            )
        )

        assertNull(payload.items.first().itemId)
        assertEquals(2, payload.items.first().lineNumber)
        assertEquals(101L, payload.items.first().accountId)
    }

    @Test
    fun categorizeExpenseRequestSerializesExpectedBody() {
        val request = CategorizeExpenseRequest(
            defaultAccountId = 39L,
            onlyUncategorized = false,
            items = listOf(
                CategorizeExpenseItemRequest(
                    itemId = 163L,
                    accountId = 39L
                )
            )
        )

        val encoded = json.encodeToJsonElement(CategorizeExpenseRequest.serializer(), request).jsonObject

        assertEquals(39L, encoded["default_account_id"]?.jsonPrimitive?.content?.toLong())
        assertEquals(false, encoded["only_uncategorized"]?.jsonPrimitive?.content?.toBoolean())
        assertEquals(1, encoded["items"]?.jsonArray?.size)
        assertEquals(163L, encoded["items"]?.jsonArray?.first()?.jsonObject?.get("item_id")?.jsonPrimitive?.content?.toLong())
        assertEquals(39L, encoded["items"]?.jsonArray?.first()?.jsonObject?.get("account_id")?.jsonPrimitive?.content?.toLong())
    }

    @Test
    fun buildCategorizationPayloadMatchesBackendShape() {
        val payload = buildCategorizationPayload(
            CategorizeExpenseRequest(
                defaultAccountId = 39L,
                onlyUncategorized = false,
                items = listOf(
                    CategorizeExpenseItemRequest(
                        itemId = 163L,
                        accountId = 39L
                    )
                )
            )
        )

        assertEquals(39L, payload["default_account_id"]?.jsonPrimitive?.content?.toLong())
        assertEquals(false, payload["only_uncategorized"]?.jsonPrimitive?.content?.toBoolean())
        assertEquals(1, payload["items"]?.jsonArray?.size)
        assertEquals(163L, payload["items"]?.jsonArray?.first()?.jsonObject?.get("item_id")?.jsonPrimitive?.content?.toLong())
        assertEquals(39L, payload["items"]?.jsonArray?.first()?.jsonObject?.get("account_id")?.jsonPrimitive?.content?.toLong())
    }

    @Test
    fun listExpensesRequestSerializesPaymentStatusAsString() {
        val request = ListExpensesRequest(
            businessId = 4,
            page = 1,
            pageSize = 10,
            paymentStatus = "not_paid"
        )

        val encoded = json.encodeToJsonElement(ListExpensesRequest.serializer(), request).jsonObject

        assertEquals(4, encoded["business_id"]?.jsonPrimitive?.content?.toInt())
        assertEquals(1, encoded["page"]?.jsonPrimitive?.content?.toInt())
        assertEquals(10, encoded["page_size"]?.jsonPrimitive?.content?.toInt())
        assertEquals("not_paid", encoded["payment_status"]?.jsonPrimitive?.content)
    }

    @Test
    fun errorMapperReturnsSpanishMessages() {
        val response = normalizeExpensesApiResponse(
            Json.parseToJsonElement(
                """
                {
                  "success": false,
                  "data": null,
                  "error": {
                    "code": "AUTH_001",
                    "message": "expired"
                  }
                }
                """.trimIndent()
            ) as JsonObject
        )

        assertEquals(
            "Tu sesion expiro. Inicia sesion nuevamente.",
            ExpensesErrorMapper.mapCreateOrEditExpenseError(response)
        )
    }
}
