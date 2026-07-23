package com.teco.ventago.features.pos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.teco.ventago.core.camera.PermissionCallback
import com.teco.ventago.core.camera.PermissionStatus
import com.teco.ventago.core.camera.PermissionType
import com.teco.ventago.core.camera.createPermissionsManager
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.organism.CartOrganism
import com.teco.ventago.design_system.organism.PosListOrganism
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.pos.domain.models.CartLine
import com.teco.ventago.features.pos.domain.models.Tax
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.features.pos.ui.viewmodel.totalItems
import com.teco.ventago.features.pos.ui.viewmodel.FlowMode
import com.teco.ventago.features.pos.ui.viewmodel.OrderCreationStep
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.isTablet
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.BarcodeScanMode
import com.teco.ventago.utils.CameraPreview
import com.teco.ventago.utils.ScannerOverlay
import com.teco.ventago.utils.ScannerScrimWithCutout
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.playBarcodeScanBeep
import com.teco.ventago.utils.toDecimalString
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.pos_cart
import ventago.composeapp.generated.resources.pos_new_invoice
import ventago.composeapp.generated.resources.pos_new_quote
import ventago.composeapp.generated.resources.pos_update_quote

private data class PendingScannedProduct(
    val barcode: String,
    val item: Item
)

private data class LastScannerHit(
    val barcode: String,
    val mark: TimeMark
)

private val sameBarcodeScanCooldown = 500.milliseconds

@Composable
fun PosProductScreenBottomBar(backStackEntry: NavBackStackEntry?, navigate: (PosScreens) -> Unit) {
    val viewModel: PosViewModel = backStackEntry?.let {
        koinViewModel(viewModelStoreOwner = it)
    } ?: koinViewModel()

    val uiState by viewModel.uiState.collectAsState()
    val isQuoteFlow = uiState.flowMode == FlowMode.QUOTE
    val snackbarHostState = remember { SnackbarHostState() }
    var lastShownProductAddedSnackbarToken by remember {
        mutableStateOf(uiState.productAddedSnackbarToken)
    }

    LaunchedEffect(uiState.productAddedSnackbarToken) {
        val token = uiState.productAddedSnackbarToken
        if (token == 0L || token == lastShownProductAddedSnackbarToken) return@LaunchedEffect
        lastShownProductAddedSnackbarToken = token

        val result = snackbarHostState.showSnackbar(
            message = "Producto agregado",
            actionLabel = "Facturar",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            navigate(PosScreens.CartScreen)
        }
    }

    if (isTablet()) {
        val actionLabel = when {
            isQuoteFlow && uiState.quoteId != null -> stringResource(Res.string.pos_update_quote)
            isQuoteFlow -> stringResource(Res.string.pos_new_quote)
            else -> stringResource(Res.string.pos_new_invoice)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            ButtonM(
                onClick = {
                    viewModel.saveOrderCreationCheckpoint(OrderCreationStep.CART)
                    navigate(if (isQuoteFlow) PosScreens.QuoteSummaryScreen else PosScreens.PaymentScreen)
                },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                enabled = uiState.cart.isNotEmpty(),
            ) {
                Text(
                    "$actionLabel ${
                        formatNumberToMoney(
                            viewModel.legalInvoiceTotal().toDecimalString()
                        )
                    }"
                )
            }
            ProductAddedSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            ButtonM(
                onClick = {
                    navigate(PosScreens.CartScreen)
                },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                enabled = uiState.cart.isNotEmpty(),
            ) {
                Text(
                    "${stringResource(Res.string.pos_cart)} ${uiState.cart.totalItems()} items ${
                        formatNumberToMoney(
                            viewModel.legalInvoiceTotal().toDecimalString()
                        )
                    }"
                )
            }
            ProductAddedSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun ProductAddedSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { snackbarData ->
        Snackbar(
            modifier = Modifier.height(56.dp),
            action = {
                snackbarData.visuals.actionLabel?.let { actionLabel ->
                    TextButton(
                        onClick = { snackbarData.performAction() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(text = actionLabel)
                    }
                }
            }
        ) {
            Text(snackbarData.visuals.message)
        }
    }
}

@Composable
fun PosProductScreen(
    viewModel: PosViewModel,
    navigate: (PosScreens) -> Unit
) {
    var scannerMode by rememberSaveable { mutableStateOf(false) }
    var launchScannerCamera by remember { mutableStateOf(false) }
    var launchSetting by remember { mutableStateOf(false) }
    var permissionRationalDialog by remember { mutableStateOf(false) }
    val permissionsManager = createPermissionsManager(object : PermissionCallback {
        override fun onPermissionStatus(permissionType: PermissionType, status: PermissionStatus) {
            when {
                permissionType == PermissionType.CAMERA && status == PermissionStatus.GRANTED -> {
                    scannerMode = true
                }
                permissionType == PermissionType.CAMERA -> {
                    permissionRationalDialog = true
                }
                else -> Unit
            }
        }
    })

    if (launchScannerCamera) {
        if (permissionsManager.isPermissionGranted(PermissionType.CAMERA)) {
            scannerMode = true
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
            message = "Para escanear productos, otorga acceso a la cámara. Puedes administrar este permiso en la configuración del dispositivo.",
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

    if (scannerMode) {
        PosBarcodeScannerMode(
            viewModel = viewModel,
            onClose = { scannerMode = false }
        )
        return
    }

    if (isTablet()) {
        TabletPosProductScreen(
            viewModel = viewModel,
            navigate = navigate,
            onOpenScanner = { launchScannerCamera = true }
        )
    } else {
        MobilePosProductScreen(
            viewModel = viewModel,
            navigate = navigate,
            onOpenScanner = { launchScannerCamera = true }
        )
    }

}

@Composable
private fun MobilePosProductScreen(
    viewModel: PosViewModel,
    navigate: (PosScreens) -> Unit,
    onOpenScanner: () -> Unit
) {
    PosListOrganism(viewModel, Modifier, navigate, onOpenScanner)
}

@Composable
private fun TabletPosProductScreen(
    viewModel: PosViewModel,
    navigate: (PosScreens) -> Unit,
    onOpenScanner: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
        PosListOrganism(viewModel, Modifier.weight(1f), navigate, onOpenScanner)
        CartOrganism(viewModel, Modifier.weight(1f), navigate)
    }
}

@Composable
private fun PosBarcodeScannerMode(
    viewModel: PosViewModel,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingProduct by remember { mutableStateOf<PendingScannedProduct?>(null) }
    var lastScannerHit by remember { mutableStateOf<LastScannerHit?>(null) }
    var torchEnabled by rememberSaveable { mutableStateOf(false) }

    fun cancelPendingProduct() {
        pendingProduct?.let { product ->
            viewModel.removeScannedItemFromCart(product.item)
        }
        pendingProduct = null
    }

    fun handleBarcode(rawBarcode: String) {
        val barcode = rawBarcode.trim()
        if (barcode.isBlank()) {
            return
        }

        val previousHit = lastScannerHit
        if (
            previousHit != null &&
            previousHit.barcode.equals(barcode, ignoreCase = true) &&
            previousHit.mark.elapsedNow() < sameBarcodeScanCooldown
        ) {
            return
        }
        lastScannerHit = LastScannerHit(barcode = barcode, mark = TimeSource.Monotonic.markNow())

        val item = uiState.items.firstOrNull { product ->
            product.barcode?.trim()?.equals(barcode, ignoreCase = true) == true
        }

        if (item == null) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Producto no encontrado",
                    duration = SnackbarDuration.Short
                )
            }
            return
        }

        playBarcodeScanBeep()
        viewModel.addScannedItemToCart(item)
        pendingProduct = PendingScannedProduct(
            barcode = barcode,
            item = item
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val sectionHeight = maxHeight / 2

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sectionHeight)
                    .clipToBounds()
                    .background(Color.Black)
            ) {
                CameraPreview(
                    modifier = Modifier
                        .matchParentSize()
                        .clipToBounds(),
                    singleShot = false,
                    torchEnabled = torchEnabled,
                    scanMode = BarcodeScanMode.RETAIL_PRODUCT,
                    stabilityMillis = 120L,
                    requiredHits = 2,
                    tapToFocus = true,
                    centerAutoFocus = true,
                    defaultZoomRatio = 1.4f,
                    onBarcode = ::handleBarcode
                )

                ScannerScrimWithCutout(
                    modifier = Modifier.matchParentSize(),
                    cutoutWidthFraction = 0.82f,
                    cutoutAspectRatio = 2.2f
                )
                ScannerOverlay(
                    modifier = Modifier.matchParentSize(),
                    cutoutWidthFraction = 0.82f,
                    cutoutAspectRatio = 2.2f,
                    cornerColor = MaterialTheme.colorScheme.secondary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.58f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Escanear producto",
                            style = bodyMediumBold(color = Color.White)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { torchEnabled = !torchEnabled },
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (torchEnabled) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        Color.Black.copy(alpha = 0.58f)
                                    }
                                )
                        ) {
                            Icon(
                                imageVector = if (torchEnabled) {
                                    Icons.Rounded.FlashOff
                                } else {
                                    Icons.Rounded.FlashOn
                                },
                                contentDescription = if (torchEnabled) {
                                    "Apagar linterna"
                                } else {
                                    "Encender linterna"
                                },
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.58f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cerrar scanner",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            ScannerBottomPanel(
                modifier = Modifier.height(sectionHeight),
                pendingProduct = pendingProduct,
                cart = uiState.cart,
                onCancelPending = ::cancelPendingProduct
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
private fun ScannerBottomPanel(
    modifier: Modifier = Modifier,
    pendingProduct: PendingScannedProduct?,
    cart: List<CartLine>,
    onCancelPending: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (pendingProduct == null) {
            EmptyScannerProductPanel()
        } else {
            PendingScannerProductPanel(
                product = pendingProduct,
                onCancel = onCancelPending
            )
        }

        ScannerCartProductsPanel(
            cart = cart,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EmptyScannerProductPanel() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Listo para escanear",
                style = titleMediumBold(color = MaterialTheme.colorScheme.onSurface)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Apunta al código de barras del producto.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun PendingScannerProductPanel(
    product: PendingScannedProduct,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.item.name,
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.onSurface),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.barcode,
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatNumberToMoney(product.item.price.toString()),
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.primary)
                )
            }

            TextButton(onClick = onCancel) {
                Text("Cancelar")
            }
        }
    }
}

@Composable
private fun ScannerCartProductsPanel(
    cart: List<CartLine>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Productos agregados",
                style = bodyMediumBold(color = MaterialTheme.colorScheme.onSurface)
            )
            Text(
                text = "${cart.totalItems()} items",
                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (cart.isEmpty()) {
            Text(
                text = "Aún no hay productos en el carrito.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cart, key = { it.lineId }) { line ->
                ScannerCartProductRow(line)
            }
        }
    }
}

@Composable
private fun ScannerCartProductRow(line: CartLine) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = line.name,
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurface),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatNumberToMoney(line.unitPrice().toDecimalString()),
                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = scannerQuantityText(line.quantity),
            style = bodyMediumBold(color = MaterialTheme.colorScheme.primary),
            maxLines = 1
        )
    }
}

private fun scannerQuantityText(quantity: Double): String {
    val normalized = com.teco.ventago.utils.normalizeQuantity(quantity)
    val value = if (normalized % 1.0 == 0.0) {
        normalized.toInt().toString()
    } else {
        normalized.toString()
    }
    return "${value}x"
}

private fun PosViewModel.addScannedItemToCart(item: Item) {
    addItemToCart(
        item,
        tax = item.taxPercent?.let { tax ->
            Tax(
                id = tax,
                name = "$tax",
                rateBps = tax * 100
            )
        }
    )
}

private fun PosViewModel.removeScannedItemFromCart(item: Item) {
    addItemToCart(
        item,
        deltaQty = -1,
        tax = item.taxPercent?.let { tax ->
            Tax(
                id = tax,
                name = "$tax",
                rateBps = tax * 100
            )
        }
    )
}
