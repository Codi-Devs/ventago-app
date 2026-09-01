package com.teco.ventago.features.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.inventory.data.InventoryProvider
import com.teco.ventago.features.inventory.domain.InventoryAvailabilityStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class InventoryOpsViewModel(
    private val provider: InventoryProvider,
    private val businessService: BusinessService,
    private val availabilityStore: InventoryAvailabilityStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InventoryOpsState())
    val uiState: StateFlow<InventoryOpsState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onFromLocation(value: String) = update { copy(fromLocationId = value) }
    fun onToLocation(value: String) = update { copy(toLocationId = value) }
    fun onItemId(value: String) = update { copy(itemId = value) }
    fun onQuantity(value: String) = update { copy(quantity = value) }
    fun onCountLocation(value: String) = update { copy(countLocationId = value) }
    fun onCountedQty(value: String) = update { copy(countedQty = value) }

    fun refresh() {
        viewModelScope.launch {
            val businessId = businessService.business.value?.businessId ?: 0
            if (businessId <= 0) {
                _uiState.value = InventoryOpsState()
                return@launch
            }
            val enabled = availabilityStore.isModuleEnabled(businessId)
            val canView = availabilityStore.canView()
            val locationsResponse = runCatching { provider.listLocations(businessId) }.getOrNull()
            val locations = parseLocations(locationsResponse?.data)
            val dashboard = if (enabled && canView) {
                parseDashboard(runCatching { provider.dashboard(businessId) }.getOrNull()?.data)
            } else {
                null
            }
            _uiState.value = _uiState.value.copy(
                enabled = enabled,
                canView = canView,
                canTransfer = availabilityStore.canTransfer(),
                canCount = availabilityStore.canCount(),
                locations = locations,
                dashboard = dashboard,
                message = if (!enabled) "El módulo de inventario no está activo." else "",
            )
            if (enabled && canView) {
                loadAlerts()
            }
        }
    }

    fun transfer() {
        viewModelScope.launch {
            val state = _uiState.value
            val businessId = businessService.business.value?.businessId ?: 0
            val fromId = state.fromLocationId.toIntOrNull() ?: 0
            val toId = state.toLocationId.toIntOrNull() ?: 0
            val itemId = state.itemId.toIntOrNull() ?: 0
            if (fromId <= 0 || toId <= 0 || itemId <= 0 || state.quantity.isBlank()) {
                update { copy(message = "Completa origen, destino, producto y cantidad.") }
                return@launch
            }
            update { copy(loading = true, message = "") }
            val response = runCatching {
                provider.transfer(
                    businessId = businessId,
                    idempotencyKey = "app-transfer-${Clock.System.now().toEpochMilliseconds()}",
                    fromLocationId = fromId,
                    toLocationId = toId,
                    itemId = itemId,
                    quantity = state.quantity,
                    reason = "app-transfer",
                )
            }.getOrNull()
            update {
                copy(
                    loading = false,
                    message = if (response?.successful == true) {
                        "Transferencia posteada."
                    } else {
                        response?.errorMessage ?: "No se pudo transferir."
                    },
                )
            }
        }
    }

    fun commitCount() {
        viewModelScope.launch {
            val state = _uiState.value
            val businessId = businessService.business.value?.businessId ?: 0
            val locationId = state.countLocationId.toIntOrNull() ?: 0
            val itemId = state.itemId.toIntOrNull() ?: 0
            if (locationId <= 0 || itemId <= 0 || state.countedQty.isBlank()) {
                update { copy(message = "Completa ubicación, producto y cantidad contada.") }
                return@launch
            }
            update { copy(loading = true, message = "") }
            val response = runCatching {
                provider.countCommit(
                    businessId = businessId,
                    idempotencyKey = "app-count-${Clock.System.now().toEpochMilliseconds()}",
                    locationId = locationId,
                    itemId = itemId,
                    countedQty = state.countedQty,
                )
            }.getOrNull()
            update {
                copy(
                    loading = false,
                    message = if (response?.successful == true) {
                        "Conteo posteado."
                    } else {
                        response?.errorMessage ?: "No se pudo postear el conteo."
                    },
                )
            }
        }
    }

    fun loadAlerts() {
        viewModelScope.launch {
            if (!availabilityStore.canView()) {
                update { copy(alerts = emptyList()) }
                return@launch
            }
            val businessId = businessService.business.value?.businessId ?: 0
            val locationId = _uiState.value.countLocationId.toIntOrNull()
            val response = runCatching { provider.belowMin(businessId, locationId) }.getOrNull()
            val rows = parseAlerts(response?.data)
            update { copy(alerts = rows) }
        }
    }

    private fun update(block: InventoryOpsState.() -> InventoryOpsState) {
        _uiState.value = _uiState.value.block()
    }

    private fun parseLocations(data: JsonElement?): List<InventoryLocationOption> {
        val array = when (data) {
            is JsonArray -> data
            is JsonObject -> data["items"]?.jsonArray
            else -> null
        } ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
            val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull
                ?: obj["code"]?.jsonPrimitive?.contentOrNull
                ?: id.toString()
            InventoryLocationOption(id = id, name = name)
        }
    }

    private fun parseAlerts(data: JsonElement?): List<InventoryAlertRow> {
        val array = when (data) {
            is JsonArray -> data
            is JsonObject -> data["items"]?.jsonArray
            else -> null
        } ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
            val itemId = obj["item_id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            InventoryAlertRow(
                itemId = itemId,
                available = obj["available"]?.jsonPrimitive?.contentOrNull ?: "0",
                minQty = obj["min_qty"]?.jsonPrimitive?.contentOrNull ?: "0",
                suggestedQty = obj["suggested_qty"]?.jsonPrimitive?.contentOrNull ?: "0",
            )
        }
    }

    private fun parseDashboard(data: JsonElement?): InventoryDashboardSummary? {
        val obj = data as? JsonObject ?: return null
        val sales = obj["sales_30d"] as? JsonObject
        return InventoryDashboardSummary(
            availableUnits = obj["available_units"]?.jsonPrimitive?.contentOrNull ?: "0",
            stockValue = obj["stock_value"]?.jsonPrimitive?.contentOrNull ?: "0",
            averageUnitCost = obj["average_unit_cost"]?.jsonPrimitive?.contentOrNull,
            salesUnits30d = sales?.get("units")?.jsonPrimitive?.contentOrNull ?: "0",
            salesAmount30d = sales?.get("amount")?.jsonPrimitive?.contentOrNull ?: "0",
        )
    }
}
