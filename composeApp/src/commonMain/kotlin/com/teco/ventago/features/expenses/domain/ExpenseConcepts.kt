package com.teco.ventago.features.expenses.domain

import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpenseAccount
import com.teco.ventago.features.expenses.domain.models.ExpenseItem
import com.teco.ventago.features.expenses.domain.models.requests.CategorizeExpenseItemRequest
import com.teco.ventago.features.expenses.domain.models.requests.CategorizeExpenseRequest
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseItemRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpenseRequest

enum class ExpenseConceptMode {
    GLOBAL,
    PER_ITEM
}

data class ExpenseAccountTreeNode(
    val account: ExpenseAccount,
    val depth: Int,
    val isLeaf: Boolean,
    val children: List<ExpenseAccountTreeNode>
)

data class ExpenseItemConceptSelection(
    val itemId: Long? = null,
    val lineNumber: Int? = null,
    val accountId: Long? = null
)

fun resolveExpenseConceptMode(
    defaultAccountId: Long?,
    items: List<ExpenseItemConceptSelection>
): ExpenseConceptMode {
    val assignedAccounts = items.mapNotNull { it.accountId }.distinct()
    if (assignedAccounts.isEmpty()) return ExpenseConceptMode.GLOBAL
    if (assignedAccounts.size > 1) return ExpenseConceptMode.PER_ITEM

    val uniqueId = assignedAccounts.first()
    return if (defaultAccountId == null || defaultAccountId != uniqueId) {
        ExpenseConceptMode.PER_ITEM
    } else {
        ExpenseConceptMode.GLOBAL
    }
}

fun inferDefaultExpenseAccount(
    defaultAccountId: Long?,
    items: List<ExpenseItemConceptSelection>
): Long? {
    if (defaultAccountId != null) return defaultAccountId
    val uniqueId = items.mapNotNull { it.accountId }.distinct().singleOrNull()
    return uniqueId
}

fun buildExpenseCreatePayload(
    baseRequest: UpsertExpenseRequest,
    defaultAccountId: Long?,
    applyConceptPerItem: Boolean
): UpsertExpenseRequest {
    val items = baseRequest.items.map { item ->
        if (applyConceptPerItem) item else item.copy(expenseAccountId = null)
    }
    return baseRequest.copy(
        defaultAccountId = defaultAccountId,
        items = items
    )
}

fun buildExpenseCategorizationPayload(
    defaultAccountId: Long?,
    mode: ExpenseConceptMode,
    items: List<ExpenseItemConceptSelection>
): CategorizeExpenseRequest {
    fun toGlobalItems(globalId: Long): List<CategorizeExpenseItemRequest> {
        return items.mapNotNull { item ->
            val normalizedItemId = item.itemId?.takeIf { it > 0 }
            val lineNumber = item.lineNumber
            if (normalizedItemId == null && lineNumber == null) {
                null
            } else {
                CategorizeExpenseItemRequest(
                    itemId = normalizedItemId,
                    lineNumber = if (normalizedItemId == null) lineNumber else null,
                    accountId = globalId
                )
            }
        }
    }

    val payloadItems = when (mode) {
        ExpenseConceptMode.GLOBAL -> {
            val globalId = defaultAccountId ?: return CategorizeExpenseRequest(
                defaultAccountId = null,
                onlyUncategorized = false,
                items = emptyList()
            )
            toGlobalItems(globalId)
        }
        ExpenseConceptMode.PER_ITEM -> {
            val perItemSelections = items.mapNotNull { item ->
                val accountId = item.accountId ?: return@mapNotNull null
                val normalizedItemId = item.itemId?.takeIf { it > 0 }
                if (normalizedItemId == null && item.lineNumber == null) return@mapNotNull null
                CategorizeExpenseItemRequest(
                    itemId = normalizedItemId,
                    lineNumber = if (normalizedItemId == null) item.lineNumber else null,
                    accountId = accountId
                )
            }
            if (perItemSelections.isEmpty() && defaultAccountId != null) {
                toGlobalItems(defaultAccountId)
            } else {
                perItemSelections
            }
        }
    }

    return CategorizeExpenseRequest(
        defaultAccountId = defaultAccountId,
        onlyUncategorized = false,
        items = payloadItems
    )
}

fun buildExpenseConceptLabel(expense: Expense): String {
    val itemConceptNames = expense.items.orEmpty()
        .mapNotNull { it.expenseAccount?.name ?: it.expenseAccountId?.let { id -> "Concepto #$id" } }

    if (itemConceptNames.isNotEmpty()) {
        val grouped = itemConceptNames.groupingBy { it }.eachCount()
        val mainConcept = grouped.maxWithOrNull(
            compareBy<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key }
        )?.key ?: itemConceptNames.first()
        return if (grouped.size == 1) mainConcept else "$mainConcept y otros"
    }

    return expense.defaultAccount?.name
        ?: expense.defaultAccountId?.let { "Concepto #$it" }
        ?: "Sin concepto"
}

fun buildExpenseAccountsTree(accounts: List<ExpenseAccount>): List<ExpenseAccountTreeNode> {
    val accountsById = accounts.associateBy { it.id }
    val childrenByParent = mutableMapOf<Long?, MutableList<ExpenseAccount>>()

    accounts.forEach { account ->
        val normalizedParentId = when {
            account.parentId == null -> null
            account.parentId == account.id -> null
            accountsById[account.parentId] == null -> null
            else -> account.parentId
        }
        childrenByParent.getOrPut(normalizedParentId) { mutableListOf() }.add(account)
    }

    fun buildNodes(parentId: Long?, depth: Int): List<ExpenseAccountTreeNode> {
        val children = childrenByParent[parentId]
            .orEmpty()
            .sortedBy { it.name.lowercase() }
        return children.map { account ->
            val nested = buildNodes(account.id, depth + 1)
            ExpenseAccountTreeNode(
                account = account,
                depth = depth,
                isLeaf = nested.isEmpty(),
                children = nested
            )
        }
    }

    return buildNodes(parentId = null, depth = 0)
}

fun Expense.toConceptSelections(): List<ExpenseItemConceptSelection> {
    return items.orEmpty().map { item ->
        ExpenseItemConceptSelection(
            itemId = item.id,
            lineNumber = item.lineNumber,
            accountId = item.expenseAccountId
        )
    }
}

fun List<ExpenseItemRequest>.applyGlobalConcept(defaultAccountId: Long?): List<ExpenseItemRequest> {
    return map { item -> item.copy(expenseAccountId = null) }
}
