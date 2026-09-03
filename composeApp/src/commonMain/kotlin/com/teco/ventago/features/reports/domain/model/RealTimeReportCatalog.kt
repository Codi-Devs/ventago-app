package com.teco.ventago.features.reports.domain.model

enum class RealTimeReportCategory(
    val title: String,
    val description: String,
) {
    SALES(
        title = "Ventas",
        description = "Facturación, cobros, productos, vendedores y saldos por cobrar."
    ),
    CUSTOMERS(
        title = "Clientes",
        description = "Cartera, actividad, estados de cuenta y ranking comercial."
    ),
    TAXES(
        title = "Impuestos",
        description = "ITBMS de ventas y gastos con detalle fiscal."
    ),
    EXPENSES(
        title = "Gastos",
        description = "Resumen, antiguedad, cuentas, proveedores y detalle operativo."
    ),
    FINANCE(
        title = "Finanzas",
        description = "Rentabilidad, comparativos, margen operativo y resumen ejecutivo."
    ),
    OPERATIONS(
        title = "Operativos",
        description = "Flujo de caja proyectado y vencimientos."
    ),
}

enum class ReportExportFormat(val label: String) {
    XLSX("Excel"),
    PDF("PDF"),
}

enum class ReportPdfEndpoint {
    NONE,
    DOWNLOAD_PDF,
    EXPORT_PDF,
}

data class RealTimeReportDefinition(
    val key: String,
    val title: String,
    val category: RealTimeReportCategory,
    val description: String,
    val defaultPreset: String,
    val filters: List<String>,
    val kpis: List<String>,
    val primaryChart: String?,
    val secondaryChart: String?,
    val rows: String,
    val isPaginated: Boolean = false,
    val exportFormats: Set<ReportExportFormat> = setOf(ReportExportFormat.XLSX, ReportExportFormat.PDF),
    val pdfEndpoint: ReportPdfEndpoint = ReportPdfEndpoint.DOWNLOAD_PDF,
    val notes: List<String> = emptyList(),
) {
    val endpoint: String = "/api/v1/reports/real-time/$key"
}

object RealTimeReportCatalog {
    val categories: List<RealTimeReportCategory> = RealTimeReportCategory.entries

    val reports: List<RealTimeReportDefinition> = listOf(
        RealTimeReportDefinition(
            key = "sales_summary",
            title = "Resumen de Ventas",
            category = RealTimeReportCategory.SALES,
            description = "KPIs de ventas, impuestos, cobros, pendientes, descuentos y anulaciones por periodo.",
            defaultPreset = "current_month",
            filters = listOf("preset", "start_date/end_date si custom", "document_status=all|valid|canceled"),
            kpis = listOf("Total vendido", "ITBMS generado", "Ticket promedio", "Pendiente", "Cobrado", "Descuentos", "Anuladas"),
            primaryChart = "Barra horizontal primaria con total por periodo o grupo.",
            secondaryChart = "Donut secundario Cobrado vs pendiente.",
            rows = "Filas por periodo con subtotal, descuento, ITBMS, total, cobrado, pendiente y anulado.",
            notes = listOf("No enviar group_by por defecto en reportes real-time de ventas.")
        ),
        RealTimeReportDefinition(
            key = "sales_by_product",
            title = "Ventas por Producto",
            category = RealTimeReportCategory.SALES,
            description = "Ranking de productos por monto y cantidad, con porcentaje de participación en ventas.",
            defaultPreset = "current_month",
            filters = listOf("preset", "start_date/end_date si custom", "document_status=all|valid|canceled"),
            kpis = listOf("Total vendido", "Documentos", "Cantidad de unidades", "Top por monto", "Top por cantidad"),
            primaryChart = "Barra horizontal primaria top productos por ventas.",
            secondaryChart = "Barra horizontal secundaria top productos por cantidad.",
            rows = "Código, producto/servicio, categoría, cantidad, montos y % ventas.",
            notes = listOf("Producto fallback: Producto no disponible.")
        ),
        RealTimeReportDefinition(
            key = "sales_by_payment_method",
            title = "Ventas por Método de Pago",
            category = RealTimeReportCategory.SALES,
            description = "Distribucion de ventas y cobros por metodo de pago.",
            defaultPreset = "current_month",
            filters = listOf("preset", "start_date/end_date si custom", "document_status=all|valid|canceled"),
            kpis = listOf("Total vendido", "Cobrado", "Pendiente", "Método principal", "Tasa de pago digital", "Documentos"),
            primaryChart = "Barra horizontal primaria monto por metodo.",
            secondaryChart = "Donut secundario distribucion de cobros.",
            rows = "Método, transacciones, documentos, total, cobrado, pendiente, ticket promedio y % ventas.",
            notes = listOf("Preferir label del backend y no mostrar tokens técnicos como etiqueta principal.")
        ),
        RealTimeReportDefinition(
            key = "sales_by_user",
            title = "Ventas por Vendedor",
            category = RealTimeReportCategory.SALES,
            description = "Ventas, clientes, cobros y ticket promedio agrupados por usuario vendedor.",
            defaultPreset = "current_month",
            filters = listOf("preset", "start_date/end_date si custom", "document_status=all|valid|canceled"),
            kpis = listOf("Total vendido", "Cobrado", "Pendiente", "Mejor vendedor", "Ticket promedio", "Documentos"),
            primaryChart = "Barra horizontal primaria ranking de vendedores.",
            secondaryChart = "Donut secundario cobrado vs pendiente.",
            rows = "Usuario, documentos, clientes, subtotal, ITBMS, total, cobrado, pendiente y ticket promedio.",
            notes = listOf("Usuario fallback: Sistema.")
        ),
        RealTimeReportDefinition(
            key = "sales_by_branch",
            title = "Ventas por Sucursal",
            category = RealTimeReportCategory.SALES,
            description = "Ranking de sucursales por ventas, cobros pendientes y participación.",
            defaultPreset = "current_month",
            filters = listOf("preset", "start_date/end_date si custom", "document_status=all|valid|canceled"),
            kpis = listOf("Total vendido", "Sucursal principal", "Cobrado", "Pendiente", "Ticket promedio", "Documentos"),
            primaryChart = "Barra horizontal primaria ranking de sucursales.",
            secondaryChart = "Donut secundario cobrado vs pendiente.",
            rows = "Sucursal, codigo, documentos, subtotal, ITBMS, total, cobrado, pendiente y % ventas.",
            notes = listOf("Sucursal fallback: Principal.")
        ),
        RealTimeReportDefinition(
            key = "sales_adjustments",
            title = "Anulaciones y Notas de Crédito",
            category = RealTimeReportCategory.SALES,
            description = "Anulaciones, notas de crédito, motivos y tendencia de ajustes.",
            defaultPreset = "current_month",
            filters = listOf("preset", "start_date/end_date si custom", "adjustment_type=all|canceled|credit_note", "page", "per_page"),
            kpis = listOf("Total ajustado", "Documentos ajustados", "Total anulado", "Notas de crédito", "% de ventas ajustadas", "Usuario principal"),
            primaryChart = "Barra primaria por tipo de ajuste.",
            secondaryChart = "Area secundaria de tendencia de ajustes.",
            rows = "Tipo, documento, documento afectado, cliente, fecha, usuario, motivo, subtotal, ITBMS y total.",
            isPaginated = true
        ),
        RealTimeReportDefinition(
            key = "sales_pending_collection",
            title = "Ventas Pendientes de Cobro",
            category = RealTimeReportCategory.SALES,
            description = "Cuentas por cobrar abiertas, vencidas, parciales y aging.",
            defaultPreset = "current_month",
            filters = listOf("preset", "start_date/end_date si custom", "date_field=issue|due", "balance_status=all|open|overdue|current", "only_partial", "only_overdue", "page", "per_page"),
            kpis = listOf("Total pendiente", "Total vencido", "Total parcial", "Cliente principal", "Días vencidos promedio", "Documentos pendientes"),
            primaryChart = "Barra horizontal primaria top saldos pendientes.",
            secondaryChart = "Donut secundario aging de CxC.",
            rows = "Documento, cliente, fecha de emision, vencimiento, total, pagado, pendiente, estado y dias vencidos.",
            isPaginated = true
        ),
        RealTimeReportDefinition(
            key = "sales_by_customer",
            title = "Ventas por Cliente",
            category = RealTimeReportCategory.CUSTOMERS,
            description = "Ventas, cobros pendientes y concentracion por cliente.",
            defaultPreset = "current_month",
            filters = listOf("preset", "start_date/end_date si custom", "document_status=all|valid|canceled"),
            kpis = listOf("Total vendido", "Documentos", "Clientes", "Cliente principal", "Concentración top 5"),
            primaryChart = "Barra horizontal primaria top clientes por ventas.",
            secondaryChart = "Donut secundario de concentracion o breakdown.",
            rows = "Cliente, RUC/ID, documentos, subtotal, ITBMS, total, cobrado, pendiente y % ventas.",
            notes = listOf("Cliente fallback: Consumidor Final.")
        ),
        RealTimeReportDefinition(
            key = "customer_statement",
            title = "Estado de Cuenta",
            category = RealTimeReportCategory.CUSTOMERS,
            description = "Movimientos, cargos, pagos, saldo y vencimiento para un cliente seleccionado.",
            defaultPreset = "current_month",
            filters = listOf("customer_id requerido", "preset", "start_date/end_date si custom", "include_paid", "include_credit_notes", "page", "per_page"),
            kpis = listOf("Total facturado", "Total pagado", "Saldo pendiente", "Saldo vencido", "Ultimo pago", "Documentos"),
            primaryChart = "Barra horizontal primaria de movimientos del cliente desde trend/timeline.",
            secondaryChart = "Donut secundario Facturado/Pagado/Saldo pendiente.",
            rows = "Fecha, tipo, documento, descripcion, cargo, pago, balance y estado.",
            isPaginated = true,
            notes = listOf("Si no hay cliente seleccionado, mostrar estado vacío y no consultar.")
        ),
        RealTimeReportDefinition(
            key = "customers_with_balance",
            title = "Clientes con Saldo Pendiente",
            category = RealTimeReportCategory.CUSTOMERS,
            description = "Cartera de clientes con saldos abiertos, vencidos y exposicion.",
            defaultPreset = "current_month",
            filters = listOf("preset", "start_date/end_date si custom", "balance_status=all|open|overdue|current", "min_amount"),
            kpis = listOf("Total pendiente", "Total vencido", "Clientes", "Cliente principal", "Promedio por cliente", "Días vencidos promedio"),
            primaryChart = "Barra horizontal primaria top clientes deudores.",
            secondaryChart = "Donut secundario aging por saldo.",
            rows = "Cliente, RUC, documentos, pendiente, vencido, ultimo pago y maximo de dias vencidos."
        ),
        RealTimeReportDefinition(
            key = "new_customers",
            title = "Clientes Nuevos",
            category = RealTimeReportCategory.CUSTOMERS,
            description = "Clientes creados, primeras compras y ventas asociadas.",
            defaultPreset = "current_month",
            filters = listOf("preset", "start_date/end_date si custom", "has_first_purchase"),
            kpis = listOf("Clientes nuevos", "Con primera compra", "Ventas a nuevos clientes", "Ticket primera compra", "Usuario principal"),
            primaryChart = "Barra horizontal primaria clientes nuevos.",
            secondaryChart = "Donut secundario ventas a clientes nuevos.",
            rows = "Cliente, RUC, tipo, fecha, primera compra, total comprado, usuario, creador y sucursal.",
            notes = listOf("Mostrar Sin compra cuando no hay primera compra.")
        ),
        RealTimeReportDefinition(
            key = "inactive_customers",
            title = "Clientes Inactivos",
            category = RealTimeReportCategory.CUSTOMERS,
            description = "Clientes sin compra reciente con ventas historicas, saldo pendiente y sugerencia.",
            defaultPreset = "current_month",
            filters = listOf("preset", "start_date/end_date si custom", "inactive_since", "min_amount"),
            kpis = listOf("Clientes inactivos", "Ventas históricas", "Días inactivos promedio", "Inactivos con saldo", "Cliente principal"),
            primaryChart = "Barra horizontal primaria valor historico inactivo.",
            secondaryChart = "Donut secundario buckets de inactividad.",
            rows = "Cliente, ultima compra, dias inactivo, ventas historicas, pendiente, usuario, creador y accion sugerida.",
            notes = listOf("Las sugerencias son texto; no crear acciones nuevas.")
        ),
        RealTimeReportDefinition(
            key = "customer_ranking",
            title = "Ranking de Clientes",
            category = RealTimeReportCategory.CUSTOMERS,
            description = "Ranking numerico de clientes por ventas, ticket y saldo pendiente.",
            defaultPreset = "current_month",
            filters = listOf("preset", "start_date/end_date si custom", "document_status=all|valid|canceled", "min_amount"),
            kpis = listOf("Total facturado", "Saldo pendiente", "Clientes activos", "Cliente principal", "% top 10", "Ticket promedio"),
            primaryChart = "Barra horizontal primaria top clientes por ventas.",
            secondaryChart = "Barra horizontal secundaria saldo pendiente.",
            rows = "Ranking, cliente, total, facturas, documentos, ticket promedio, ultima compra, pendiente y % ventas."
        ),
        RealTimeReportDefinition(
            key = "sales_tax_summary",
            title = "ITBMS en Ventas",
            category = RealTimeReportCategory.TAXES,
            description = "ITBMS bruto, retenciones, notas de crédito e impuesto neto por documento.",
            defaultPreset = "current_month",
            filters = listOf("preset", "start_date/end_date si custom", "include_credit_notes", "page", "per_page"),
            kpis = listOf("Subtotal gravable", "ITBMS bruto", "ITBMS generado", "Retenciones", "Notas de crédito", "ITBMS neto", "Total facturado"),
            primaryChart = "Barra vertical primaria ITBMS neto.",
            secondaryChart = "Barra horizontal distribuida de ITBMS bruto, retenciones, notas e ITBMS neto.",
            rows = "Documento, tipo, fecha, cliente, subtotal gravable, ITBMS, retencion, neto, exento, no gravado y total.",
            isPaginated = true
        ),
        RealTimeReportDefinition(
            key = "expense_tax_summary",
            title = "ITBMS en Gastos",
            category = RealTimeReportCategory.TAXES,
            description = "ITBMS de gastos por periodo, proveedor, categoria, fuente y moneda.",
            defaultPreset = "current_month",
            filters = listOf("preset", "start_date/end_date si custom", "group_by", "tax_filter", "supplier_ruc", "source", "currency_code"),
            kpis = listOf("Subtotal gravable", "ITBMS total", "Total con ITBMS", "Sin ITBMS", "Documentos", "Promedio ITBMS"),
            primaryChart = "Combo line/column por mes: Total como columna, ITBMS como linea.",
            secondaryChart = "Barra horizontal top proveedores por ITBMS y donut de breakdown fiscal.",
            rows = "Grupo, periodo, proveedor, RUC, categoria, fuente, documentos, subtotal, ITBMS y total.",
            pdfEndpoint = ReportPdfEndpoint.DOWNLOAD_PDF
        ),
        RealTimeReportDefinition(
            key = "expense_summary",
            title = "Resumen de Gastos",
            category = RealTimeReportCategory.EXPENSES,
            description = "KPIs, graficas y detalle de gastos por periodo.",
            defaultPreset = "current_year",
            filters = listOf("preset", "start_date/end_date si custom", "group_by=month|week|day", "source", "payment_status", "currency_code"),
            kpis = listOf("Total gastos", "Documentos", "Subtotal", "ITBMS", "Promedio por gasto", "Periodo mas alto"),
            primaryChart = "Barra agrupada Subtotal, ITBMS y Total.",
            secondaryChart = "Línea secundaria Total y Documentos.",
            rows = "Periodo, clave, cantidad, subtotal, ITBMS, total y totales de footer.",
            pdfEndpoint = ReportPdfEndpoint.EXPORT_PDF
        ),
        RealTimeReportDefinition(
            key = "expense_aging",
            title = "Antigüedad de CxP",
            category = RealTimeReportCategory.EXPENSES,
            description = "Cuentas por pagar abiertas, vencidas, parciales y alertas por proveedor.",
            defaultPreset = "all_open",
            filters = listOf("preset=all_open|overdue|next_7_days|current_month|custom", "start_date/end_date si custom", "aging_bucket", "payment_status", "only_overdue", "only_partial", "currency_code", "page", "per_page"),
            kpis = listOf("Total por pagar", "Total vencido", "No vencido", "Parcialmente pagado", "Documentos vencidos", "Proveedor mas vencido"),
            primaryChart = "Barra vertical agrupada por bucket con saldo y documentos.",
            secondaryChart = null,
            rows = "Gasto, documento, proveedor, vencimiento, bucket, estado, total, pagado, saldo, dias vencidos y alerta.",
            isPaginated = true,
            pdfEndpoint = ReportPdfEndpoint.EXPORT_PDF
        ),
        RealTimeReportDefinition(
            key = "expense_by_account",
            title = "Gastos por Cuenta",
            category = RealTimeReportCategory.EXPENSES,
            description = "Gastos clasificados por cuenta, categoría, tipo de cuenta, proveedor o período.",
            defaultPreset = "current_year",
            filters = listOf("preset", "start_date/end_date si custom", "group_by=account|category|account_kind|supplier|month|quarter|year", "source", "account_kind", "categorization_status", "payment_status", "currency_code"),
            kpis = listOf("Total categorizado", "% categorizado", "Total sin categorizar", "Gastos", "Items", "Categoría principal"),
            primaryChart = "Barra horizontal top categorias/cuentas.",
            secondaryChart = "Donut de clasificaciones.",
            rows = "Nombre, codigo, clasificacion, items, gastos, subtotal, ITBMS, total y % del total.",
            pdfEndpoint = ReportPdfEndpoint.EXPORT_PDF
        ),
        RealTimeReportDefinition(
            key = "expense_by_supplier",
            title = "Gastos por Proveedor",
            category = RealTimeReportCategory.EXPENSES,
            description = "Ranking de proveedores por gasto y concentracion top 5.",
            defaultPreset = "current_year",
            filters = listOf("preset", "start_date/end_date si custom", "group_by=supplier", "supplier_ruc", "payment_status", "source", "currency_code"),
            kpis = listOf("Total gastado", "Facturas", "Proveedores", "Proveedor principal", "Concentración top 5"),
            primaryChart = "Barra horizontal top proveedores.",
            secondaryChart = "Barra vertical agrupada de concentracion: Total y % del gasto.",
            rows = "RUC, proveedor, facturas, subtotal, ITBMS, total y % del total.",
            pdfEndpoint = ReportPdfEndpoint.EXPORT_PDF,
            notes = listOf("Ordenar filas por total descendente y alertar si top 5 >= 70%.")
        ),
        RealTimeReportDefinition(
            key = "expense_detail",
            title = "Detalle de Gastos",
            category = RealTimeReportCategory.EXPENSES,
            description = "Listado paginado de gastos con filtros operativos, montos y acciones.",
            defaultPreset = "current_month",
            filters = listOf("preset", "start_date/end_date si custom", "merchant_id", "supplier_ruc", "category_id", "payment_status", "categorization_status", "source", "payment_method", "currency_code", "min_amount", "max_amount", "page", "per_page", "sort_by", "sort_direction"),
            kpis = listOf("Documentos", "Subtotal", "ITBMS", "Total", "Pagado", "Pendiente"),
            primaryChart = null,
            secondaryChart = null,
            rows = "Documento, proveedor, RUC, emision, vencimiento, subtotal, ITBMS, total, pagado, pendiente, estado, categoria, fuente y acciones.",
            isPaginated = true,
            pdfEndpoint = ReportPdfEndpoint.EXPORT_PDF,
            notes = listOf("No inventar graficas; este reporte usa KPIs y tabla paginada.")
        ),
        RealTimeReportDefinition(
            key = "recurring_expense_report",
            title = "Gastos Recurrentes",
            category = RealTimeReportCategory.EXPENSES,
            description = "Patrones de gastos recurrentes por frecuencia, proveedor y categoria.",
            defaultPreset = "last_12_months",
            filters = listOf("preset", "start_date/end_date si custom", "supplier_id", "category_id", "frequency=monthly|bimonthly|quarterly|annual|variable", "payment_status", "currency_code"),
            kpis = listOf("Estimado mensual", "Patrones recurrentes", "Proveedor principal", "Categoría principal"),
            primaryChart = "Barra vertical agrupada por frecuencia: Estimado mensual y Patrones.",
            secondaryChart = "Barras horizontales por proveedor y categoria.",
            rows = "Proveedor, RUC, categoria, frecuencia, ocurrencias, ultimo documento, fecha, promedio, estimado mensual, proximo vencimiento y estado.",
            pdfEndpoint = ReportPdfEndpoint.DOWNLOAD_PDF
        ),
        RealTimeReportDefinition(
            key = "profit_and_loss",
            title = "Estado de Resultados",
            category = RealTimeReportCategory.FINANCE,
            description = "Ingresos, costos, gastos, utilidad y margen operativo.",
            defaultPreset = "current_month",
            filters = listOf("preset", "start_date/end_date si custom", "group_by=day|week|month|quarter|year", "base=issued|collected"),
            kpis = listOf("Ingresos", "Costos", "Gastos", "Gastos sin categoria", "Utilidad bruta", "Utilidad operativa", "Margen operativo"),
            primaryChart = "Línea multi-serie primaria Ingresos, Gastos y Utilidad.",
            secondaryChart = "Donut secundario composicion financiera.",
            rows = "Clave, etiqueta, actual, margen operativo, ingresos, gastos y utilidad.",
            pdfEndpoint = ReportPdfEndpoint.EXPORT_PDF,
            notes = listOf("Fila operating_margin se muestra como porcentaje, no dinero.")
        ),
        RealTimeReportDefinition(
            key = "financial_comparison",
            title = "Comparativo Financiero",
            category = RealTimeReportCategory.FINANCE,
            description = "Comparacion de ingresos, gastos y utilidad entre dos rangos custom.",
            defaultPreset = "custom",
            filters = listOf("preset=custom", "start_date", "end_date", "compare_start_date", "compare_end_date", "base"),
            kpis = listOf("Ingresos actual/anterior", "Variación de ingresos", "Gastos actual/anterior", "Variación de gastos", "Utilidad actual/anterior", "Variación de utilidad"),
            primaryChart = "Barra vertical agrupada primaria Actual vs Anterior.",
            secondaryChart = "Barra horizontal secundaria de variacion porcentual.",
            rows = "Clave, etiqueta, actual, anterior, diferencia y variacion porcentual.",
            exportFormats = setOf(ReportExportFormat.XLSX),
            pdfEndpoint = ReportPdfEndpoint.NONE,
            notes = listOf("Validar ambos rangos completos y ordenados.")
        ),
        RealTimeReportDefinition(
            key = "operating_margin",
            title = "Margen Operativo",
            category = RealTimeReportCategory.FINANCE,
            description = "Margen operativo, utilidad operativa, ingresos y gastos por periodo.",
            defaultPreset = "last_12_months",
            filters = listOf("preset", "start_date/end_date si custom", "group_by=month|quarter|year", "base"),
            kpis = listOf("Ingresos", "Gastos", "Utilidad operativa", "Margen operativo", "Variación de margen"),
            primaryChart = "Línea primaria de margen operativo con eje dinámico.",
            secondaryChart = "Donut secundario Ingresos/Gastos/Utilidad operativa.",
            rows = "Clave, etiqueta, ingresos, gastos, utilidad operativa y margen operativo.",
            exportFormats = emptySet(),
            pdfEndpoint = ReportPdfEndpoint.NONE
        ),
        RealTimeReportDefinition(
            key = "business_overview",
            title = "Resumen Ejecutivo",
            category = RealTimeReportCategory.FINANCE,
            description = "Vista ejecutiva de ventas, cobros, gastos, caja, utilidad, CxC, CxP y nuevos clientes.",
            defaultPreset = "current_month",
            filters = listOf("preset", "start_date/end_date si custom", "base"),
            kpis = listOf("Ventas", "Facturas", "Cobros", "Gastos", "Pagos", "Flujo neto", "Utilidad operativa", "CxC", "CxP", "Clientes nuevos"),
            primaryChart = "Barra horizontal primaria con principales indicadores desde rows[].",
            secondaryChart = "Donut secundario de clientes principales o breakdown.",
            rows = "Clave, etiqueta, valor actual y cantidad.",
            exportFormats = emptySet(),
            pdfEndpoint = ReportPdfEndpoint.NONE,
            notes = listOf("Filas solo de cantidad se muestran como cantidad, no $0.00.")
        ),
        RealTimeReportDefinition(
            key = "cash_flow",
            title = "Flujo de Caja",
            category = RealTimeReportCategory.OPERATIONS,
            description = "Cobros, pagos, neto proyectado, vencidos y aging en un dataset compartido.",
            defaultPreset = "next_30_days",
            filters = listOf("preset=next_30_days|next_7_days|current_month|custom", "start_date/end_date si custom", "group_by=week|day|month", "include_overdue", "type=all|receivable|payable", "status=all|overdue|due_soon|current", "currency_code", "page", "per_page", "sort_by", "sort_direction"),
            kpis = listOf("Total por cobrar", "Cobros vencidos", "Total por pagar", "Pagos vencidos", "Flujo neto", "Flujo neto con vencidos", "Cobro esperado", "Pago esperado", "Ratio de cobertura"),
            primaryChart = "Combo ApexCharts: columnas Cobros, columnas Pagos negativas y linea Neto.",
            secondaryChart = "Tabs Resumen/Detalle/Cobros/Pagos/Aging usan el mismo items[].",
            rows = "Tipo, fuente, documento, contraparte, vencimiento, estado, total, pagado, pendiente y dias al vencimiento.",
            isPaginated = true,
            pdfEndpoint = ReportPdfEndpoint.EXPORT_PDF,
            notes = listOf("La paginacion vive fuera de tabs y controla el mismo items[] para Detalle, Cobros y Pagos.")
        ),
    )

    fun reportsFor(category: RealTimeReportCategory): List<RealTimeReportDefinition> {
        return reports.filter { it.category == category }
    }

    fun reportByKey(key: String): RealTimeReportDefinition? {
        return reports.firstOrNull { it.key == key }
    }
}
