package com.teco.ventago.features.home.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HomeSummary(
    @SerialName("month_sales_total") val monthSalesTotal: Double = 0.0,
    @SerialName("month_sales_tax_total") val monthSalesTaxTotal: Double = 0.0,
    @SerialName("month_sales_total_with_taxes") val monthSalesTotalWithTaxes: Double = 0.0,
    @SerialName("year_sales_total") val yearSalesTotal: Double = 0.0,
    @SerialName("fiscal_monthly_sales") val fiscalMonthlySales: List<FiscalMonthlySalesPoint> = emptyList(),
    @SerialName("month_order_count") val monthOrderCount: Int = 0,
    @SerialName("month_sales_change_perc") val monthSalesChangePerc: Double = 0.0,
    @SerialName("today_sales_total") val todaySalesTotal: Double = 0.0,
    @SerialName("today_order_count") val todayOrderCount: Int = 0,
    @SerialName("month_expense_total") val monthExpenseTotal: Double = 0.0,
    @SerialName("month_expense_count") val monthExpenseCount: Int = 0,
    @SerialName("receivable_pending_amount") val receivablePendingAmount: Double = 0.0,
    @SerialName("receivable_pending_count") val receivablePendingCount: Int = 0,
    @SerialName("receivable_overdue_amount") val receivableOverdueAmount: Double = 0.0,
    @SerialName("receivable_overdue_count") val receivableOverdueCount: Int = 0,
    @SerialName("payable_pending_amount") val payablePendingAmount: Double = 0.0,
    @SerialName("payable_pending_count") val payablePendingCount: Int = 0,
    @SerialName("payable_overdue_amount") val payableOverdueAmount: Double = 0.0,
    @SerialName("payable_overdue_count") val payableOverdueCount: Int = 0,
    @SerialName("recurring_customer_count") val recurringCustomerCount: Int = 0,
    @SerialName("top_customers") val topCustomers: List<TopCustomer> = emptyList(),
    @SerialName("daily_sales_chart") val dailySalesChart: List<DailySalesPoint> = emptyList()
)

@Serializable
data class DailySalesPoint(
    @SerialName("date") val date: String = "",
    @SerialName("amount") val amount: Double = 0.0,
    @SerialName("count") val count: Int = 0
)

@Serializable
data class FiscalMonthlySalesPoint(
    @SerialName("month") val month: String = "",
    @SerialName("amount") val amount: Double = 0.0
)

@Serializable
data class TopCustomer(
    @SerialName("customer_id") val customerId: Long? = null,
    @SerialName("name") val name: String = "",
    @SerialName("total") val total: Double = 0.0,
    @SerialName("order_count") val orderCount: Int = 0
)

enum class HomeSalesRange {
    D7,
    D15,
    MONTH,
    YEAR
}
