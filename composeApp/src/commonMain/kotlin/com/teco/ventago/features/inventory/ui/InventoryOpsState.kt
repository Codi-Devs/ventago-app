package com.teco.ventago.features.inventory.ui

data class InventoryLocationOption(
    val id: Int,
    val name: String,
)

data class InventoryAlertRow(
    val itemId: Int,
    val available: String,
    val minQty: String,
    val suggestedQty: String,
)

data class InventoryDashboardSummary(
    val availableUnits: String = "0",
    val stockValue: String = "0",
    val averageUnitCost: String? = null,
    val salesUnits30d: String = "0",
    val salesAmount30d: String = "0",
)

data class InventoryOpsState(
    val loading: Boolean = false,
    val enabled: Boolean = false,
    val canView: Boolean = false,
    val canTransfer: Boolean = false,
    val canCount: Boolean = false,
    val message: String = "",
    val dashboard: InventoryDashboardSummary? = null,
    val locations: List<InventoryLocationOption> = emptyList(),
    val fromLocationId: String = "",
    val toLocationId: String = "",
    val itemId: String = "",
    val quantity: String = "",
    val countLocationId: String = "",
    val countedQty: String = "",
    val alerts: List<InventoryAlertRow> = emptyList(),
)
