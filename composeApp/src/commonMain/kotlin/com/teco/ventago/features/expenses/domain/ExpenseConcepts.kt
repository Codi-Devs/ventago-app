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

enum class ExpenseConceptVisualCategory {
    INVENTORY,
    PEOPLE,
    PROFESSIONAL,
    RENT,
    WATER,
    ENERGY,
    CONNECTIVITY,
    SECURITY,
    FOOD,
    TRAVEL,
    OFFICE,
    FUEL,
    SHIPPING,
    PARKING,
    MARKETING,
    TRAINING,
    INSURANCE,
    TECHNOLOGY,
    LEGAL,
    MAINTENANCE,
    FINANCE,
    TAX,
    GENERAL,
    FALLBACK,
}

private val systemExpenseAccountCodes = setOf(
    "COSTOS", "GASTOS", "COSTOS_VENTAS_OPERACION", "COSTO_MERCANCIA_VENDIDA",
    "COSTO_INVENTARIO", "AJUSTES_INVENTARIO", "DESCUENTOS_FINANCIEROS_COSTO",
    "DEVOLUCIONES_COMPRA_INVENTARIO", "COSTO_SERVICIOS_VENDIDOS", "COBRANZAS_COSTO",
    "COMISION_SERVICIOS_DESCUENTO", "COSTO_MANEJO", "COMISION_TARJETA_CREDITO_CLAVE",
    "COSTO_MERCANCIA_DANADA_DETERIORADA", "INTERESES_PAGADOS_COSTO", "MERMA_INVENTARIO",
    "MUESTRAS_COSTO", "PRIMA_PRODUCCION", "REASEGUROS_COSTO", "RESCATES_DIVIDENDOS",
    "SERVICIOS_OCASIONALES_COSTO", "SUBCONTRATISTAS", "OTROS_COSTOS", "GASTOS_VENTA",
    "GASTOS_PERSONAL_VENTAS", "SUELDOS_PERSONAL_VENTAS", "HORAS_EXTRAS_PERSONAL_VENTAS",
    "COMISIONES_PERSONAL_VENTAS", "PRIMA_ANTIGUEDAD_PERSONAL_VENTAS",
    "INDEMNIZACION_PERSONAL_VENTAS", "SEGURO_SOCIAL_VENTAS", "SEGURO_EDUCATIVO_VENTAS",
    "PREAVISO_VENTAS", "VACACIONES_VENTAS", "DOTACION_VENTAS", "GASTOS_ADMINISTRACION",
    "GASTOS_PERSONAL", "SUELDOS_SALARIOS", "HORAS_EXTRAS", "COMISIONES",
    "PRIMA_ANTIGUEDAD", "INDEMNIZACION_RECARGO", "SEGURO_EDUCATIVO", "PREAVISO",
    "VACACIONES", "DOTACION_TRABAJADORES", "GASTOS_GENERALES", "SERVICIOS_PROFESIONALES",
    "ASESORIA_JURIDICA", "ASESORIA_CONTABLE", "ARRENDAMIENTOS", "ARRENDAMIENTO_EQUIPOS",
    "ARRENDAMIENTO_OFICINAS", "SERVICIOS_PUBLICOS", "SERVICIO_GAS", "SERVICIO_ASEO",
    "SERVICIO_AGUA", "SERVICIO_ENERGIA", "SERVICIO_TELEFONO_INTERNET", "ASISTENCIA_TECNICA",
    "OTROS_SERVICIOS", "VIGILANCIA_SEGURIDAD", "GASTOS_REPRESENTACION",
    "COMIDAS_ENTRETENIMIENTO", "VIATICOS_VIAJES", "ARTICULOS_OFICINA", "PAPELERIA",
    "COMBUSTIBLES_LUBRICANTES", "FLETES_ENVIOS", "ENVIOS_MENSAJERIA", "ESTACIONAMIENTO",
    "PROPAGANDA_PUBLICIDAD", "CAPACITACION_PERSONAL", "SEGUROS", "SEGURO_ACCIDENTES",
    "SEGURO_VEHICULOS", "SEGURO_INCENDIOS", "PATENTES_MARCAS", "SERVICIOS_ONLINE",
    "SOFTWARE_CONTABLE", "CUOTAS_SUSCRIPCIONES", "OTROS_GASTOS_GENERALES",
    "GASTOS_CONSTITUCION", "GASTOS_LEGALES", "NOTARIALES", "REGISTROS_MERCANTILES",
    "TRAMITES_LEGALES", "MANTENIMIENTO_CONSERVACION", "CONSTRUCCION_EDIFICACION",
    "EQUIPO_OFICINA", "EQUIPO_COMPUTACION", "ADECUACIONES_INSTALACIONES",
    "DEPRECIACIONES_AMORTIZACIONES", "DETERIORO_CXC", "DEPR_PROPIEDAD_PLANTA_EQUIPO",
    "DEPR_CONSTRUCCIONES", "DEPR_MOBILIARIO_OFICINA", "DEPR_EQUIPO_COMPUTACION",
    "DEPR_VEHICULOS", "GASTOS_FINANCIEROS", "INTERESES_FINANCIEROS", "INTERESES_MORA",
    "COMISIONES_BANCARIAS", "PERDIDA_DIFERENCIA_CAMBIO", "AJUSTES_APROX_CALCULOS",
    "PERDIDA_DISPOSICION_ACTIVOS", "GASTOS_IMPUESTOS", "IMPUESTOS_VARIOS", "IMPUESTO_PLACA",
    "IMPUESTO_TIMBRES", "IMPUESTO_MUNICIPAL", "IMPUESTO_AVISO_OPERACION",
    "IMPUESTO_TASA_UNICA", "IMPUESTO_RENTA", "IMPUESTOS_NO_ACREDITABLES",
    "GASTOS_NO_DEDUCIBLES", "MULTAS_RECARGOS", "OTROS_GASTOS",
)

fun isKnownSystemExpenseAccountCode(code: String): Boolean =
    code.trim().uppercase() in systemExpenseAccountCodes

fun resolveExpenseConceptVisualCategory(expense: Expense): ExpenseConceptVisualCategory {
    if (expense.categorizationStatus == "partial") return ExpenseConceptVisualCategory.FALLBACK

    val itemAccountIds = expense.items.orEmpty().mapNotNull { it.expenseAccountId }.distinct()
    if (itemAccountIds.size > 1) return ExpenseConceptVisualCategory.FALLBACK

    val account = when {
        itemAccountIds.size == 1 -> {
            expense.items.orEmpty()
                .mapNotNull { it.expenseAccount }
                .firstOrNull { it.id == itemAccountIds.single() }
        }
        expense.defaultAccountId != null -> expense.defaultAccount
        else -> null
    } ?: return ExpenseConceptVisualCategory.FALLBACK

    if (!account.isSystem) return ExpenseConceptVisualCategory.FALLBACK
    val code = account.code.uppercase()

    return when {
        code == "COMBUSTIBLES_LUBRICANTES" -> ExpenseConceptVisualCategory.FUEL
        code == "COMIDAS_ENTRETENIMIENTO" -> ExpenseConceptVisualCategory.FOOD
        code == "VIATICOS_VIAJES" -> ExpenseConceptVisualCategory.TRAVEL
        code in setOf("ARTICULOS_OFICINA", "PAPELERIA", "EQUIPO_OFICINA") ->
            ExpenseConceptVisualCategory.OFFICE
        code.startsWith("FLETES_") || code.startsWith("ENVIOS_") ->
            ExpenseConceptVisualCategory.SHIPPING
        code == "ESTACIONAMIENTO" -> ExpenseConceptVisualCategory.PARKING
        code == "PROPAGANDA_PUBLICIDAD" -> ExpenseConceptVisualCategory.MARKETING
        code == "CAPACITACION_PERSONAL" -> ExpenseConceptVisualCategory.TRAINING
        code == "SERVICIO_AGUA" -> ExpenseConceptVisualCategory.WATER
        code == "SERVICIO_ENERGIA" -> ExpenseConceptVisualCategory.ENERGY
        code == "SERVICIO_TELEFONO_INTERNET" -> ExpenseConceptVisualCategory.CONNECTIVITY
        code == "VIGILANCIA_SEGURIDAD" -> ExpenseConceptVisualCategory.SECURITY
        code.startsWith("SEGURO") || code == "SEGUROS" || code == "REASEGUROS_COSTO" ->
            ExpenseConceptVisualCategory.INSURANCE
        code in setOf("SERVICIOS_ONLINE", "SOFTWARE_CONTABLE", "CUOTAS_SUSCRIPCIONES", "EQUIPO_COMPUTACION") ->
            ExpenseConceptVisualCategory.TECHNOLOGY
        code.startsWith("ARRENDAMIENTO") || code == "ARRENDAMIENTOS" ->
            ExpenseConceptVisualCategory.RENT
        code.contains("JURIDICA") || code.contains("LEGAL") || code == "NOTARIALES" ||
            code == "REGISTROS_MERCANTILES" || code == "TRAMITES_LEGALES" ->
            ExpenseConceptVisualCategory.LEGAL
        code == "SERVICIOS_PROFESIONALES" || code == "ASESORIA_CONTABLE" || code == "SUBCONTRATISTAS" ->
            ExpenseConceptVisualCategory.PROFESSIONAL
        code.startsWith("MANTENIMIENTO_") || code.startsWith("CONSTRUCCION_") ||
            code.startsWith("ADECUACIONES_") -> ExpenseConceptVisualCategory.MAINTENANCE
        code.startsWith("GASTOS_FINANCIEROS") || code.startsWith("INTERESES_") ||
            code.startsWith("COMISIONES_BANCARIAS") || code.startsWith("PERDIDA_") ->
            ExpenseConceptVisualCategory.FINANCE
        code.startsWith("GASTOS_IMPUESTOS") || code.startsWith("IMPUESTO") ->
            ExpenseConceptVisualCategory.TAX
        code.startsWith("COSTO") || code.startsWith("COSTOS") || code.contains("INVENTARIO") ||
            code.startsWith("MERCANCIA") || code.startsWith("MERMA_") ->
            ExpenseConceptVisualCategory.INVENTORY
        code.contains("PERSONAL") || code.startsWith("SUELDOS_") || code.startsWith("HORAS_EXTRAS") ||
            code.startsWith("PRIMA_ANTIGUEDAD") || code.startsWith("INDEMNIZACION") ||
            code.startsWith("VACACIONES") || code.startsWith("DOTACION_") || code.startsWith("PREAVISO") ->
            ExpenseConceptVisualCategory.PEOPLE
        else -> ExpenseConceptVisualCategory.GENERAL
    }
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
    val payloadItems = when (mode) {
        ExpenseConceptMode.GLOBAL -> emptyList()
        ExpenseConceptMode.PER_ITEM -> items.mapNotNull { item ->
            val accountId = item.accountId ?: return@mapNotNull null
            val normalizedItemId = item.itemId?.takeIf { it > 0 }
            if (normalizedItemId == null && item.lineNumber == null) return@mapNotNull null
            CategorizeExpenseItemRequest(
                itemId = normalizedItemId,
                lineNumber = if (normalizedItemId == null) item.lineNumber else null,
                accountId = accountId
            )
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

data class SelectableExpenseAccount(
    val account: ExpenseAccount,
    val ancestorNames: List<String>
)

fun compactExpenseAccountAncestors(ancestorNames: List<String>): String {
    val names = ancestorNames.map { it.trim() }.filter { it.isNotEmpty() }
    return when {
        names.isEmpty() -> ""
        names.size <= 2 -> names.joinToString(" / ")
        else -> "${names.first()} / … / ${names.last()}"
    }
}

fun selectableExpenseAccounts(
    accounts: List<ExpenseAccount>,
    leafOnly: Boolean
): List<SelectableExpenseAccount> {
    val result = mutableListOf<SelectableExpenseAccount>()

    fun walk(nodes: List<ExpenseAccountTreeNode>, ancestors: List<String>) {
        nodes.forEach { node ->
            if (!leafOnly || node.isLeaf) {
                result += SelectableExpenseAccount(node.account, ancestors)
            }
            if (node.children.isNotEmpty()) {
                walk(node.children, ancestors + node.account.name)
            }
        }
    }

    walk(buildExpenseAccountsTree(accounts), emptyList())
    return result
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
