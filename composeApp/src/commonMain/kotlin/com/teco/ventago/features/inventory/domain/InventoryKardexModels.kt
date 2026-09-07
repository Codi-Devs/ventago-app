package com.teco.ventago.features.inventory.domain

data class InventoryBalanceRow(
    val locationId: Int,
    val locationName: String,
    val stockState: String,
    val quantity: String,
)

data class KardexMovementRow(
    val movementId: Int,
    val movementType: String,
    val displayType: String? = null,
    val locationId: Int,
    val locationName: String? = null,
    val locationCode: String? = null,
    val direction: String,
    val quantity: String,
    val occurredAt: String,
    val postedAt: String,
    val stockState: String,
    val lineNo: Int = 0,
    val sourceType: String? = null,
    val sourceId: String? = null,
    val correlationId: String? = null,
    val reasonText: String? = null,
)

data class KardexOrderRef(
    val label: String,
    val orderNumber: String,
)

data class KardexPurchaseOrderRef(
    val purchaseOrderId: Int,
)

data class KardexMovementDetail(
    val typeLabel: String,
    val senseLabel: String,
    val isInbound: Boolean,
    val isValuation: Boolean,
    val quantityDisplay: String,
    val dateLabel: String,
    val locationLabel: String,
    val adjustmentReason: String? = null,
    val comment: String? = null,
    val valuationNote: String? = null,
    val orderRef: KardexOrderRef? = null,
    val purchaseOrderRef: KardexPurchaseOrderRef? = null,
    val genericReference: String? = null,
)

data class StockChartPoint(
    val qty: Double,
    val label: String,
)

data class ProductInventoryDetails(
    val visible: Boolean = false,
    val tracked: Boolean = false,
    val available: String = "0",
    val reserved: String = "0",
    val minQty: String = "",
    val avgCost: String = "",
    val alarmLabel: String = "Sin umbral",
    val alarmIsAlert: Boolean = false,
    val balances: List<InventoryBalanceRow> = emptyList(),
    val chartPoints: List<StockChartPoint> = emptyList(),
)

data class KardexPageResult(
    val rows: List<KardexMovementRow> = emptyList(),
    val displayRows: List<KardexMovementRow> = emptyList(),
    val nextBeforeMovementId: Int? = null,
    val hasMoreOnServer: Boolean = false,
)
