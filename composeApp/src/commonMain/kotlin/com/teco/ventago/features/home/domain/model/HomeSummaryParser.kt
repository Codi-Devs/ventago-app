package com.teco.ventago.features.home.domain.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object HomeSummaryParser {

    fun parse(data: JsonObject): HomeSummary {
        return HomeSummary(
            monthSalesTotal = data.doubleOrZero("month_sales_total"),
            monthSalesTaxTotal = data.doubleOrZero("month_sales_tax_total"),
            monthSalesTotalWithTaxes = data.doubleOrZero("month_sales_total_with_taxes"),
            yearSalesTotal = data.doubleOrZero("year_sales_total"),
            fiscalMonthlySales = parseFiscalMonthlySales(data["fiscal_monthly_sales"]),
            monthOrderCount = data.intOrZero("month_order_count"),
            monthSalesChangePerc = data.doubleOrZero("month_sales_change_perc"),
            todaySalesTotal = data.doubleOrZero("today_sales_total"),
            todayOrderCount = data.intOrZero("today_order_count"),
            monthExpenseTotal = data.doubleOrZero("month_expense_total"),
            monthExpenseCount = data.intOrZero("month_expense_count"),
            receivablePendingAmount = data.doubleOrZero("receivable_pending_amount"),
            receivablePendingCount = data.intOrZero("receivable_pending_count"),
            receivableOverdueAmount = data.doubleOrZero("receivable_overdue_amount"),
            receivableOverdueCount = data.intOrZero("receivable_overdue_count"),
            payablePendingAmount = data.doubleOrZero("payable_pending_amount"),
            payablePendingCount = data.intOrZero("payable_pending_count"),
            payableOverdueAmount = data.doubleOrZero("payable_overdue_amount"),
            payableOverdueCount = data.intOrZero("payable_overdue_count"),
            recurringCustomerCount = data.intOrZero("recurring_customer_count"),
            topCustomers = parseTopCustomers(data["top_customers"]),
            dailySalesChart = parseDailySalesChart(data["daily_sales_chart"])
        )
    }

    private fun parseDailySalesChart(data: JsonElement?): List<DailySalesPoint> {
        val jsonArray = data as? JsonArray ?: return emptyList()
        return jsonArray.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val date = obj.stringOrBlank("date")
            if (date.isBlank()) return@mapNotNull null
            DailySalesPoint(
                date = date,
                amount = obj.doubleOrZero("amount"),
                count = obj.intOrZero("count")
            )
        }
    }

    private fun parseFiscalMonthlySales(data: JsonElement?): List<FiscalMonthlySalesPoint> {
        val jsonArray = data as? JsonArray ?: return emptyList()
        return jsonArray.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val month = obj.stringOrBlank("month")
            if (month.isBlank()) return@mapNotNull null
            FiscalMonthlySalesPoint(
                month = month,
                amount = obj.doubleOrZero("amount")
            )
        }
    }

    private fun parseTopCustomers(data: JsonElement?): List<TopCustomer> {
        val jsonArray = data as? JsonArray ?: return emptyList()
        return jsonArray.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            TopCustomer(
                customerId = obj.longOrNull("customer_id", "customerId"),
                name = obj.stringOrBlank("name"),
                total = obj.doubleOrZero("total"),
                orderCount = obj.intOrZero("order_count", "orders_count", "orders", "orderCount")
            )
        }
    }
}

private fun JsonObject.stringOrBlank(field: String): String {
    return (this[field] as? JsonPrimitive)?.contentOrNull ?: ""
}

private fun JsonObject.doubleOrZero(vararg fields: String): Double {
    fields.forEach { field ->
        val primitive = this[field] as? JsonPrimitive ?: return@forEach
        primitive.contentOrNull?.toDoubleOrNull()?.let { return it }
    }
    return 0.0
}

private fun JsonObject.intOrZero(vararg fields: String): Int {
    fields.forEach { field ->
        val primitive = this[field] as? JsonPrimitive ?: return@forEach
        primitive.contentOrNull?.toIntOrNull()?.let { return it }
    }
    return 0
}

private fun JsonObject.longOrNull(vararg fields: String): Long? {
    fields.forEach { field ->
        val primitive = this[field] as? JsonPrimitive ?: return@forEach
        primitive.contentOrNull?.toLongOrNull()?.let { return it }
    }
    return null
}
