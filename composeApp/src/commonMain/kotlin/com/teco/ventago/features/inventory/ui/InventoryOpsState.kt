package com.teco.ventago.features.inventory.ui

import com.teco.ventago.features.branches.domain.model.Branch
import kotlinx.serialization.Serializable

@Serializable
data class InventoryLocationOption(
    val id: Int,
    val name: String,
    val code: String? = null,
    val stockable: Boolean = true,
    val isDefault: Boolean = false,
    val locationType: String = "warehouse",
    val parentLocationId: Int? = null,
    val active: Boolean = true,
    val version: Int = 0,
)

data class InventoryScopeDefault(
    val branchCode: String,
    val billingPoint: String,
    val locationId: Int,
    val version: Int = 0,
    val active: Boolean = true,
)

data class InventoryUnconfiguredScope(
    val branchCode: String,
    val branchName: String,
    val billingPoint: String,
    val billingPointLabel: String,
)

data class InventoryProductOption(
    val itemId: Int,
    val name: String,
    val sku: String? = null,
    val barcode: String? = null,
)

data class InventoryAlertRow(
    val itemId: Int,
    val productName: String = "",
    val available: String,
    val minQty: String,
    val suggestedQty: String,
)

enum class InventoryAlertStatus {
    BELOW_MIN,
    IN_RANGE,
    NO_THRESHOLD,
}

@Serializable
data class InventoryDashboardSummary(
    val availableUnits: String = "0",
    val stockValue: String = "0",
    val averageUnitCost: String? = null,
    val salesUnits30d: String = "0",
    val salesAmount30d: String = "0",
)

enum class InventoryActionSheet {
    ADJUST,
    TRANSFER,
    COUNT,
    ALERTS,
    WAREHOUSES,
}

data class InventoryOpsState(
    val loading: Boolean = false,
    val enabled: Boolean = false,
    val canView: Boolean = false,
    val canTransfer: Boolean = false,
    val canCount: Boolean = false,
    val canAdjust: Boolean = false,
    val canConfigure: Boolean = false,
    val message: String = "",
    val dashboard: InventoryDashboardSummary? = null,
    val locations: List<InventoryLocationOption> = emptyList(),
    val fromLocationId: String = "",
    val toLocationId: String = "",
    val itemId: String = "",
    val quantity: String = "",
    val countLocationId: String = "",
    val countedQty: String = "",
    val adjustLocationId: String = "",
    val adjustOperation: String = "increase",
    val adjustReason: String = "Ajuste manual",
    val adjustUnitCost: String = "",
    val alertsLocationId: String = "",
    val warehouseCode: String = "",
    val warehouseName: String = "",
    val warehouseLocationType: String = "warehouse",
    val warehouseParentLocationId: String = "",
    val warehouseStockable: Boolean = true,
    val warehouseAdvancedExpanded: Boolean = false,
    val scopeDefaults: List<InventoryScopeDefault> = emptyList(),
    val mappingBranches: List<Branch> = emptyList(),
    val unconfiguredScopes: List<InventoryUnconfiguredScope> = emptyList(),
    val mappingAddExpanded: Boolean = false,
    val mappingAddBranchCode: String = "",
    val mappingAddBillingPoint: String = "",
    val mappingAddLocationId: String = "",
    val mappingLocationEdits: Map<String, Int> = emptyMap(),
    val retireTargetLocationId: Int? = null,
    val retireReason: String = "",
    val warehousesLoading: Boolean = false,
    val productQuery: String = "",
    val selectedProductLabel: String = "",
    val productSuggestions: List<InventoryProductOption> = emptyList(),
    val alerts: List<InventoryAlertRow> = emptyList(),
)
