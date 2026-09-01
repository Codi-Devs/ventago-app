package com.teco.ventago.features.inventory.ui

import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.Products

internal object InventoryOpsSupport {
    fun productsFromCatalog(products: Products?): List<InventoryProductOption> {
        if (products == null) return emptyList()
        return products.categories
            .asSequence()
            .filter { it.active }
            .flatMap { category -> category.items.asSequence().filter { it.active } }
            .map { it.toInventoryProductOption() }
            .distinctBy { it.itemId }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    fun filterProducts(
        catalog: List<InventoryProductOption>,
        query: String,
        maxResults: Int = 8,
    ): List<InventoryProductOption> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return emptyList()
        return catalog
            .filter { option ->
                option.searchHaystack().contains(normalized)
            }
            .take(maxResults)
    }

    fun defaultStockableLocationId(locations: List<InventoryLocationOption>): String {
        val stockable = locations.filter { it.stockable }
        val preferred = stockable.firstOrNull { location ->
            location.isDefault ||
                location.name.contains("principal", ignoreCase = true) ||
                location.code?.contains("principal", ignoreCase = true) == true ||
                location.code.equals("default", ignoreCase = true)
        } ?: stockable.firstOrNull()
        return preferred?.id?.toString().orEmpty()
    }

    fun alertStatus(row: InventoryAlertRow): InventoryAlertStatus {
        val min = row.minQty.toDoubleOrNull() ?: 0.0
        if (min <= 0.0) return InventoryAlertStatus.NO_THRESHOLD
        val available = row.available.toDoubleOrNull() ?: 0.0
        return if (available < min) InventoryAlertStatus.BELOW_MIN else InventoryAlertStatus.IN_RANGE
    }

    fun alertStatusLabel(status: InventoryAlertStatus): String = when (status) {
        InventoryAlertStatus.BELOW_MIN -> "Bajo mínimo"
        InventoryAlertStatus.IN_RANGE -> "En rango"
        InventoryAlertStatus.NO_THRESHOLD -> "Sin umbral"
    }

    fun productLabel(option: InventoryProductOption): String =
        option.name.ifBlank { option.sku ?: "Producto ${option.itemId}" }

    fun productSecondaryLine(option: InventoryProductOption): String? {
        val parts = buildList {
            option.sku?.takeIf { it.isNotBlank() }?.let { add("SKU: $it") }
            option.barcode?.takeIf { it.isNotBlank() }?.let { add("Código: $it") }
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    fun locationLabel(location: InventoryLocationOption): String =
        location.name.ifBlank { location.code ?: location.id.toString() }

    fun parentLabel(location: InventoryLocationOption, locations: List<InventoryLocationOption>): String {
        val parentId = location.parentLocationId ?: return "—"
        return locations.firstOrNull { it.id == parentId }?.let(::locationLabel) ?: "—"
    }

    fun locationTypeLabel(type: String): String = when (type.lowercase()) {
        "bin" -> "Bin"
        "warehouse" -> "Almacén"
        else -> type.ifBlank { "—" }
    }

    fun suggestNextLocationCode(locations: List<InventoryLocationOption>, prefix: String = "ALM"): String {
        val normalizedPrefix = prefix.trim().uppercase().ifBlank { "ALM" }
        val used = locations.mapNotNull { row ->
            row.code?.trim()?.uppercase()?.takeIf { it.startsWith(normalizedPrefix) }
        }
        var index = 1
        while (true) {
            val candidate = "$normalizedPrefix-${index.toString().padStart(2, '0')}"
            if (used.none { it == candidate }) return candidate
            index++
            if (index > 999) return candidate
        }
    }

    fun parentWarehouseOptions(locations: List<InventoryLocationOption>): List<InventoryLocationOption> =
        locations.filter { location ->
            location.active &&
                location.locationType.equals("warehouse", ignoreCase = true) &&
                !location.stockable
        }

    fun stockableLocations(locations: List<InventoryLocationOption>): List<InventoryLocationOption> =
        locations.filter { it.active && it.stockable }

    fun findFallbackSaleLocation(locations: List<InventoryLocationOption>): InventoryLocationOption? {
        val stockable = stockableLocations(locations)
        return stockable.firstOrNull { it.code?.trim()?.equals("MAIN", ignoreCase = true) == true }
            ?: stockable.firstOrNull()
    }

    fun scopeDefaultKey(branchCode: String, billingPoint: String): String =
        "${branchCode.trim()}|${billingPoint.trim()}"

    fun activeScopeDefaults(defaults: List<InventoryScopeDefault>): List<InventoryScopeDefault> =
        defaults.filter { it.active && it.locationId > 0 }

    fun activeBranches(branches: List<Branch>): List<Branch> =
        branches.filter { it.status == 1 }

    fun activeBillingPoints(branch: Branch): List<FiscalBillingPoint> =
        branch.fiscalBillingPoints.filter { it.status == 1 }

    fun scopeDisplayLabel(branch: Branch?, point: FiscalBillingPoint?): String {
        if (branch == null || point == null) return "Punto de venta"
        val branchLabel = branch.name.ifBlank { "Sucursal ${branch.branchCode}" }
        val pointLabel = point.description?.takeIf { it.isNotBlank() } ?: "Punto ${point.billingPoint}"
        return "$branchLabel · $pointLabel"
    }

    fun scopeDisplayMeta(branch: Branch?, point: FiscalBillingPoint?): String {
        if (branch == null || point == null) return ""
        return "${branch.branchCode} · ${point.billingPoint}"
    }

    fun listUnconfiguredScopes(
        branches: List<Branch>,
        defaults: List<InventoryScopeDefault>,
    ): List<InventoryUnconfiguredScope> {
        val configured = activeScopeDefaults(defaults)
            .map { scopeDefaultKey(it.branchCode, it.billingPoint) }
            .toSet()
        val rows = mutableListOf<InventoryUnconfiguredScope>()
        activeBranches(branches).forEach { branch ->
            activeBillingPoints(branch).forEach { point ->
                val key = scopeDefaultKey(branch.branchCode, point.billingPoint)
                if (!configured.contains(key)) {
                    rows += InventoryUnconfiguredScope(
                        branchCode = branch.branchCode,
                        branchName = branch.name,
                        billingPoint = point.billingPoint,
                        billingPointLabel = point.description?.takeIf { it.isNotBlank() }
                            ?: "Punto ${point.billingPoint}",
                    )
                }
            }
        }
        return rows
    }

    fun resolveBranchPoint(
        branches: List<Branch>,
        branchCode: String,
        billingPoint: String,
    ): Pair<Branch?, FiscalBillingPoint?> {
        val branch = activeBranches(branches).firstOrNull { it.branchCode == branchCode }
        val point = branch?.let { branchItem ->
            activeBillingPoints(branchItem).firstOrNull { it.billingPoint == billingPoint }
        }
        return branch to point
    }

    fun isImplicitFallbackDefault(
        mapped: InventoryScopeDefault,
        locations: List<InventoryLocationOption>,
    ): Boolean {
        val fallback = findFallbackSaleLocation(locations) ?: return false
        return mapped.locationId == fallback.id
    }
}

private fun Item.toInventoryProductOption(): InventoryProductOption =
    InventoryProductOption(
        itemId = itemId,
        name = name,
        sku = sku,
        barcode = barcode,
    )

private fun InventoryProductOption.searchHaystack(): String =
    "$name ${sku.orEmpty()} ${barcode.orEmpty()} $itemId".lowercase()
