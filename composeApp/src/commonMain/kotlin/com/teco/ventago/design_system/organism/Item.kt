package com.teco.ventago.design_system.organism

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowOutward
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
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
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
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.headlineSmall
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.features.orders.ui.orders.viewmodel.OrdersUiEvent
import com.teco.ventago.features.product.domain.model.ItemTax
import com.teco.ventago.features.product.domain.model.ProductType
import com.teco.ventago.features.product.domain.model.UomRegistry
import com.teco.ventago.features.product.ui.item.add.EditModeTabs
import com.teco.ventago.features.product.ui.item.add.GoodsSelectorDialog
import com.teco.ventago.features.product.ui.item.add.InformacionAdicionalCard
import com.teco.ventago.features.product.ui.item.add.OTITaxesCard
import com.teco.ventago.features.product.ui.item.add.viewmodel.ItemEditMode
import com.teco.ventago.features.product.ui.item.add.viewmodel.ItemStateUiEvent
import com.teco.ventago.features.product.ui.item.add.viewmodel.ItemViewModel
import com.teco.ventago.features.product.ui.item.edit.EditItemViewModel
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.BarcodeScannerScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
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

@Composable
fun ItemScreenActions(backStackEntry: NavBackStackEntry?) {
    val viewModel: EditItemViewModel = backStackEntry?.let {
        koinViewModel(viewModelStoreOwner = it)
    } ?: koinViewModel()

    val uiState by viewModel.uiState.collectAsState()

    if (!uiState.showScanner) {
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
    var showHelpDialog by remember { mutableStateOf(false) }
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })
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
    }
    if (launchCamera) {
        if (permissionsManager.isPermissionGranted(PermissionType.CAMERA)) {
            cameraManager.launch()
        } else {
            permissionsManager.askPermission(PermissionType.CAMERA)
        }
    }
    if (launchSetting) {
        permissionsManager.launchSettings()
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

    Column(
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {

        EditModeTabs(
            mode = uiState.editMode,
            onChange = { viewModel.setEditMode(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (imageBitmap != null || (uiState.imgUrl != null && uiState.imgUrl != "")) {
            Box(modifier = Modifier) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = "Product Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(0.dp),
                    )
                } else {
                    AsyncImage(
                        model = uiState.imgUrl!!,
                        contentDescription = "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(0.dp),
                        contentScale = ContentScale.Fit,
                        placeholder = ColorPainter(Color.LightGray),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp)
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
        } else {
            DottedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp)
                    .height(150.dp),
                onClick = {
                    showUploadImageSheet = true
                }
            )
        }

        DMOutlinedTextField(
            text = uiState.name,
            label = stringResource(Res.string.name),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp, top = 16.dp),
            onChange = {
                // Limit to 100 characters before passing to ViewModel
                viewModel.onNameChange(it.take(100))
            },
            maxLines = 1,
            imeAction = ImeAction.Next,
            isError = uiState.wrongName,
            supportingText = if (uiState.wrongName) stringResource(Res.string.name_not_valid) else "",
        )

        DMOutlinedTextField(text = uiState.description,
            label = stringResource(Res.string.description_optional),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
            onChange = {
                viewModel.onDescriptionChange(it)
            },
            maxLines = 100,
            imeAction = ImeAction.Next,
            trailingIcon = vectorResource(Res.drawable.help),
            trailingIconClick = {
                showHelpDialog = true
            })

        DMMoneyOutlinedTextField(
            text = uiState.price.toString(),
            label = stringResource(Res.string.price),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
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

        DMMoneyOutlinedTextField(
            text = uiState.cost.toString(),
            label = "Costo (Opcional)",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
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

        DMOutlinedTextField(text = uiState.barcode,
            label = stringResource(Res.string.description_optional),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
            onChange = {
                viewModel.onBarcodeChange(it)
            },
            maxLines = 100,
            imeAction = ImeAction.Next,
            trailingIcon = Icons.Rounded.QrCodeScanner,
            trailingIconClick = {
                launchScannerCamera = true
            })

        DMOutlinedTextField(text = uiState.sku,
            label = stringResource(Res.string.description_optional),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
            onChange = {
                viewModel.onSkuChange(it)
            },
            maxLines = 100,
            imeAction = ImeAction.Done,
            trailingIcon = Icons.Outlined.ArrowOutward,
            trailingIconClick = {
                showHelpDialog = true
            })

        DMDropDownField(
            label = "Tasa ITBMS",
            items = ItemTax.defaultTaxes.map { tax -> tax.name },
            selectedIndex = ItemTax.defaultTaxes.indexOfFirst { tax -> tax.value == uiState.taxPercent},
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            onItemSelected = { index, _ -> viewModel.onTaxPercentChange(ItemTax.defaultTaxes[index].value) },
            isError = false,
        )

        DMDropDownField(
            label = "Tipo de producto",
            items = ProductType.toList().map { type -> type.description },
            selectedIndex = ProductType.toList().indexOfFirst { type -> type.typeId == uiState.productTypeId},
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            onItemSelected = { index, _ -> viewModel.onProductTypeChange(ProductType.toList()[index].typeId) },
            isError = false,
        )

        if (uiState.editMode == ItemEditMode.ADVANCED) {
            DMDropDownField(
                label = "Unidad de medida",
                items = viewModel.uomOptions(), // e.g., listOf("und","kg","m","l",...) with "und" first
                selectedIndex = viewModel.selectedUomIndex(), // should map to uiState.unitMeasureCode
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                onItemSelected = { idx, _ -> viewModel.onUomSelected(UomRegistry.all()[idx].code) },
                isError = false,
            )

            // === Tasas opcionales (ISC / OTI) ===
            DMOutlinedTextField(
                text = uiState.iscRate ?: "",
                label = "Tasa ISC (opcional)",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
                onChange = { viewModel.onIscRateChange(it) }, // expects numeric string or ""
                maxLines = 1,
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Number,
            )

            OTITaxesCard(
                otiTaxes = uiState.otiTaxes, // List<OTITaxUI>
                selectedOtiIndex = uiState.selectedOtiIndex,
                otiRateInput = uiState.otiRateInput,
                onOtiTypeSelected = { idx -> viewModel.onOtiTypeSelected(idx) },
                onOtiRateChanged = { rate -> viewModel.onOtiRateChanged(rate) },
                onAddOti = { viewModel.onAddOtiTax() },
                onDeleteOti = { idx -> viewModel.onRemoveOtiTax(idx) },
            )

            InformacionAdicionalCard(
                entries = uiState.additionalInfo, // List<AdditionalEntryUI>
                selectedKeyIndex = uiState.additionalSelectedKeyIndex, // Int
                inputValue = uiState.additionalInputValue,             // String
                onKeySelected = { idx -> viewModel.onAdditionalKeySelected(idx) },
                onValueChanged = { v -> viewModel.onAdditionalValueChanged(v) },
                onAdd = { viewModel.onAddAdditionalInfo() },
                onDelete = { idx -> viewModel.onRemoveAdditionalInfo(idx) },
                keyOptions = viewModel.additionalInfoOptions(), // List<String> built from AdditionalInfoKey enum titles
                keyValueTypes = viewModel.additionalInfoValueTypes(), // List<AdditionalValueType> aligned with options
                onOpenGoodsDialog = { viewModel.onOpenGoodsDialog() }
            )
        }

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
                    scope.launch { uploadImageSheetState.hide() }.invokeOnCompletion {
                        showUploadImageSheet = false
                    }
                }

                TextButtonS(
                    label = stringResource(Res.string.select_photo_from_gallery),
                    prefixIcon = rememberVectorPainter(Icons.Outlined.Image)
                ) {
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
}