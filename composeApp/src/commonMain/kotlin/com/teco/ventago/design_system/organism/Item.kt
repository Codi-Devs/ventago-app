package com.teco.ventago.design_system.organism

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import coil3.compose.AsyncImage
import com.teco.ventago.core.camera.PermissionCallback
import com.teco.ventago.core.camera.PermissionStatus
import com.teco.ventago.core.camera.PermissionType
import com.teco.ventago.core.camera.SharedImage
import com.teco.ventago.core.camera.createPermissionsManager
import com.teco.ventago.core.camera.rememberCameraManager
import com.teco.ventago.core.camera.rememberGalleryManager
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.DottedButton
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.textfields.DMMoneyOutlinedTextField
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.headlineSmall
import com.teco.ventago.design_system.theme.labelMedium
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.design_system.theme.Online
import com.teco.ventago.design_system.theme.RedLight
import com.teco.ventago.design_system.theme.WarningAmber
import com.teco.ventago.design_system.theme.badgeColorBlue
import com.teco.ventago.features.product.domain.model.ItemTax
import com.teco.ventago.features.product.domain.model.ProductType
import com.teco.ventago.features.product.domain.model.UomRegistry
import com.teco.ventago.features.product.ui.item.add.CollapsibleSectionCard
import com.teco.ventago.features.product.ui.item.add.GoodsSelectorDialog
import com.teco.ventago.features.product.ui.item.add.InformacionAdicionalContent
import com.teco.ventago.features.product.ui.item.add.OTITaxesContent
import com.teco.ventago.features.product.ui.item.add.ProductServiceSelector
import com.teco.ventago.features.product.ui.item.add.SectionCard
import com.teco.ventago.features.product.ui.item.add.StatusBadge
import com.teco.ventago.features.product.ui.item.add.UnitMeasureSelectorDialog
import com.teco.ventago.features.product.ui.item.add.viewmodel.ItemStateUiEvent
import com.teco.ventago.features.product.ui.item.add.viewmodel.ItemViewModel
import com.teco.ventago.features.product.ui.item.edit.EditItemViewModel
import com.teco.ventago.utils.BarcodeScannerScreen
import com.teco.ventago.utils.formatNumberToMoney
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.category
import ventago.composeapp.generated.resources.description
import ventago.composeapp.generated.resources.description_optional
import ventago.composeapp.generated.resources.description_optional_desc
import ventago.composeapp.generated.resources.help
import ventago.composeapp.generated.resources.must_select_price
import ventago.composeapp.generated.resources.name
import ventago.composeapp.generated.resources.name_not_valid
import ventago.composeapp.generated.resources.price
import ventago.composeapp.generated.resources.select_photo_from_camera
import ventago.composeapp.generated.resources.select_photo_from_gallery
import ventago.composeapp.generated.resources.understood
import kotlin.math.roundToInt

@Composable
fun ItemScreenActions(backStackEntry: NavBackStackEntry?) {
    val viewModel: EditItemViewModel = backStackEntry?.let {
        koinViewModel(viewModelStoreOwner = it)
    } ?: koinViewModel()

    val uiState by viewModel.uiState.collectAsState()

    if (!uiState.showScanner) {
        if (!uiState.canManageItems) return
        Switch(checked = uiState.active, onCheckedChange = {
            viewModel.onActiveChange(it)
        })
    } else {
        IconButton(onClick = {
            viewModel.showScanner(false)
        }) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemScreenContent(
    viewModel: ItemViewModel,
    navigateBack: () -> Unit,
    buttonActionTitle: String,
) {
    val analytics = koinInject<AnalyticsService>()
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    val uploadImageSheetState = rememberModalBottomSheetState()
    var showUploadImageSheet by remember { mutableStateOf(false) }

    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var sharedImage by remember { mutableStateOf<SharedImage?>(null) }

    var launchCamera by remember { mutableStateOf(value = false) }
    var launchScannerCamera by remember { mutableStateOf(value = false) }
    var launchGallery by remember { mutableStateOf(value = false) }
    var launchSetting by remember { mutableStateOf(value = false) }
    var permissionRationalDialog by remember { mutableStateOf(value = false) }
    val permissionsManager = createPermissionsManager(object : PermissionCallback {
        override fun onPermissionStatus(
            permissionType: PermissionType,
            status: PermissionStatus
        ) {
            when (status) {
                PermissionStatus.GRANTED -> {
                    when (permissionType) {
                        PermissionType.CAMERA -> launchCamera = true
                        PermissionType.GALLERY -> launchGallery = true
                    }
                }

                else -> {
                    permissionRationalDialog = true
                }
            }
        }
    })

    val cameraManager = rememberCameraManager {
        scope.launch {
            sharedImage = it
            imageBitmap = withContext(Dispatchers.Default) {
                it?.toImageBitmap()
            }
        }
    }

    val galleryManager = rememberGalleryManager {
        scope.launch {
            sharedImage = it
            imageBitmap = withContext(Dispatchers.Default) {
                it?.toImageBitmap()
            }
        }
    }

    if (launchGallery) {
        if (permissionsManager.isPermissionGranted(PermissionType.GALLERY)) {
            galleryManager.launch()
        } else {
            permissionsManager.askPermission(PermissionType.GALLERY)
        }
        launchGallery = false
    }
    if (launchCamera) {
        if (permissionsManager.isPermissionGranted(PermissionType.CAMERA)) {
            cameraManager.launch()
        } else {
            permissionsManager.askPermission(PermissionType.CAMERA)
        }
        launchCamera = false
    }
    if (launchSetting) {
        permissionsManager.launchSettings()
        launchSetting = false
    }
    if (launchScannerCamera) {
        if (permissionsManager.isPermissionGranted(PermissionType.CAMERA)) {
            viewModel.showScanner(!uiState.showScanner)
        } else {
            permissionsManager.askPermission(PermissionType.CAMERA)
        }
        launchScannerCamera = false
    }

    if (permissionRationalDialog) {
        DMAlertDialog(
            title = "Permisos requeridos",
            message = "Para acceder a la cámara, otorgue este permiso. Puedes administrar los permisos en la configuración de tu dispositivo.",
            confirmText = "Opciones",
            dismissText = "Cancelar",
            onConfirm = {
                permissionRationalDialog = false
                launchSetting = true
            },
            onDismiss = {
                permissionRationalDialog = false
            },
            show = permissionRationalDialog
        )

    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ItemStateUiEvent.GoBack -> navigateBack()
                else -> {}
            }
        }
    }

    if (uiState.showScanner) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            BarcodeScannerScreen(
                onResult = { barcodeNumber ->
                    viewModel.onBarcodeChange(barcodeNumber)
                    viewModel.showScanner(false)
                },
                onClose = { viewModel.showScanner(false) }
            )
        }
        return
    }

    var taxesExpanded by rememberSaveable { mutableStateOf(false) }
    var identificationExpanded by rememberSaveable { mutableStateOf(false) }
    var fiscalExpanded by rememberSaveable { mutableStateOf(false) }
    var inventoryExpanded by rememberSaveable { mutableStateOf(true) }

    val marginPercent = remember(uiState.price, uiState.cost) {
        if (uiState.price > 0 && uiState.cost > 0) {
            ((uiState.price - uiState.cost).toDouble() / uiState.price) * 100.0
        } else null
    }
    val marginColor = when {
        marginPercent == null -> MaterialTheme.colorScheme.onSurfaceVariant
        marginPercent >= 30 -> Online
        marginPercent >= 15 -> WarningAmber
        else -> RedLight
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .pointerInput(focusManager) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
    ) {
        SectionCard(
            title = "Información básica",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            badge = { StatusBadge(text = "Requerida", color = badgeColorBlue) }
        ) {
            ProductServiceSelector(
                selectedProductTypeId = uiState.productTypeId,
                onProductTypeSelected = viewModel::onProductTypeChange,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            DMOutlinedTextField(
                text = uiState.name,
                label = "Nombre del producto o servicio",
                modifier = Modifier.padding(bottom = 0.dp),
                onChange = { viewModel.onNameChange(it.take(100)) },
                maxLines = 5,
                imeAction = ImeAction.Next,
                isError = uiState.wrongName,
                supportingText = if (uiState.wrongName) stringResource(Res.string.name_not_valid) else "",
            )

            DMMoneyOutlinedTextField(
                text = uiState.price.toString(),
                label = stringResource(Res.string.price),
                modifier = Modifier.padding(bottom = 0.dp),
                onChange = {
                    try {
                        if (uiState.price == 0L && it.length > 1 && it[1] == '0') {
                            viewModel.onPriceChange(it.substring(0, 1).toLong())
                        } else {
                            viewModel.onPriceChange(it.toLong())
                        }
                    } catch (_: Exception) {
                        viewModel.onPriceChange(0)
                    }
                },
                leadingIcon = null,
                maxLines = 1,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
                isError = uiState.wrongPrice,
                supportingText = if (uiState.wrongPrice) stringResource(Res.string.must_select_price) else "",
            )

            DMDropDownField(
                label = "Tasa ITBMS",
                items = ItemTax.defaultTaxes.map { tax -> tax.name },
                selectedIndex = ItemTax.defaultTaxes.indexOfFirst { tax -> tax.value == uiState.taxPercent },
                modifier = Modifier.padding(bottom = 8.dp),
                onItemSelected = { index, _ -> viewModel.onTaxPercentChange(ItemTax.defaultTaxes[index].value) },
                isError = false,
            )

            if (uiState.productTypeId == ProductType.GOOD.typeId) {
                DMDropDownField(
                    label = "Unidad de medida",
                    items = viewModel.uomOptions(),
                    selectedIndex = viewModel.selectedUomIndex(),
                    modifier = Modifier.padding(bottom = 0.dp),
                    onItemSelected = { idx, _ -> viewModel.onUomSelected(UomRegistry.all()[idx].code) },
                    isError = false,
                )
            }
        }

        CollapsibleSectionCard(
            title = "Impuestos adicionales",
            expanded = taxesExpanded,
            onToggle = { taxesExpanded = !taxesExpanded },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            badge = { StatusBadge(text = "Opcional", color = Color(0xFFB0B0B0)) }
        ) {
            DMOutlinedTextField(
                text = uiState.iscRate ?: "",
                label = "Tasa ISC (opcional)",
                modifier = Modifier.padding(bottom = 0.dp),
                onChange = { viewModel.onIscRateChange(it) },
                maxLines = 1,
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Number,
            )

            Spacer(Modifier.height(8.dp))

            OTITaxesContent(
                otiTaxes = uiState.otiTaxes,
                selectedOtiIndex = uiState.selectedOtiIndex,
                otiRateInput = uiState.otiRateInput,
                onOtiTypeSelected = { idx -> viewModel.onOtiTypeSelected(idx) },
                onOtiRateChanged = { rate -> viewModel.onOtiRateChanged(rate) },
                onAddOti = { viewModel.onAddOtiTax() },
                onDeleteOti = { idx -> viewModel.onRemoveOtiTax(idx) },
            )
        }

        CollapsibleSectionCard(
            title = "Identificación y control",
            expanded = identificationExpanded,
            onToggle = { identificationExpanded = !identificationExpanded },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            badge = { StatusBadge(text = "Opcional", color = Color(0xFFB0B0B0)) }
        ) {
            EditProductImagePicker(
                imageBitmap = imageBitmap,
                imageUrl = uiState.imgUrl,
                onClick = { showUploadImageSheet = true }
            )

            DMOutlinedTextField(
                text = uiState.sku,
                label = "Referencia interna (SKU)",
                modifier = Modifier.padding(bottom = 0.dp),
                onChange = { viewModel.onSkuChange(it) },
                maxLines = 1,
                imeAction = ImeAction.Next,
            )

            DMOutlinedTextField(
                text = uiState.barcode,
                label = "Código de barras",
                modifier = Modifier.padding(bottom = 0.dp),
                onChange = { viewModel.onBarcodeChange(it) },
                maxLines = 1,
                imeAction = ImeAction.Next,
                trailingIcon = Icons.Rounded.QrCodeScanner,
                trailingIconClick = { launchScannerCamera = true }
            )

            DMMoneyOutlinedTextField(
                text = uiState.cost.toString(),
                label = if (uiState.inventorySectionVisible && uiState.inventoryTracked) {
                    "Costo de catálogo"
                } else {
                    "Costo (Opcional)"
                },
                modifier = Modifier.padding(bottom = 0.dp),
                onChange = {
                    try {
                        if (uiState.cost == 0L && it.length > 1 && it[1] == '0') {
                            viewModel.onCostChange(it.substring(0, 1).toLong())
                        } else {
                            viewModel.onCostChange(it.toLong())
                        }
                    } catch (_: Exception) {
                        viewModel.onCostChange(0)
                    }
                },
                leadingIcon = null,
                maxLines = 1,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            )

            if (uiState.inventorySectionVisible && uiState.inventoryCanView && uiState.inventoryTracked) {
                val avg = uiState.inventoryAvgCost.trim()
                Text(
                    text = if (avg.isNotEmpty()) {
                        "Costo de inventario (Inv.): ${formatNumberToMoney(avg)}"
                    } else {
                        "Costo de inventario: Sin promedio aún. Se usará el costo de catálogo."
                    },
                    style = labelMedium(MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                )
            }

            if (marginPercent != null) {
                Text(
                    text = "Margen: ${marginPercent.roundToInt()}%",
                    style = labelMedium(marginColor),
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }

            DMOutlinedTextField(
                text = uiState.description,
                label = stringResource(Res.string.description_optional),
                modifier = Modifier.padding(bottom = 0.dp),
                onChange = { viewModel.onDescriptionChange(it) },
                maxLines = 100,
                imeAction = ImeAction.Next,
                trailingIcon = vectorResource(Res.drawable.help),
                trailingIconClick = { showHelpDialog = true }
            )
        }

        if (uiState.inventorySectionVisible) {
            CollapsibleSectionCard(
                title = "Inventario",
                expanded = inventoryExpanded,
                onToggle = { inventoryExpanded = !inventoryExpanded },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            ) {
                Text(
                    text = if (uiState.inventoryCanConfigure) {
                        "Define si el producto controlará stock. Saldos y kardex se operan cuando está inventariable."
                    } else {
                        "Saldos, alarma de stock y costo promedio de este producto."
                    },
                    style = labelMedium(MaterialTheme.colorScheme.onSurfaceVariant),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Inventariable", style = bodyMedium(), modifier = Modifier.weight(1f))
                    Switch(
                        checked = uiState.inventoryTracked,
                        onCheckedChange = viewModel::onInventoryTrackedChange,
                        enabled = uiState.inventoryCanConfigure,
                    )
                }
                if (uiState.inventoryCanView && uiState.inventoryTracked) {
                    Text(
                        text = "Disponible: ${uiState.inventoryAvailable.ifBlank { "—" }}",
                        style = bodyMediumBold(),
                    )
                }
                if (uiState.inventoryCanConfigure) {
                    DMOutlinedTextField(
                        text = uiState.inventoryMinQty,
                        label = "Cantidad mínima",
                        modifier = Modifier.padding(bottom = 0.dp),
                        onChange = viewModel::onInventoryMinQtyChange,
                        maxLines = 1,
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    )
                    DMDropDownField(
                        label = "Stock / ventas negativas",
                        items = listOf("Según negocio", "Permitir", "No permitir"),
                        selectedIndex = uiState.inventoryNegativePolicyIndex,
                        onItemSelected = { index, _ -> viewModel.onInventoryNegativePolicyIndex(index) },
                        selectedItemToString = { it },
                    )
                    Text(
                        text = when (uiState.inventoryNegativePolicyIndex) {
                            1 -> "Puede quedar en negativo"
                            2 -> "Bloquea si no hay stock"
                            else -> "Hereda la política general"
                        },
                        style = labelMedium(MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
            }
        }

        CollapsibleSectionCard(
            title = "Información fiscal DGI",
            expanded = fiscalExpanded,
            onToggle = { fiscalExpanded = !fiscalExpanded },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            badge = { StatusBadge(text = "Opcional", color = Color(0xFFB0B0B0)) }
        ) {
            InformacionAdicionalContent(
                entries = uiState.additionalInfo,
                selectedKeyIndex = uiState.additionalSelectedKeyIndex,
                inputValue = uiState.additionalInputValue,
                onKeySelected = { idx -> viewModel.onAdditionalKeySelected(idx) },
                onValueChanged = { v -> viewModel.onAdditionalValueChanged(v) },
                onAdd = { viewModel.onAddAdditionalInfo() },
                onDelete = { idx -> viewModel.onRemoveAdditionalInfo(idx) },
                keyOptions = viewModel.additionalInfoOptions(),
                keyValueTypes = viewModel.additionalInfoValueTypes(),
                onOpenGoodsDialog = { viewModel.onOpenGoodsDialog() },
                onOpenUnitMeasureDialog = { viewModel.onOpenUnitMeasureDialog() }
            )
        }

        if (viewModel is EditItemViewModel) {
            DMDropDownField(
                label = stringResource(Res.string.category),
                items = viewModel.categories().map { category -> category.name },
                selectedIndex = viewModel.selectedCategoryIndex(),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 16.dp),
                onItemSelected = { index, _ -> viewModel.onCategoryChanged(index) },
                isError = false,
            )
        }

        if (uiState.canManageItems) {
            ButtonM(
                onClick = {
                    analytics.logEvent("edit_item")
                    viewModel.saveItem(sharedImage)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = buttonActionTitle, style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontFamily = latoFontFamily(),
                        fontWeight = FontWeight.W700,
                        letterSpacing = 0.02.sp,
                    )
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (uiState.loadingBottomSheet.isLoading()) {
        LoadingSheet(
            state = uiState.loadingBottomSheet,
            sheetState = loadingSheetState
        ) {
            viewModel.hideLoading()
        }
    }

    if (showUploadImageSheet) {
        ModalBottomSheet(
            containerColor = cardContainerColor(),
            sheetState = uploadImageSheetState,
            onDismissRequest = { showUploadImageSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButtonS(
                    label = stringResource(Res.string.select_photo_from_camera),
                    prefixIcon = rememberVectorPainter(Icons.Outlined.CameraAlt)
                ) {
                    launchCamera = true
                    scope.launch { uploadImageSheetState.hide() }.invokeOnCompletion {
                        showUploadImageSheet = false
                    }
                }

                TextButtonS(
                    label = stringResource(Res.string.select_photo_from_gallery),
                    prefixIcon = rememberVectorPainter(Icons.Outlined.Image)
                ) {
                    launchGallery = true
                    scope.launch { uploadImageSheetState.hide() }.invokeOnCompletion {
                        showUploadImageSheet = false
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            confirmButton = {
                TextButtonS(label = stringResource(Res.string.understood)) {
                    showHelpDialog = false
                }
            },
            title = {
                Text(
                    text = stringResource(Res.string.description),
                    style = headlineSmall()
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.description_optional_desc),
                    style = bodyMedium(MaterialTheme.colorScheme.onSurfaceVariant).merge(textAlign = TextAlign.Start),
                )
            },
            containerColor = cardContainerColor(),
        )
    }

    GoodsSelectorDialog(
        show = uiState.showGoodsDialog,
        goodsSegments = uiState.goodsSegments,
        selectedSegmentIndex = uiState.selectedSegmentIndex,
        selectedFamilyIndex = uiState.selectedFamilyIndex,
        onSelectSegment = { viewModel.onSelectSegment(it) },
        onSelectFamily = { viewModel.onSelectFamily(it) },
        onConfirm = { viewModel.onConfirmGoodsSelection() },
        onDismiss = { viewModel.onCloseGoodsDialog() }
    )

    UnitMeasureSelectorDialog(
        show = uiState.showUnitMeasureDialog,
        selectedIndex = uiState.selectedUnitMeasureIndex,
        onSelect = { viewModel.onSelectUnitMeasure(it) },
        onConfirm = { viewModel.onConfirmUnitMeasureSelection() },
        onDismiss = { viewModel.onCloseUnitMeasureDialog() }
    )
}

@Composable
private fun EditProductImagePicker(
    imageBitmap: ImageBitmap?,
    imageUrl: String?,
    onClick: () -> Unit
) {
    val hasRemoteImage = !imageUrl.isNullOrBlank()

    if (imageBitmap != null || hasRemoteImage) {
        Box(modifier = Modifier.padding(bottom = 8.dp)) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Product Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                )
            } else {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Product Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Fit,
                    placeholder = ColorPainter(Color.LightGray),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onClick) {
                    Icon(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(50)
                            )
                            .padding(8.dp)
                            .height(24.dp)
                            .width(24.dp),
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit Image",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    } else {
        DottedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .height(150.dp),
            onClick = onClick
        )
    }
}
