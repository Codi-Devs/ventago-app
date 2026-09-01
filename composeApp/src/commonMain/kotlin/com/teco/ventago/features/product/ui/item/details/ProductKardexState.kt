package com.teco.ventago.features.product.ui.item.details

import com.teco.ventago.features.inventory.domain.InventoryKardexSupport
import com.teco.ventago.features.inventory.domain.KardexMovementRow
import kotlin.math.min

data class ProductKardexUiState(
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val itemName: String = "",
    val rows: List<KardexMovementRow> = emptyList(),
    val displayRows: List<KardexMovementRow> = emptyList(),
    val page: Int = 0,
    val hasMoreOnServer: Boolean = false,
    val nextCursor: Int? = null,
    val errorMessage: String? = null,
) {
    val pageCount: Int
        get() = InventoryKardexSupport.pageCount(displayRows.size)

    val pagedRows: List<KardexMovementRow>
        get() = InventoryKardexSupport.pageSlice(displayRows, page)

    val totalMovements: Int
        get() = displayRows.size

    val pageStart: Int
        get() = if (displayRows.isEmpty()) 0 else page * InventoryKardexSupport.KARDEX_PAGE_SIZE + 1

    val pageEnd: Int
        get() = min((page + 1) * InventoryKardexSupport.KARDEX_PAGE_SIZE, displayRows.size)

    val hasPreviousPage: Boolean
        get() = page > 0

    val hasNextLocalPage: Boolean
        get() = page < pageCount - 1

    val showLoadMore: Boolean
        get() = hasMoreOnServer && !hasNextLocalPage && displayRows.isNotEmpty()

    val movementRangeText: String
        get() {
            if (displayRows.isEmpty()) return ""
            val suffix = if (hasMoreOnServer) "+" else ""
            return "Mostrando $pageStart–$pageEnd de $totalMovements$suffix"
        }

    val pageIndicatorText: String
        get() {
            if (displayRows.isEmpty()) return ""
            val suffix = if (hasMoreOnServer && page >= pageCount - 1) "+" else ""
            return "Página ${page + 1} de $pageCount$suffix"
        }
}
