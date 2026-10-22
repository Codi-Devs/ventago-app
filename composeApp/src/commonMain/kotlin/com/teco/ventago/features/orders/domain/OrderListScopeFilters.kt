package com.teco.ventago.features.orders.domain

import com.teco.ventago.features.branches.domain.model.Branch

data class OrderScopeFilterOption(
    val code: String,
    val label: String,
)

private const val ACTIVE_STATUS = 1

fun branchFilterOptions(branches: List<Branch>): List<OrderScopeFilterOption>? {
    val available = branches
        .filter { it.status == ACTIVE_STATUS && it.branchCode.isNotBlank() }
        .distinctBy { it.branchCode }
    if (available.size <= 1) return null
    return available.map { branch ->
        OrderScopeFilterOption(
            code = branch.branchCode,
            label = branch.name.takeIf { it.isNotBlank() }?.let { "${branch.branchCode} - $it" }
                ?: branch.branchCode,
        )
    }
}

fun billingPointFilterOptions(
    branches: List<Branch>,
    selectedBranchCode: String?,
): List<OrderScopeFilterOption>? {
    val selected = selectedBranchCode?.takeIf { it.isNotBlank() }
    val scope = branches
        .filter { it.status == ACTIVE_STATUS && it.branchCode.isNotBlank() }
        .filter { selected == null || it.branchCode == selected }
    val descriptionsByCode = linkedMapOf<String, MutableList<String>>()
    scope.forEach { branch ->
        branch.fiscalBillingPoints.forEach { point ->
            if (point.status != ACTIVE_STATUS || point.billingPoint.isBlank()) return@forEach
            val descriptions = descriptionsByCode.getOrPut(point.billingPoint) { mutableListOf() }
            val description = point.description?.trim().orEmpty()
            if (description.isNotEmpty() && description !in descriptions) {
                descriptions += description
            }
        }
    }
    if (descriptionsByCode.size <= 1) return null
    return descriptionsByCode.map { (code, descriptions) ->
        OrderScopeFilterOption(
            code = code,
            label = if (descriptions.size == 1) "$code - ${descriptions.first()}" else code,
        )
    }
}
