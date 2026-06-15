package com.teco.ventago.features.reports.domain.model

enum class RealTimeReportFilterType {
    TEXT,
    NUMBER,
    DATE,
    SELECT,
    BOOLEAN,
}

data class RealTimeReportFilterDefinition(
    val key: String,
    val label: String,
    val type: RealTimeReportFilterType,
    val options: List<RealTimeReportFilterOption> = emptyList(),
)

data class RealTimeReportFilterOption(
    val value: String,
    val label: String,
)

object RealTimeReportFilterCatalog {
    fun filtersFor(definition: RealTimeReportDefinition): List<RealTimeReportFilterDefinition> {
        val filters = mutableListOf<RealTimeReportFilterDefinition>()

        if (definition.filters.any { it.contains("start_date") || it.contains("custom") }) {
            filters.add(dateFilter("start_date", "Desde"))
            filters.add(dateFilter("end_date", "Hasta"))
        }

        if (definition.key == "financial_comparison") {
            filters.add(dateFilter("compare_start_date", "Comparar desde"))
            filters.add(dateFilter("compare_end_date", "Comparar hasta"))
        }

        filters.addAll(
            when (definition.key) {
                "sales_summary",
                "sales_by_product",
                "sales_by_payment_method",
                "sales_by_user",
                "sales_by_branch",
                "sales_by_customer" -> listOf(documentStatus())
                "sales_adjustments" -> listOf(
                    select(
                        key = "adjustment_type",
                        label = "Tipo de ajuste",
                        "all" to "Todos",
                        "canceled" to "Anulaciones",
                        "credit_note" to "Notas de crédito"
                    )
                )
                "sales_pending_collection" -> listOf(
                    select("date_field", "Fecha a evaluar", "due" to "Vencimiento", "issue" to "Emisión"),
                    balanceStatus(),
                    boolean("only_partial", "Solo parciales"),
                    boolean("only_overdue", "Solo vencidos")
                )
                "customer_statement" -> listOf(
                    text("customer_id", "Cliente"),
                    boolean("include_paid", "Incluir pagados"),
                    boolean("include_credit_notes", "Incluir notas de crédito")
                )
                "customers_with_balance" -> listOf(balanceStatus(), number("min_amount", "Monto minimo"))
                "new_customers" -> listOf(boolean("has_first_purchase", "Con primera compra"))
                "inactive_customers" -> listOf(dateFilter("inactive_since", "Inactivo desde"), number("min_amount", "Monto minimo"))
                "customer_ranking" -> listOf(documentStatus(), number("min_amount", "Monto minimo"))
                "sales_tax_summary" -> listOf(boolean("include_credit_notes", "Incluir notas de crédito"))
                "expense_tax_summary" -> listOf(
                    expenseGroupBy(),
                    select("tax_filter", "Filtro fiscal", "all" to "Todos", "with_tax" to "Con ITBMS", "without_tax" to "Sin ITBMS"),
                    text("supplier_ruc", "RUC proveedor"),
                    source(),
                    currency()
                )
                "expense_summary" -> listOf(periodGroupBy(), source(), paymentStatus(), currency())
                "expense_aging" -> listOf(
                    select(
                        "aging_bucket",
                        "Antigüedad",
                        "all" to "Todas",
                        "current" to "Actual",
                        "1_30" to "1-30 días",
                        "31_60" to "31-60 días",
                        "61_90" to "61-90 días",
                        "90_plus" to "Más de 90 días"
                    ),
                    paymentStatus(),
                    boolean("only_overdue", "Solo vencidos"),
                    boolean("only_partial", "Solo parciales"),
                    currency()
                )
                "expense_by_account" -> listOf(
                    select(
                        "group_by",
                        "Agrupar por",
                        "account" to "Cuenta",
                        "category" to "Categoría",
                        "classification" to "Clasificación",
                        "supplier" to "Proveedor"
                    ),
                    source(),
                    select("account_kind", "Tipo de cuenta", "cost" to "Costo", "expense" to "Gasto", "uncategorized" to "Sin categorizar"),
                    categorizationStatus(),
                    paymentStatus(),
                    currency()
                )
                "expense_by_supplier" -> listOf(
                    select("group_by", "Agrupar por", "supplier" to "Proveedor"),
                    text("supplier_ruc", "RUC proveedor"),
                    paymentStatus(),
                    source(),
                    currency()
                )
                "expense_detail" -> listOf(
                    text("merchant_id", "Comercio"),
                    text("supplier_ruc", "RUC proveedor"),
                    text("category_id", "Categoría"),
                    text("branch_id", "Sucursal"),
                    paymentStatus(),
                    categorizationStatus(),
                    source(),
                    select(
                        "payment_method",
                        "Método de pago",
                        "cash" to "Efectivo",
                        "card" to "Tarjeta",
                        "transfer" to "Transferencia",
                        "credit" to "Crédito"
                    ),
                    currency(),
                    number("min_amount", "Monto minimo"),
                    number("max_amount", "Monto maximo")
                )
                "recurring_expense_report" -> listOf(
                    text("supplier_id", "Proveedor"),
                    text("category_id", "Categoría"),
                    text("branch_id", "Sucursal"),
                    select("frequency", "Frecuencia", "monthly" to "Mensual", "weekly" to "Semanal", "quarterly" to "Trimestral", "annual" to "Anual"),
                    paymentStatus(),
                    currency()
                )
                "profit_and_loss" -> listOf(periodGroupBy(), base(), currency())
                "financial_comparison" -> listOf(base())
                "operating_margin" -> listOf(select("group_by", "Agrupar por", "month" to "Mes", "quarter" to "Trimestre", "year" to "Año"), base())
                "business_overview" -> listOf(base(), currency())
                "cash_flow" -> listOf(
                    select("group_by", "Agrupar por", "week" to "Semana", "day" to "Día", "month" to "Mes"),
                    boolean("include_overdue", "Incluir vencidos"),
                    select("type", "Tipo", "all" to "Todos", "receivable" to "Cobros", "payable" to "Pagos"),
                    select("status", "Estado", "all" to "Todos", "overdue" to "Vencidos", "due_soon" to "Por vencer", "current" to "Actuales"),
                    currency()
                )
                else -> emptyList()
            }
        )

        return filters.distinctBy { it.key }
    }

    private fun text(key: String, label: String) = RealTimeReportFilterDefinition(key, label, RealTimeReportFilterType.TEXT)
    private fun number(key: String, label: String) = RealTimeReportFilterDefinition(key, label, RealTimeReportFilterType.NUMBER)
    private fun dateFilter(key: String, label: String) = RealTimeReportFilterDefinition(key, label, RealTimeReportFilterType.DATE)
    private fun boolean(key: String, label: String) = RealTimeReportFilterDefinition(key, label, RealTimeReportFilterType.BOOLEAN)

    private fun select(key: String, label: String, vararg options: Pair<String, String>) =
        RealTimeReportFilterDefinition(
            key = key,
            label = label,
            type = RealTimeReportFilterType.SELECT,
            options = options.map { (value, optionLabel) -> RealTimeReportFilterOption(value, optionLabel) }
        )

    private fun documentStatus() = select("document_status", "Estado de documentos", "all" to "Todos", "valid" to "Válidos", "canceled" to "Anulados")
    private fun balanceStatus() = select("balance_status", "Estado de saldo", "all" to "Todos", "open" to "Abiertos", "overdue" to "Vencidos", "current" to "Al dia")
    private fun periodGroupBy() = select("group_by", "Agrupar por", "month" to "Mes", "week" to "Semana", "day" to "Día")
    private fun expenseGroupBy() = select("group_by", "Agrupar por", "month" to "Mes", "supplier" to "Proveedor", "category" to "Categoría", "source" to "Fuente")
    private fun source() = select("source", "Origen", "manual" to "Manual", "ocr" to "Escaneado", "import" to "Importado", "crawled" to "Automático")
    private fun paymentStatus() = select("payment_status", "Estado de pago", "all" to "Todos", "paid" to "Pagado", "pending" to "Pendiente", "partial" to "Parcial")
    private fun categorizationStatus() = select("categorization_status", "Categorización", "all" to "Todas", "categorized" to "Categorizado", "uncategorized" to "Sin categorizar")
    private fun currency() = select("currency_code", "Moneda", "USD" to "USD", "PAB" to "PAB")
    private fun base() = select("base", "Base", "issued" to "Emitido", "collected" to "Cobrado")
}
