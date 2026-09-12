package com.teco.ventago.features.expenses.domain

import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpensePayment
import com.teco.ventago.features.expenses.domain.models.ExpensePaymentSnapshot
import com.teco.ventago.features.expenses.domain.models.PaymentSummary
import kotlinx.datetime.Instant

fun expenseTimestamp(expense: Expense): Long {
    val raw = expense.updatedAt ?: expense.createdAt ?: return 0L
    return runCatching { Instant.parse(raw).toEpochMilliseconds() }.getOrDefault(0L)
}

fun isExpenseSnapshotComplete(expense: Expense?): Boolean {
    if (expense == null) return false
    val totalItems = expense.totalItemsCount ?: 0
    val itemsLen = expense.items?.size ?: 0
    if (totalItems > 0 && itemsLen == 0) return false
    val totalPaid = expense.totalPaid ?: 0.0
    val paymentsLen = expense.payments?.size ?: 0
    if (totalPaid > 0.0 && paymentsLen == 0) return false
    return true
}

fun mergeExpenseSnapshots(existing: Expense?, incoming: Expense?): Expense? {
    if (existing == null) return incoming
    if (incoming == null) return existing
    if (expenseTimestamp(incoming) < expenseTimestamp(existing)) {
        return existing
    }
    val existingComplete = isExpenseSnapshotComplete(existing)
    val incomingComplete = isExpenseSnapshotComplete(incoming)
    if (existingComplete && !incomingComplete) {
        return existing
    }
    var merged = incoming
    if (incoming.items.isNullOrEmpty() && !existing.items.isNullOrEmpty()) {
        merged = merged.copy(
            items = existing.items,
            categorizationStatus = incoming.categorizationStatus ?: existing.categorizationStatus,
            categorizedItemsCount = incoming.categorizedItemsCount ?: existing.categorizedItemsCount,
            defaultAccountId = incoming.defaultAccountId ?: existing.defaultAccountId,
            defaultAccount = incoming.defaultAccount ?: existing.defaultAccount
        )
    }
    if (incoming.payments.isNullOrEmpty() && !existing.payments.isNullOrEmpty()) {
        val keepPaidTotals = (incoming.totalPaid ?: 0.0) <= 0.0
        merged = merged.copy(
            payments = existing.payments,
            totalPaid = if (keepPaidTotals) existing.totalPaid else incoming.totalPaid,
            paymentStatus = if (keepPaidTotals) existing.paymentStatus else incoming.paymentStatus,
            paymentSummary = if (keepPaidTotals) existing.paymentSummary else incoming.paymentSummary
        )
    }
    return merged
}

fun isPaymentEffectivelyPaid(payment: ExpensePayment): Boolean {
    val paymentDate = payment.paymentDate
    val status = payment.paymentStatus
    return !paymentDate.isNullOrBlank() || status == "paid"
}

fun registeredPaymentsTotal(
    payments: List<ExpensePayment>,
    excludePaymentId: Long? = null
): Double {
    return payments
        .filter { excludePaymentId == null || it.id != excludePaymentId }
        .sumOf { it.amountPaid ?: 0.0 }
}

fun remainingToRegister(
    totalAmount: Double,
    payments: List<ExpensePayment>,
    excludePaymentId: Long? = null
): Double {
    return (totalAmount - registeredPaymentsTotal(payments, excludePaymentId)).coerceAtLeast(0.0)
}

fun paidPaymentsTotal(payments: List<ExpensePayment>): Double {
    return payments.filter(::isPaymentEffectivelyPaid).sumOf { it.amountPaid ?: 0.0 }
}

fun upsertPayments(
    existing: List<ExpensePayment>,
    incoming: List<ExpensePayment>
): List<ExpensePayment> {
    val byId = linkedMapOf<Long, ExpensePayment>()
    existing.forEach { payment ->
        val id = payment.id ?: return@forEach
        byId[id] = payment
    }
    incoming.forEach { payment ->
        val id = payment.id ?: return@forEach
        byId[id] = payment
    }
    return byId.values.toList()
}

fun applyExpenseSummary(expense: Expense, summary: ExpensePaymentSnapshot): Expense {
    val payments = summary.payments
    val paid = summary.totalPaid ?: paidPaymentsTotal(payments)
    val remaining = summary.remaining
        ?: remainingToRegister(expense.totalAmount ?: 0.0, payments)
    val status = summary.paymentStatus ?: paymentStatusFromTotals(
        totalAmount = expense.totalAmount ?: 0.0,
        paidTotal = paid,
        remainingRegistered = remaining
    )
    return expense.copy(
        payments = payments,
        totalPaid = paid,
        paymentStatus = status,
        paymentSummary = PaymentSummary(
            totalPaid = paid,
            remaining = remaining,
            status = status
        )
    )
}

fun applyUpsertedPayment(expense: Expense, payment: ExpensePayment): Expense {
    val payments = upsertPayments(expense.payments.orEmpty(), listOf(payment))
    return summarizeExpensePayments(expense, payments)
}

fun applyDeletedPayment(expense: Expense, paymentId: Long): Expense {
    val payments = expense.payments.orEmpty().filterNot { it.id == paymentId }
    return summarizeExpensePayments(expense, payments)
}

fun summarizeExpensePayments(expense: Expense, payments: List<ExpensePayment>): Expense {
    val paidTotal = paidPaymentsTotal(payments)
    val remaining = remainingToRegister(expense.totalAmount ?: 0.0, payments)
    val status = paymentStatusFromTotals(expense.totalAmount ?: 0.0, paidTotal, remaining)
    return expense.copy(
        payments = payments,
        totalPaid = paidTotal,
        paymentStatus = status,
        paymentSummary = PaymentSummary(
            totalPaid = paidTotal,
            remaining = remaining,
            status = status
        )
    )
}

private fun paymentStatusFromTotals(totalAmount: Double, paidTotal: Double, remainingRegistered: Double): String {
    return when {
        remainingRegistered <= 0.0001 && totalAmount > 0.0 -> "paid"
        paidTotal <= 0.0 -> "not_paid"
        else -> "partial"
    }
}
