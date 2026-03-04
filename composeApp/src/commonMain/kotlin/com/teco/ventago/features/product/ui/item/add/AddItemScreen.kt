package com.teco.ventago.features.product.ui.item.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
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
import com.teco.ventago.design_system.buttons.dashedBorder
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMMoneyOutlinedTextField
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.design_system.theme.Online
import com.teco.ventago.design_system.theme.RedLight
import com.teco.ventago.design_system.theme.WarningAmber
import com.teco.ventago.design_system.theme.badgeColorBlue
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.headlineSmall
import com.teco.ventago.design_system.theme.labelMedium
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.product.domain.model.AdditionalValueType
import com.teco.ventago.features.product.domain.model.GoodsSegment
import com.teco.ventago.features.product.domain.model.ItemTax
import com.teco.ventago.features.product.domain.model.ProductType
import com.teco.ventago.features.product.domain.model.UomRegistry
import com.teco.ventago.features.product.ui.item.add.viewmodel.AddItemViewModel
import com.teco.ventago.features.product.ui.item.add.viewmodel.AdditionalEntryUI
import com.teco.ventago.features.product.ui.item.add.viewmodel.ItemEditMode
import com.teco.ventago.features.product.ui.item.add.viewmodel.ItemStateUiEvent
import com.teco.ventago.features.product.ui.item.add.viewmodel.OTITaxUI
import com.teco.ventago.navigation.NavResults
import com.teco.ventago.utils.BarcodeScannerScreen
import com.teco.ventago.utils.formatTwoDecimals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.add
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
fun AddItemScreenActions(backStackEntry: NavBackStackEntry?) {
    val viewModel: AddItemViewModel = backStackEntry?.let {
        koinViewModel(viewModelStoreOwner = it)
    } ?: koinViewModel()

    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showScanner) {
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
fun AddItemScreen(
    backStackEntry: NavBackStackEntry? = null,
    viewModel: AddItemViewModel = koinViewModel<AddItemViewModel>(),
    navigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })

    // Check if this is a personalized product from savedStateHandle
    LaunchedEffect(backStackEntry) {
        backStackEntry?.let { entry ->
            val isPersonalized = entry.savedStateHandle.get<Boolean>(NavResults.KEY_IS_PERSONALIZED_PRODUCT) ?: false
            if (isPersonalized) {
                viewModel.setPersonalizedProductMode(true)
                // Clear the flag after reading
                entry.savedStateHandle[NavResults.KEY_IS_PERSONALIZED_PRODUCT] = null
            }
        }
    }

    // Handle events - ReturnPersonalizedProduct and GoBack
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ItemStateUiEvent.GoBack -> {
                    navigateBack()
                }
                is ItemStateUiEvent.ReturnPersonalizedProduct -> {
                    backStackEntry?.let { entry ->
                        val itemJson = Json.encodeToString(event.item)
                        entry.savedStateHandle[NavResults.KEY_PERSONALIZED_PRODUCT] = itemJson
                    }
                }
            }
        }
    }

    var showHelpDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val uploadImageSheetState = rememberModalBottomSheetState()
    var showUploadImageSheet by remember { mutableStateOf(false) }

    val analytics = koinInject<AnalyticsService>()

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
    if (launchScannerCamera) {
        if (permissionsManager.isPermissionGranted(PermissionType.CAMERA)) {
            viewModel.showScanner(!uiState.showScanner)
        } else {
            permissionsManager.askPermission(PermissionType.CAMERA)
        }
        launchScannerCamera = false
    }
    if (launchSetting) {
        permissionsManager.launchSettings()
        launchSetting = false
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

    if (uiState.showScanner) {
        Column(
            modifier = Modifier.fillMaxSize(),
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

    // Collapsible section states
    var taxesExpanded by rememberSaveable { mutableStateOf(false) }
    var identificationExpanded by rememberSaveable { mutableStateOf(false) }
    var fiscalExpanded by rememberSaveable { mutableStateOf(false) }

    // Margin calculation
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
            .verticalScroll(rememberScrollState()),
    ) {
        // ═══════════════════════════════════════════════════════
        // Card 1: Información básica (always expanded, not collapsible)
        // ═══════════════════════════════════════════════════════
        SectionCard(
            title = "Información básica",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
            badge = { StatusBadge(text = "Requerida", color = badgeColorBlue) }
        ) {
            // Image picker
            imageBitmap?.let { image ->
                Box(modifier = Modifier.padding(bottom = 8.dp)) {
                    Image(
                        bitmap = image,
                        contentDescription = "Product Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = {
                            showUploadImageSheet = true
                        }) {
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
            } ?: DottedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .height(150.dp),
                onClick = {
                    showUploadImageSheet = true
                }
            )
            DMOutlinedTextField(
                text = uiState.name,
                label = stringResource(Res.string.name),
                modifier = Modifier.padding(bottom = 0.dp),
                onChange = {
                    viewModel.onNameChange(it.take(500))
                },
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

            DMDropDownField(
                label = "Unidad de medida",
                items = viewModel.uomOptions(),
                selectedIndex = viewModel.selectedUomIndex(),
                modifier = Modifier.padding(bottom = 0.dp),
                onItemSelected = { idx, _ -> viewModel.onUomSelected(UomRegistry.all()[idx].code) },
                isError = false,
            )
        }

        // ═══════════════════════════════════════════════════════
        // Card 2: Impuestos adicionales (collapsible, starts collapsed)
        // ═══════════════════════════════════════════════════════
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

        // ═══════════════════════════════════════════════════════
        // Card 3: Identificación y control (collapsible, starts collapsed)
        // ═══════════════════════════════════════════════════════
        CollapsibleSectionCard(
            title = "Identificación y control",
            expanded = identificationExpanded,
            onToggle = { identificationExpanded = !identificationExpanded },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            badge = { StatusBadge(text = "Opcional", color = Color(0xFFB0B0B0)) }
        ) {
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

            DMDropDownField(
                label = "Tipo de producto",
                items = ProductType.toList().map { type -> type.description },
                selectedIndex = ProductType.toList().indexOfFirst { type -> type.typeId == uiState.productTypeId },
                modifier = Modifier.padding(bottom = 8.dp),
                onItemSelected = { index, _ -> viewModel.onProductTypeChange(ProductType.toList()[index].typeId) },
                isError = false,
            )

            DMMoneyOutlinedTextField(
                text = uiState.cost.toString(),
                label = "Costo (Opcional)",
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

        // ═══════════════════════════════════════════════════════
        // Card 4: Información fiscal DGI (collapsible, starts collapsed)
        // ═══════════════════════════════════════════════════════
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

        // ═══════════════════════════════════════════════════════
        // Outside cards: checkbox, category, save button
        // ═══════════════════════════════════════════════════════

        // Show checkbox for personalized products
        if (uiState.isPersonalizedProduct) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.saveProduct,
                    onCheckedChange = { viewModel.onSaveProductChange(it) }
                )
                Text(
                    text = "Guardar producto",
                    modifier = Modifier.padding(start = 8.dp),
                    style = bodyMedium()
                )
            }
        }

        // Hide category selector if it's a personalized product and save product is unchecked
        if (!(uiState.isPersonalizedProduct && !uiState.saveProduct)) {
            DMDropDownField(
                label = stringResource(Res.string.category),
                items = viewModel.categories().map { category -> category.name },
                selectedIndex = viewModel.selectedCategoryIndex(),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                onItemSelected = { index, _ -> viewModel.onCategoryChanged(index) },
                isError = false,
            )
        }

        ButtonM(
            onClick = {
                analytics.logEvent("add_item")
                viewModel.saveItem(sharedImage)
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(Res.string.add), style = TextStyle(
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
            containerColor = MaterialTheme.colorScheme.background,
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

    DMAlertDialog(
        title = "No hay categorías activas",
        message = "Para guardar un producto necesitas tener al menos una categoría activa. Ve a la gestión de productos para crear o activar una categoría.",
        show = uiState.showNoCategoryAlert,
        onDismiss = { viewModel.hideNoCategoryAlert() },
        onConfirm = { viewModel.hideNoCategoryAlert() },
        confirmText = "Entendido",
        dismissText = "Cerrar"
    )

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            confirmButton = {
                TextButtonS(label = stringResource(Res.string.understood)) {
                    showHelpDialog = false
                }
            },
            title = {
                Text(text = stringResource(Res.string.description), style = headlineSmall())
            },
            text = {
                Text(
                    text = stringResource(Res.string.description_optional_desc),
                    style = bodyMedium(MaterialTheme.colorScheme.onSurfaceVariant).merge(textAlign = TextAlign.Start),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
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

// ═══════════════════════════════════════════════════════
// Reusable card composables
// ═══════════════════════════════════════════════════════

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontSize = 12.sp,
                fontFamily = latoFontFamily(),
                fontWeight = FontWeight(600),
                textAlign = TextAlign.Center,
                color = color
            )
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    badge: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = bodyMediumBold(MaterialTheme.colorScheme.secondary),
                )
                Spacer(Modifier.weight(1f))
                badge()
            }

            Spacer(Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
private fun CollapsibleSectionCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    badge: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = bodyMediumBold(MaterialTheme.colorScheme.secondary),
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .rotate(rotation),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.weight(1f))
                badge()
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    content()
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Content composables (extracted from old standalone cards)
// ═══════════════════════════════════════════════════════

@Composable
private fun OTITaxesContent(
    otiTaxes: List<OTITaxUI>,
    selectedOtiIndex: Int,
    otiRateInput: String,
    onOtiTypeSelected: (Int) -> Unit,
    onOtiRateChanged: (String) -> Unit,
    onAddOti: () -> Unit,
    onDeleteOti: (Int) -> Unit,
) {
    val otiTypes = listOf(
        "01 - SUME911",
        "02 - Portabilidad Numérica",
        "03 - Seguro 5%",
        "04 - ATTT Seguro Autos 1%",
        "05 - Tasa Salida Aeropuerto FZ",
        "06 - Cargo Incentivo F3",
        "07 - Cargo Seguridad AH",
        "08 - Otros Cargos XT",
        "09 - Combustible YQ",
        "10 - FECI",
        "11 - Intereses"
    )

    Text("Impuestos OTI", style = bodyMedium(), modifier = Modifier.padding(bottom = 8.dp))

    DMDropDownField(
        label = "Tipo de OTI",
        items = otiTypes,
        selectedIndex = selectedOtiIndex,
        onItemSelected = { idx, _ -> onOtiTypeSelected(idx) },
        modifier = Modifier.fillMaxWidth(),
        isError = false
    )

    Spacer(Modifier.height(8.dp))

    DMOutlinedTextField(
        text = otiRateInput,
        label = "Tasa (%)",
        modifier = Modifier.fillMaxWidth(),
        onChange = onOtiRateChanged,
        maxLines = 1,
        imeAction = ImeAction.Done,
        keyboardType = KeyboardType.Number,
    )

    Spacer(Modifier.height(8.dp))

    ButtonM(
        onClick = onAddOti,
        modifier = Modifier
    ) { Text("Agregar OTI") }

    if (otiTaxes.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            otiTaxes.forEachIndexed { idx, oti ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            vanishedBackgroundColor(),
                            RoundedCornerShape(8.dp)
                        )
                        .dashedBorder(
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.secondary,
                            cornerRadiusDp = 8.dp
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(oti.name, style = bodyMedium(MaterialTheme.colorScheme.onSurface))
                        Text(
                            "${(oti.rate.toDoubleOrNull() ?: 0.0).formatTwoDecimals()}%",
                            style = bodyMedium(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    IconButton(onClick = { onDeleteOti(idx) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
                    }
                }
            }
        }
    }
}

@Composable
fun InformacionAdicionalContent(
    entries: List<AdditionalEntryUI>,
    selectedKeyIndex: Int,
    inputValue: String,
    onKeySelected: (Int) -> Unit,
    onValueChanged: (String) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Int) -> Unit,
    keyOptions: List<String>,
    keyValueTypes: List<AdditionalValueType>,
    onOpenGoodsDialog: () -> Unit,
    onOpenUnitMeasureDialog: () -> Unit
) {
    // Selector de clave
    DMDropDownField(
        label = "Seleccionar dato",
        items = keyOptions,
        selectedIndex = selectedKeyIndex,
        onItemSelected = { idx, _ -> onKeySelected(idx) },
        modifier = Modifier.fillMaxWidth(),
        isError = false
    )

    Spacer(Modifier.height(8.dp))

    // Campo dinámico según tipo
    val inputLabel = when (keyValueTypes.getOrNull(selectedKeyIndex)) {
        AdditionalValueType.DATE -> "Valor (AAAA-MM-DD)"
        AdditionalValueType.NUMBER -> "Valor numérico"
        else -> "Valor (texto)"
    }
    val kbType = when (keyValueTypes.getOrNull(selectedKeyIndex)) {
        AdditionalValueType.NUMBER -> KeyboardType.Number
        else -> KeyboardType.Text
    }

    val isGoodsServicesCodeKey = keyOptions.getOrNull(selectedKeyIndex)?.contains("Código bienes/servicios") == true
    val goodsServicesCodeExists = entries.any { it.keyName == "panama_goods_services_code" }
    val shouldShowGoodsDialogButton = keyValueTypes.getOrNull(selectedKeyIndex) == AdditionalValueType.STRING &&
        isGoodsServicesCodeKey &&
        !goodsServicesCodeExists

    val isGoodsServicesUnitKey = keyOptions.getOrNull(selectedKeyIndex)?.contains("Unidad bienes/servicios") == true
    val shouldShowUnitMeasureDialogButton = keyValueTypes.getOrNull(selectedKeyIndex) == AdditionalValueType.STRING &&
        isGoodsServicesUnitKey

    when {
        shouldShowGoodsDialogButton -> {
            TextButtonS(
                label = "Seleccionar código de bienes/servicios",
                modifier = Modifier.fillMaxWidth(),
            ) {
                onOpenGoodsDialog()
            }
        }
        shouldShowUnitMeasureDialogButton -> {
            TextButtonS(
                label = "Seleccionar unidad de medida",
                modifier = Modifier.fillMaxWidth(),
            ) {
                onOpenUnitMeasureDialog()
            }
        }
        else -> {
            DMOutlinedTextField(
                text = inputValue,
                label = inputLabel,
                modifier = Modifier.fillMaxWidth(),
                onChange = onValueChanged,
                maxLines = 1,
                imeAction = ImeAction.Done,
                keyboardType = kbType,
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    ButtonM(
        onClick = onAdd,
        modifier = Modifier
    ) { Text("Agregar") }

    if (entries.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEachIndexed { idx, e ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            vanishedBackgroundColor(),
                            RoundedCornerShape(8.dp)
                        )
                        .dashedBorder(
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.secondary,
                            cornerRadiusDp = 8.dp
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(e.title, style = bodyMedium(MaterialTheme.colorScheme.onSurface))
                        Text(e.displayValue, style = bodyMedium(MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                    IconButton(onClick = { onDelete(idx) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
                    }
                }
            }
        }
    }
}

@Composable
fun GoodsSelectorDialog(
    show: Boolean,
    goodsSegments: List<GoodsSegment>,
    selectedSegmentIndex: Int,
    selectedFamilyIndex: Int,
    onSelectSegment: (Int) -> Unit,
    onSelectFamily: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        title = { Text("Seleccionar código de bienes y servicios") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Seleccione el segmento:")
                DMDropDownField(
                    label = "Segmento",
                    items = goodsSegments.map { "${it.code} - ${it.description}" },
                    selectedIndex = selectedSegmentIndex,
                    onItemSelected = { idx, _ -> onSelectSegment(idx) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                val families = goodsSegments.getOrNull(selectedSegmentIndex)?.families ?: emptyList()
                if (families.isNotEmpty()) {
                    Text("Seleccione la familia:")
                    DMDropDownField(
                        label = "Familia",
                        items = families.map { "${it.code} - ${it.description}" },
                        selectedIndex = selectedFamilyIndex,
                        onItemSelected = { idx, _ -> onSelectFamily(idx) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    )
}

@Composable
fun UnitMeasureSelectorDialog(
    show: Boolean,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return

    val uomOptions = UomRegistry.all().map { "${it.code} - ${it.nameEs}" }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        title = { Text("Seleccionar unidad de medida") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Seleccione la unidad de medida:")
                DMDropDownField(
                    label = "Unidad de medida",
                    items = uomOptions,
                    selectedIndex = selectedIndex.coerceIn(0, uomOptions.size - 1),
                    onItemSelected = { idx, _ -> onSelect(idx) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

// ═══════════════════════════════════════════════════════
// Public composables used by Item.kt (edit screen)
// ═══════════════════════════════════════════════════════

@Composable
fun EditModeTabs(
    mode: ItemEditMode,
    onChange: (ItemEditMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(ItemEditMode.BASIC, ItemEditMode.ADVANCED)
    val labels = listOf("Básico", "Avanzado")
    TabRow(
        selectedTabIndex = modes.indexOf(mode),
        modifier = modifier,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[modes.indexOf(mode)]),
                color = MaterialTheme.colorScheme.secondary
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) {
        modes.forEachIndexed { i, m ->
            Tab(
                selected = (m == mode),
                onClick = { onChange(m) },
                text = { Text(labels[i]) }
            )
        }
    }
}

@Composable
fun OTITaxesCard(
    otiTaxes: List<OTITaxUI>,
    selectedOtiIndex: Int,
    otiRateInput: String,
    onOtiTypeSelected: (Int) -> Unit,
    onOtiRateChanged: (String) -> Unit,
    onAddOti: () -> Unit,
    onDeleteOti: (Int) -> Unit,
) {
    val otiTypes = listOf(
        "01 - SUME911",
        "02 - Portabilidad Numérica",
        "03 - Seguro 5%",
        "04 - ATTT Seguro Autos 1%",
        "05 - Tasa Salida Aeropuerto FZ",
        "06 - Cargo Incentivo F3",
        "07 - Cargo Seguridad AH",
        "08 - Otros Cargos XT",
        "09 - Combustible YQ",
        "10 - FECI",
        "11 - Intereses"
    )

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = vanishedBackgroundColor()),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Impuestos OTI", style = headlineSmall())

            Spacer(Modifier.height(8.dp))

            DMDropDownField(
                label = "Tipo de OTI",
                items = otiTypes,
                selectedIndex = selectedOtiIndex,
                onItemSelected = { idx, _ -> onOtiTypeSelected(idx) },
                modifier = Modifier.fillMaxWidth(),
                isError = false
            )

            Spacer(Modifier.height(8.dp))

            DMOutlinedTextField(
                text = otiRateInput,
                label = "Tasa (%)",
                modifier = Modifier.fillMaxWidth(),
                onChange = onOtiRateChanged,
                maxLines = 1,
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Number,
            )

            Spacer(Modifier.height(8.dp))

            ButtonM(
                onClick = onAddOti,
                modifier = Modifier.align(Alignment.End)
            ) { Text("Agregar OTI") }

            if (otiTaxes.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    otiTaxes.forEachIndexed { idx, oti ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    vanishedBackgroundColor(),
                                    RoundedCornerShape(8.dp)
                                )
                                .dashedBorder(
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    cornerRadiusDp = 8.dp
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(oti.name, style = bodyMedium(MaterialTheme.colorScheme.onSurface))
                                Text(
                                    "${(oti.rate.toDoubleOrNull() ?: 0.0).formatTwoDecimals()}%",
                                    style = bodyMedium(MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            IconButton(onClick = { onDeleteOti(idx) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InformacionAdicionalCard(
    entries: List<AdditionalEntryUI>,
    selectedKeyIndex: Int,
    inputValue: String,
    onKeySelected: (Int) -> Unit,
    onValueChanged: (String) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Int) -> Unit,
    keyOptions: List<String>,
    keyValueTypes: List<AdditionalValueType>,
    onOpenGoodsDialog: () -> Unit,
    onOpenUnitMeasureDialog: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = vanishedBackgroundColor()),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Información adicional", style = headlineSmall())

            Spacer(Modifier.height(8.dp))

            InformacionAdicionalContent(
                entries = entries,
                selectedKeyIndex = selectedKeyIndex,
                inputValue = inputValue,
                onKeySelected = onKeySelected,
                onValueChanged = onValueChanged,
                onAdd = onAdd,
                onDelete = onDelete,
                keyOptions = keyOptions,
                keyValueTypes = keyValueTypes,
                onOpenGoodsDialog = onOpenGoodsDialog,
                onOpenUnitMeasureDialog = onOpenUnitMeasureDialog
            )
        }
    }
}
