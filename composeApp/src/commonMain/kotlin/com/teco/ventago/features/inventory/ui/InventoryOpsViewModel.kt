package com.teco.ventago.features.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.inventory.data.InventoryProvider
import com.teco.ventago.features.inventory.domain.InventoryAvailabilityStore
import com.teco.ventago.features.inventory.domain.InventoryKardexSupport
import com.teco.ventago.features.inventory.domain.InventoryLocalCache
import com.teco.ventago.features.product.domain.ProductService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class InventoryOpsViewModel(
    private val provider: InventoryProvider,
    private val businessService: BusinessService,
    private val branchService: BranchService,
    private val productService: ProductService,
    private val availabilityStore: InventoryAvailabilityStore,
    private val cache: InventoryLocalCache,
    private val changesManager: IChangesManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InventoryOpsState())
    val uiState: StateFlow<InventoryOpsState> = _uiState.asStateFlow()
    private var catalogProducts: List<InventoryProductOption> = emptyList()

    init {
        productService.observe().onEach { products ->
            catalogProducts = InventoryOpsSupport.productsFromCatalog(products)
            val current = _uiState.value
            if (current.alerts.isNotEmpty()) {
                update { copy(alerts = enrichAlerts(current.alerts)) }
            }
        }.launchIn(viewModelScope)
        catalogProducts = InventoryOpsSupport.productsFromCatalog(productService.state.value)
        changesManager.inventoryListener().onEach { value ->
            if (value == 1) return@onEach
            val businessId = businessService.business.value?.businessId ?: 0
            if (businessId > 0) {
                cache.invalidateBusiness(businessId)
            }
            refresh(force = true)
        }.launchIn(viewModelScope)
        refresh()
    }

    fun onFromLocation(value: String) = update { copy(fromLocationId = value) }
    fun onToLocation(value: String) = update { copy(toLocationId = value) }
    fun onItemId(value: String) = update { copy(itemId = value) }
    fun onQuantity(value: String) = update { copy(quantity = value) }
    fun onCountLocation(value: String) = update { copy(countLocationId = value) }
    fun onCountedQty(value: String) = update { copy(countedQty = value) }
    fun onAdjustLocation(value: String) = update { copy(adjustLocationId = value) }
    fun onAdjustOperation(value: String) = update { copy(adjustOperation = value) }
    fun onAdjustReason(value: String) = update { copy(adjustReason = value) }
    fun onAdjustUnitCost(value: String) = update { copy(adjustUnitCost = value) }
    fun onAlertsLocation(value: String) = update { copy(alertsLocationId = value) }
    fun onWarehouseCode(value: String) = update { copy(warehouseCode = value.uppercase()) }
    fun onWarehouseName(value: String) = update { copy(warehouseName = value) }
    fun onWarehouseLocationType(value: String) = update {
        copy(
            warehouseLocationType = value,
            warehouseParentLocationId = if (value == "bin") warehouseParentLocationId else "",
        )
    }
    fun onWarehouseParentLocation(value: String) = update { copy(warehouseParentLocationId = value) }
    fun onWarehouseStockable(value: Boolean) = update { copy(warehouseStockable = value) }
    fun onWarehouseAdvancedExpanded(value: Boolean) = update { copy(warehouseAdvancedExpanded = value) }
    fun onMappingAddExpanded(value: Boolean) = update { copy(mappingAddExpanded = value) }
    fun onMappingAddBranchCode(value: String) = update {
        copy(
            mappingAddBranchCode = value,
            mappingAddBillingPoint = "",
        )
    }
    fun onMappingAddBillingPoint(value: String) = update { copy(mappingAddBillingPoint = value) }
    fun onMappingAddLocationId(value: String) = update { copy(mappingAddLocationId = value) }
    fun onMappingLocationEdit(key: String, locationId: Int) = update {
        copy(mappingLocationEdits = mappingLocationEdits + (key to locationId))
    }
    fun onRetireTarget(locationId: Int?) = update {
        copy(retireTargetLocationId = locationId, retireReason = if (locationId == null) "" else retireReason)
    }
    fun onRetireReason(value: String) = update { copy(retireReason = value) }
    fun clearMessage() = update { copy(message = "") }

    fun onProductQueryChange(value: String) {
        val hadSelection = _uiState.value.selectedProductLabel.isNotBlank()
        val clearsSelection = hadSelection && !value.equals(_uiState.value.selectedProductLabel, ignoreCase = true)
        update {
            copy(
                productQuery = value,
                itemId = if (clearsSelection) "" else itemId,
                selectedProductLabel = if (clearsSelection) "" else selectedProductLabel,
                productSuggestions = InventoryOpsSupport.filterProducts(catalogProducts, value),
            )
        }
    }

    fun selectProduct(option: InventoryProductOption) {
        update {
            copy(
                itemId = option.itemId.toString(),
                productQuery = InventoryOpsSupport.productLabel(option),
                selectedProductLabel = InventoryOpsSupport.productLabel(option),
                productSuggestions = emptyList(),
            )
        }
    }

    fun dismissProductSuggestions() = update { copy(productSuggestions = emptyList()) }

    fun resetProductSelection() = update {
        copy(
            productQuery = "",
            selectedProductLabel = "",
            itemId = "",
            productSuggestions = emptyList(),
        )
    }

    fun ensureSheetDefaults(sheet: InventoryActionSheet) {
        val defaultLocationId = InventoryOpsSupport.defaultStockableLocationId(_uiState.value.locations)
        if (defaultLocationId.isBlank()) return
        update {
            when (sheet) {
                InventoryActionSheet.ADJUST -> copy(
                    adjustLocationId = adjustLocationId.ifBlank { defaultLocationId },
                )
                InventoryActionSheet.COUNT -> copy(
                    countLocationId = countLocationId.ifBlank { defaultLocationId },
                )
                else -> this
            }
        }
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            val businessId = businessService.business.value?.businessId ?: 0
            if (businessId <= 0) {
                _uiState.value = InventoryOpsState()
                return@launch
            }
            val canView = availabilityStore.canView()
            val cachedEnabled = if (force) null else cache.peekAccess(businessId)
            val cachedLocations = if (force) null else cache.peekLocations(businessId)
            val cachedDashboard = if (force || !canView) null else cache.peekDashboard(businessId)
            if (cachedEnabled != null || cachedLocations != null || cachedDashboard != null) {
                val resolvedLocations = cachedLocations ?: _uiState.value.locations
                _uiState.value = applyLocationDefaults(
                    _uiState.value.copy(
                        enabled = cachedEnabled ?: _uiState.value.enabled,
                        canView = canView,
                        canTransfer = availabilityStore.canTransfer(),
                        canCount = availabilityStore.canCount(),
                        canAdjust = availabilityStore.canAdjust(),
                        canConfigure = availabilityStore.canConfigure(),
                        locations = resolvedLocations,
                        dashboard = cachedDashboard ?: _uiState.value.dashboard,
                        message = if (cachedEnabled == false) "El módulo de inventario no está activo." else "",
                    ),
                    resolvedLocations,
                )
            }
            val cacheHit = cachedEnabled != null && cachedLocations != null && (!canView || cachedDashboard != null)
            if (cacheHit) {
                if ((cachedEnabled == true) && canView) {
                    loadAlerts()
                }
                return@launch
            }
            val enabled = availabilityStore.isModuleEnabled(businessId, force)
            val locationsResponse = runCatching { provider.listLocations(businessId) }.getOrNull()
            val locations = parseLocations(locationsResponse?.data)
            if (locations.isNotEmpty()) {
                cache.persistLocations(businessId, locations)
            }
            val dashboard = if (enabled && canView) {
                parseDashboard(runCatching { provider.dashboard(businessId) }.getOrNull()?.data)?.also {
                    cache.persistDashboard(businessId, it)
                }
            } else {
                null
            }
            val resolvedLocations = locations.ifEmpty { cachedLocations.orEmpty() }
            _uiState.value = applyLocationDefaults(
                _uiState.value.copy(
                    enabled = enabled,
                    canView = canView,
                    canTransfer = availabilityStore.canTransfer(),
                    canCount = availabilityStore.canCount(),
                    canAdjust = availabilityStore.canAdjust(),
                    canConfigure = availabilityStore.canConfigure(),
                    locations = resolvedLocations,
                    dashboard = dashboard ?: cachedDashboard,
                    message = if (!enabled) "El módulo de inventario no está activo." else "",
                ),
                resolvedLocations,
            )
            if (enabled && canView) {
                loadAlerts()
            }
        }
    }

    fun adjustStock() {
        viewModelScope.launch {
            val state = _uiState.value
            val businessId = businessService.business.value?.businessId ?: 0
            val locationId = state.adjustLocationId.toIntOrNull() ?: 0
            val itemId = state.itemId.toIntOrNull() ?: 0
            val qty = state.quantity.trim()
            if (locationId <= 0 || itemId <= 0 || qty.isBlank() || (qty.toDoubleOrNull() ?: 0.0) <= 0.0) {
                update { copy(message = "Completa producto, ubicación y una cantidad válida.") }
                return@launch
            }
            update { copy(loading = true, message = "") }
            val response = runCatching {
                provider.adjustStock(
                    businessId = businessId,
                    idempotencyKey = "app-adjust-${Clock.System.now().toEpochMilliseconds()}",
                    reason = state.adjustReason.trim().ifBlank { "Ajuste manual" },
                    operation = state.adjustOperation,
                    itemId = itemId,
                    locationId = locationId,
                    quantity = qty,
                    unitCost = state.adjustUnitCost.trim().ifBlank { null },
                )
            }.getOrNull()
            if (response?.successful == true) {
                cache.invalidateBusiness(businessId)
                refresh(force = true)
            }
            update {
                copy(
                    loading = false,
                    message = if (response?.successful == true) {
                        "Ajuste aplicado."
                    } else {
                        response?.errorMessage ?: "No se pudo aplicar el ajuste."
                    },
                )
            }
        }
    }

    fun loadWarehousesPanel() {
        viewModelScope.launch {
            val businessId = businessService.business.value?.businessId ?: 0
            if (businessId <= 0) return@launch
            update { copy(warehousesLoading = true, message = "") }
            if (!branchService.isInitialized) {
                branchService.initialize(businessId)
            } else {
                runCatching { branchService.refresh(businessId) }
            }
            var locations = fetchLocations(businessId, ensureIfEmpty = availabilityStore.canConfigure())
            val defaults = fetchScopeDefaults(businessId)
            val branches = branchService.observe().value
            val unconfigured = InventoryOpsSupport.listUnconfiguredScopes(branches, defaults)
            val suggestedCode = InventoryOpsSupport.suggestNextLocationCode(locations)
            val firstUnconfigured = unconfigured.firstOrNull()
            update {
                copy(
                    locations = locations,
                    scopeDefaults = defaults,
                    mappingBranches = branches,
                    unconfiguredScopes = unconfigured,
                    warehousesLoading = false,
                    warehouseCode = warehouseCode.ifBlank { suggestedCode },
                    mappingAddBranchCode = mappingAddBranchCode.ifBlank {
                        firstUnconfigured?.branchCode.orEmpty()
                    },
                    mappingAddBillingPoint = mappingAddBillingPoint.ifBlank {
                        firstUnconfigured?.billingPoint.orEmpty()
                    },
                    mappingLocationEdits = emptyMap(),
                )
            }
        }
    }

    fun createWarehouse() {
        viewModelScope.launch {
            val state = _uiState.value
            val businessId = businessService.business.value?.businessId ?: 0
            val code = state.warehouseCode.trim()
            val name = state.warehouseName.trim()
            val locationType = state.warehouseLocationType.ifBlank { "warehouse" }
            val parentId = state.warehouseParentLocationId.toIntOrNull()
            if (code.isBlank() || name.isBlank()) {
                update { copy(message = "Código y nombre son requeridos.") }
                return@launch
            }
            if (locationType == "bin" && (parentId == null || parentId <= 0)) {
                update { copy(message = "Un bin requiere almacén padre.") }
                return@launch
            }
            update { copy(loading = true, message = "") }
            val response = runCatching {
                provider.createLocation(
                    businessId = businessId,
                    code = code,
                    name = name,
                    locationType = locationType,
                    parentLocationId = if (locationType == "bin") parentId else null,
                    stockable = state.warehouseStockable,
                )
            }.getOrNull()
            if (response?.successful == true) {
                cache.invalidateBusiness(businessId)
                refresh(force = true)
                loadWarehousesPanel()
            }
            update {
                copy(
                    loading = false,
                    warehouseName = if (response?.successful == true) "" else warehouseName,
                    warehouseParentLocationId = if (response?.successful == true) "" else warehouseParentLocationId,
                    warehouseAdvancedExpanded = if (response?.successful == true) false else warehouseAdvancedExpanded,
                    message = if (response?.successful == true) {
                        "Almacén creado."
                    } else {
                        response?.errorMessage ?: "No se pudo crear el almacén."
                    },
                )
            }
        }
    }

    fun retireWarehouse() {
        viewModelScope.launch {
            val state = _uiState.value
            val businessId = businessService.business.value?.businessId ?: 0
            val locationId = state.retireTargetLocationId ?: return@launch
            val location = state.locations.firstOrNull { it.id == locationId } ?: return@launch
            val reason = state.retireReason.trim()
            if (reason.isBlank()) {
                update { copy(message = "El motivo de retiro es obligatorio.") }
                return@launch
            }
            update { copy(loading = true, message = "") }
            val response = runCatching {
                provider.retireLocation(
                    businessId = businessId,
                    locationId = locationId,
                    reason = reason,
                    expectedVersion = location.version,
                )
            }.getOrNull()
            if (response?.successful == true) {
                cache.invalidateBusiness(businessId)
                refresh(force = true)
                loadWarehousesPanel()
            }
            update {
                copy(
                    loading = false,
                    retireTargetLocationId = if (response?.successful == true) null else retireTargetLocationId,
                    retireReason = if (response?.successful == true) "" else retireReason,
                    message = if (response?.successful == true) {
                        "Ubicación retirada."
                    } else {
                        response?.errorMessage ?: "No se pudo retirar la ubicación."
                    },
                )
            }
        }
    }

    fun saveScopeDefault(branchCode: String, billingPoint: String, version: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val businessId = businessService.business.value?.businessId ?: 0
            val key = InventoryOpsSupport.scopeDefaultKey(branchCode, billingPoint)
            val locationId = state.mappingLocationEdits[key]
                ?: state.scopeDefaults.firstOrNull {
                    it.branchCode == branchCode && it.billingPoint == billingPoint
                }?.locationId
                ?: 0
            if (locationId <= 0) {
                update { copy(message = "Selecciona un almacén inventariable.") }
                return@launch
            }
            update { copy(loading = true, message = "") }
            val response = runCatching {
                provider.setDefault(
                    businessId = businessId,
                    branchCode = branchCode,
                    billingPoint = billingPoint,
                    locationId = locationId,
                    expectedVersion = version,
                )
            }.getOrNull()
            if (response?.successful == true) {
                cache.invalidateBusiness(businessId)
                loadWarehousesPanel()
            }
            update {
                copy(
                    loading = false,
                    message = if (response?.successful == true) {
                        "Configuración guardada."
                    } else {
                        response?.errorMessage ?: "No se pudo guardar la configuración."
                    },
                )
            }
        }
    }

    fun removeScopeDefault(branchCode: String, billingPoint: String, version: Int) {
        viewModelScope.launch {
            val businessId = businessService.business.value?.businessId ?: 0
            update { copy(loading = true, message = "") }
            val response = runCatching {
                provider.removeDefault(
                    businessId = businessId,
                    branchCode = branchCode,
                    billingPoint = billingPoint,
                    expectedVersion = version,
                )
            }.getOrNull()
            if (response?.successful == true) {
                cache.invalidateBusiness(businessId)
                loadWarehousesPanel()
            }
            update {
                copy(
                    loading = false,
                    message = if (response?.successful == true) {
                        "Configuración eliminada."
                    } else {
                        response?.errorMessage ?: "No se pudo quitar la configuración."
                    },
                )
            }
        }
    }

    fun saveNewScopeDefault() {
        viewModelScope.launch {
            val state = _uiState.value
            val businessId = businessService.business.value?.businessId ?: 0
            val branchCode = state.mappingAddBranchCode.trim()
            val billingPoint = state.mappingAddBillingPoint.trim()
            val locationId = state.mappingAddLocationId.toIntOrNull() ?: 0
            if (branchCode.isBlank() || billingPoint.isBlank() || locationId <= 0) {
                update { copy(message = "Selecciona sucursal, punto y almacén.") }
                return@launch
            }
            val existing = state.scopeDefaults.firstOrNull {
                it.branchCode == branchCode && it.billingPoint == billingPoint && it.active
            }
            update { copy(loading = true, message = "") }
            val response = runCatching {
                provider.setDefault(
                    businessId = businessId,
                    branchCode = branchCode,
                    billingPoint = billingPoint,
                    locationId = locationId,
                    expectedVersion = existing?.version ?: 0,
                )
            }.getOrNull()
            if (response?.successful == true) {
                cache.invalidateBusiness(businessId)
                loadWarehousesPanel()
            }
            update {
                copy(
                    loading = false,
                    mappingAddExpanded = if (response?.successful == true) false else mappingAddExpanded,
                    message = if (response?.successful == true) {
                        "Configuración guardada."
                    } else {
                        response?.errorMessage ?: "No se pudo guardar la configuración."
                    },
                )
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
            if (response?.successful == true) {
                cache.invalidateBusiness(businessId)
                refresh(force = true)
            }
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
            if (response?.successful == true) {
                cache.invalidateBusiness(businessId)
                refresh(force = true)
            }
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
            val locationId = _uiState.value.alertsLocationId.toIntOrNull()?.takeIf { it > 0 }
            val response = runCatching { provider.belowMin(businessId, locationId) }.getOrNull()
            val rows = enrichAlerts(parseAlerts(response?.data))
            update { copy(alerts = rows) }
        }
    }

    private fun applyLocationDefaults(
        state: InventoryOpsState,
        locations: List<InventoryLocationOption>,
    ): InventoryOpsState {
        val defaultLocationId = InventoryOpsSupport.defaultStockableLocationId(locations)
        if (defaultLocationId.isBlank()) return state
        return state.copy(
            adjustLocationId = state.adjustLocationId.ifBlank { defaultLocationId },
            countLocationId = state.countLocationId.ifBlank { defaultLocationId },
        )
    }

    private fun enrichAlerts(rows: List<InventoryAlertRow>): List<InventoryAlertRow> {
        if (catalogProducts.isEmpty()) return rows
        val namesById = catalogProducts.associate { it.itemId to InventoryOpsSupport.productLabel(it) }
        return rows.map { row ->
            if (row.productName.isNotBlank()) row
            else row.copy(productName = namesById[row.itemId].orEmpty())
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
            val code = obj["code"]?.jsonPrimitive?.contentOrNull
            val name = obj["name"]?.jsonPrimitive?.contentOrNull
                ?: code
                ?: id.toString()
            val stockable = obj["stockable"]?.jsonPrimitive?.let { primitive ->
                primitive.booleanOrNull == true || primitive.intOrNull == 1 ||
                    primitive.contentOrNull?.trim()?.lowercase() in setOf("true", "1")
            } ?: true
            val isDefault = obj["is_default"]?.jsonPrimitive?.let { primitive ->
                primitive.booleanOrNull == true || primitive.intOrNull == 1 ||
                    primitive.contentOrNull?.trim()?.lowercase() in setOf("true", "1")
            } == true
            val locationType = obj["location_type"]?.jsonPrimitive?.contentOrNull ?: "warehouse"
            val parentLocationId = obj["parent_location_id"]?.jsonPrimitive?.intOrNull
            val active = obj["active"]?.jsonPrimitive?.let { primitive ->
                primitive.booleanOrNull != false && primitive.intOrNull != 0 &&
                    primitive.contentOrNull?.trim()?.lowercase() !in setOf("false", "0")
            } ?: true
            val version = obj["version"]?.jsonPrimitive?.intOrNull ?: 0
            InventoryLocationOption(
                id = id,
                name = name,
                code = code,
                stockable = stockable,
                isDefault = isDefault,
                locationType = locationType,
                parentLocationId = parentLocationId,
                active = active,
                version = version,
            )
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
                productName = obj["item_name"]?.jsonPrimitive?.contentOrNull
                    ?: obj["name"]?.jsonPrimitive?.contentOrNull
                    ?: "",
                available = InventoryKardexSupport.formatInventoryQuantity(
                    obj["available"]?.jsonPrimitive?.contentOrNull ?: "0",
                ),
                minQty = InventoryKardexSupport.formatInventoryQuantity(
                    obj["min_qty"]?.jsonPrimitive?.contentOrNull ?: "0",
                ),
                suggestedQty = InventoryKardexSupport.formatInventoryQuantity(
                    obj["suggested_qty"]?.jsonPrimitive?.contentOrNull ?: "0",
                ),
            )
        }
    }

    private suspend fun fetchLocations(
        businessId: Int,
        ensureIfEmpty: Boolean,
    ): List<InventoryLocationOption> {
        var response = runCatching { provider.listLocations(businessId) }.getOrNull()
        var locations = parseLocations(response?.data)
        if (locations.isEmpty() && ensureIfEmpty) {
            runCatching { provider.ensureDefaultLocation(businessId) }
            response = runCatching { provider.listLocations(businessId) }.getOrNull()
            locations = parseLocations(response?.data)
        }
        if (locations.isNotEmpty()) {
            cache.persistLocations(businessId, locations)
        }
        return locations
    }

    private suspend fun fetchScopeDefaults(businessId: Int): List<InventoryScopeDefault> {
        val response = runCatching { provider.listDefaults(businessId) }.getOrNull()
        return parseScopeDefaults(response?.data)
    }

    private fun parseScopeDefaults(data: JsonElement?): List<InventoryScopeDefault> {
        val array = when (data) {
            is JsonArray -> data
            is JsonObject -> data["items"]?.jsonArray
            else -> null
        } ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
            val branchCode = obj["branch_code"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val billingPoint = obj["billing_point"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val locationId = obj["location_id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val active = obj["active"]?.jsonPrimitive?.let { primitive ->
                primitive.booleanOrNull != false && primitive.intOrNull != 0 &&
                    primitive.contentOrNull?.trim()?.lowercase() !in setOf("false", "0")
            } ?: true
            InventoryScopeDefault(
                branchCode = branchCode,
                billingPoint = billingPoint,
                locationId = locationId,
                version = obj["version"]?.jsonPrimitive?.intOrNull ?: 0,
                active = active,
            )
        }
    }

    private fun parseDashboard(data: JsonElement?): InventoryDashboardSummary? {
        val obj = data as? JsonObject ?: return null
        val sales = obj["sales_30d"] as? JsonObject
        return InventoryDashboardSummary(
            availableUnits = InventoryKardexSupport.formatInventoryQuantity(
                obj["available_units"]?.jsonPrimitive?.contentOrNull ?: "0",
            ),
            stockValue = obj["stock_value"]?.jsonPrimitive?.contentOrNull ?: "0",
            averageUnitCost = obj["average_unit_cost"]?.jsonPrimitive?.contentOrNull,
            salesUnits30d = InventoryKardexSupport.formatInventoryQuantity(
                sales?.get("units")?.jsonPrimitive?.contentOrNull ?: "0",
            ),
            salesAmount30d = sales?.get("amount")?.jsonPrimitive?.contentOrNull ?: "0",
        )
    }
}
