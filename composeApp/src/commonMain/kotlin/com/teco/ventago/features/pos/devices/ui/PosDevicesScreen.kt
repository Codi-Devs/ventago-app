@file:OptIn(ExperimentalMaterial3Api::class)

package com.teco.ventago.features.pos.devices.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.teco.ventago.core.SnackbarService
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.pos.devices.domain.model.PosDevice
import com.teco.ventago.features.pos.devices.ui.viewmodel.PosDevicePermissionKey
import com.teco.ventago.features.pos.devices.ui.viewmodel.PosDevicesState
import com.teco.ventago.features.pos.devices.ui.viewmodel.PosDevicesUiEvent
import com.teco.ventago.features.pos.devices.ui.viewmodel.PosDevicesViewModel
import com.teco.ventago.features.pos.provisioning.domain.model.PosDevicePermissions
import com.teco.ventago.utils.DateFormat
import com.teco.ventago.utils.getImageRequest
import com.teco.ventago.utils.openWhatsappMessage
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private const val H10_POS_IMAGE_URL = "https://ventago.b-cdn.net/app/h10pos.png"
private const val VENTAGO_SUPPORT_WHATSAPP = "50763879477"

@Composable
fun PosDevicesAdminScreen(
    viewModel: PosDevicesViewModel = koinViewModel<PosDevicesViewModel>(),
    onOpenDevice: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarService: SnackbarService = koinInject()

    LaunchedEffect(Unit) {
        viewModel.loadDevices(initial = true)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PosDevicesUiEvent.Message -> snackbarService.show(event.text)
            }
        }
    }

    when {
        uiState.isLoadingInitial -> PosDevicesLoading()
        uiState.devices.isEmpty() -> PosDevicesLanding()
        else -> PosDevicesList(
            uiState = uiState,
            onRefresh = { viewModel.loadDevices(initial = false) },
            onOpen = { device ->
                viewModel.selectDevice(device.deviceId)
                onOpenDevice()
            },
        )
    }
}

@Composable
fun PosDeviceAdminDetailsScreen(
    viewModel: PosDevicesViewModel = koinViewModel<PosDevicesViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarService: SnackbarService = koinInject()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val branchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PosDevicesUiEvent.Message -> snackbarService.show(event.text)
            }
        }
    }

    val device = uiState.selectedDevice
    LaunchedEffect(device?.deviceId) {
        if (device != null) {
            viewModel.loadSelectedDeviceConfig()
        }
    }

    if (device == null) {
        PosDeviceMissingSelection(onRefresh = { viewModel.loadDevices(initial = true) })
    } else if (uiState.isLoadingDeviceConfig) {
        PosDeviceDetailsLoading()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DeviceHeaderCard(
                    device = device,
                    onOpenMap = if (device.location?.hasCoordinates() == true) {
                        {
                            val lat = device.location.latitude.orEmpty()
                            val lng = device.location.longitude.orEmpty()
                            uriHandler.openUri("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                        }
                    } else {
                        null
                    }
                )
            }

            item {
                DeviceAssignmentCard(
                    state = uiState,
                    device = device,
                    canEdit = uiState.canEditBranchBilling,
                    onEdit = viewModel::openBranchBillingSheet,
                )
            }

            item {
                DeviceDataCard(device = device)
            }

            item {
                DevicePermissionsCard(
                    state = uiState,
                    onPermissionChange = viewModel::updatePermission,
                    onSave = viewModel::savePermissions,
                )
            }
        }
    }

    if (uiState.showBranchBillingSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissBranchBillingSheet,
            sheetState = branchSheetState,
            containerColor = cardContainerColor(),
        ) {
            BranchBillingSheet(
                state = uiState,
                onBranchChange = viewModel::selectBranch,
                onBillingPointChange = viewModel::selectBillingPoint,
                onSave = viewModel::saveBranchBilling,
                onDismiss = viewModel::dismissBranchBillingSheet,
            )
        }
    }

    if (uiState.loadingBottomSheet.isLoading()) {
        LoadingSheet(
            state = uiState.loadingBottomSheet,
            sheetState = loadingSheetState,
        ) {
            viewModel.hideLoading()
        }
    }
}

@Composable
private fun PosDevicesLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(shimmerBrush())
            )
        }
    }
}

@Composable
private fun PosDeviceDetailsLoading() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(listOf(120.dp, 132.dp, 116.dp, 260.dp)) { height ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(10.dp))
                    .background(shimmerBrush())
            )
        }
    }
}

@Composable
private fun PosDevicesLanding() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PosDeviceImage(
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Fit,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor())
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Administra tus dispositivos POS desde VentaGo", style = titleMediumBold())
                Text(
                    "Conecta cajas móviles Android con impresora térmica integrada, pagos compatibles y administración remota por sucursal y punto de facturación.",
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                BenefitRow("Impresión térmica integrada para tickets y facturas.")
                BenefitRow("Movilidad para vender dentro o fuera del negocio.")
                BenefitRow("Control remoto de permisos, pagos y datos de operación.")
                BenefitRow("Asignación por sucursal y punto de facturación.")
                ButtonM(
                    onClick = {
                        openWhatsappMessage(
                            VENTAGO_SUPPORT_WHATSAPP,
                            "Hola, quiero consultar disponibilidad de dispositivos POS H10 para mi negocio."
                        )
                    },
                    containerColor = Color(0xFF25D366),
                ) {
                    Text("Consultar disponibilidad por WhatsApp", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Filled.Print,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp)
        )
        Text(text, style = bodySmall())
    }
}

@Composable
private fun PosDevicesList(
    uiState: PosDevicesState,
    onRefresh: () -> Unit,
    onOpen: (PosDevice) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dispositivos activos", style = titleMediumBold())
                IconButton(onClick = onRefresh, enabled = !uiState.isRefreshing) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Recargar",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        items(uiState.devices, key = { it.deviceId }) { device ->
            PosDeviceListCard(
                state = uiState,
                device = device,
                onOpen = { onOpen(device) }
            )
        }
    }
}

@Composable
private fun PosDeviceListCard(
    state: PosDevicesState,
    device: PosDevice,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PosDeviceImage(
                contentDescription = null,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = device.displayName,
                    style = bodyMediumBold(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = device.displayModel,
                    style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = "Serie ${device.serialNumber.ifBlank { "No disponible" }}",
                    style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = "${branchNameFor(state, device)} · ${billingPointNameFor(state, device)}",
                    style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            Icon(
                Icons.Filled.ArrowForwardIos,
                contentDescription = "Ver detalles",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PosDeviceMissingSelection(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor())
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Selecciona un dispositivo", style = titleMediumBold())
                Text(
                    "No hay un dispositivo POS seleccionado para mostrar detalles.",
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                OutlinedButtonM(onClick = onRefresh) {
                    Text("Recargar dispositivos")
                }
            }
        }
    }
}

@Composable
private fun DeviceHeaderCard(
    device: PosDevice,
    onOpenMap: (() -> Unit)?,
) {
    DetailCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PosDeviceImage(
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Column(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .weight(1f)
            ) {
                Text(device.displayName, style = titleMediumBold(color = MaterialTheme.colorScheme.secondary))
                Text(device.displayModel, style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }
            if (onOpenMap != null) {
                IconButton(onClick = onOpenMap) {
                    Icon(
                        Icons.Rounded.LocationOn,
                        contentDescription = "Ver mapa",
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            StatusBadge(device.status.ifBlank { "active" })
        }
        Divider(modifier = Modifier.padding(vertical = 12.dp))
        InfoRow("Serie", device.serialNumber.ifBlank { "No disponible" })
        if (device.batteryLevel != null) {
            InfoRow("Batería", "${device.batteryLevel}%")
        }
    }
}

@Composable
private fun DeviceAssignmentCard(
    state: PosDevicesState,
    device: PosDevice,
    canEdit: Boolean,
    onEdit: () -> Unit,
) {
    DetailCard {
        SectionTitle("Asignación")
        InfoRow("Sucursal", branchNameFor(state, device))
        InfoRow("Punto de facturación", billingPointNameFor(state, device))
        Spacer(Modifier.height(12.dp))
        OutlinedButtonM(onClick = onEdit, enabled = canEdit) {
            Text("Editar sucursal y punto")
        }
        if (!canEdit) {
            Text(
                "No hay más sucursales o puntos disponibles para cambiar este dispositivo.",
                style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun DeviceDataCard(device: PosDevice) {
    DetailCard {
        SectionTitle("Datos del dispositivo")
        InfoRow("Versión de app", device.posAppVersion?.takeIf { it.isNotBlank() } ?: "No disponible")
        InfoRow("Red", device.networkType?.takeIf { it.isNotBlank() } ?: "No disponible")
        InfoRow("Conectividad", device.connectivityStatus?.takeIf { it.isNotBlank() } ?: "No disponible")
        InfoRow("Última conexión", formatDeviceDate(device.lastSeenAt), maxLines = 2)
    }
}

@Composable
private fun DevicePermissionsCard(
    state: PosDevicesState,
    onPermissionChange: (PosDevicePermissionKey, Boolean) -> Unit,
    onSave: () -> Unit,
) {
    DetailCard {
        SectionTitle("Permisos")
        val permissions = state.permissionDraft
        if (permissions == null) {
            Text(
                "El backend no envió los permisos actuales de este dispositivo. No se puede editar sin el estado actual.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            return@DetailCard
        }

        PermissionDefinitions.forEachIndexed { index, definition ->
            PermissionRow(
                title = definition.title,
                description = definition.description,
                checked = permissions.checked(definition.key),
                onCheckedChange = { onPermissionChange(definition.key, it) }
            )
            if (index < PermissionDefinitions.lastIndex) {
                Divider()
            }
        }
        Spacer(Modifier.height(12.dp))
        ButtonM(onClick = onSave, enabled = state.canEditPermissions) {
            Text("Guardar permisos")
        }
    }
}

@Composable
private fun BranchBillingSheet(
    state: PosDevicesState,
    onBranchChange: (String) -> Unit,
    onBillingPointChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Editar asignación", style = titleMediumBold())
        Text(
            "Selecciona la sucursal y el punto de facturación que usará este POS.",
            style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

        DMDropDownField(
            label = "Sucursal",
            items = state.branchOptions,
            selectedIndex = state.branchOptions.indexOfFirst { it.code == state.draftBranchCode },
            onItemSelected = { _, item -> onBranchChange(item.code) },
            selectedItemToString = { it.label },
            isError = state.branchCodeError != null
        )
        state.branchCodeError?.let {
            Text(it, style = bodySmall(color = MaterialTheme.colorScheme.error))
        }

        val billingPoints = state.selectedBranch?.billingPoints.orEmpty()
        DMDropDownField(
            label = "Punto de facturación",
            items = billingPoints,
            selectedIndex = billingPoints.indexOfFirst { it.code == state.draftBillingPointCode },
            onItemSelected = { _, item -> onBillingPointChange(item.code) },
            selectedItemToString = { it.label },
            isError = state.billingPointCodeError != null
        )
        state.billingPointCodeError?.let {
            Text(it, style = bodySmall(color = MaterialTheme.colorScheme.error))
        }

        ButtonM(
            onClick = onSave,
            enabled = state.hasValidBranchBillingDraft
        ) {
            Text("Guardar")
        }
        OutlinedButtonM(onClick = onDismiss) {
            Text("Cancelar")
        }
    }
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun PosDeviceImage(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    AsyncImage(
        model = getImageRequest(LocalPlatformContext.current, H10_POS_IMAGE_URL),
        contentDescription = contentDescription,
        placeholder = ColorPainter(Color(0xFFE5E7EB)),
        error = ColorPainter(Color(0xFFE5E7EB)),
        contentScale = contentScale,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary))
}

@Composable
private fun InfoRow(label: String, value: String, maxLines: Int = 1) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.weight(0.4f)
        )
        Text(
            value,
            style = bodyMedium(),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.6f),
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(title, style = bodyMedium())
            Text(description, style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                checkedBorderColor = MaterialTheme.colorScheme.secondary,
                checkedIconColor = MaterialTheme.colorScheme.secondary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            )
        )
    }
}

@Composable
private fun StatusBadge(status: String) {
    val active = status.equals("active", ignoreCase = true)
    val container = if (active) androidx.compose.ui.graphics.Color(0xFFE8F5E9) else androidx.compose.ui.graphics.Color(0xFFFFF3E0)
    val textColor = if (active) androidx.compose.ui.graphics.Color(0xFF2E7D32) else androidx.compose.ui.graphics.Color(0xFFFF8F00)
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = if (active) "Activo" else status.replaceFirstChar { it.uppercase() },
            style = labelSmall(color = textColor),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private data class PermissionDefinition(
    val key: PosDevicePermissionKey,
    val title: String,
    val description: String,
)

private val PermissionDefinitions = listOf(
    PermissionDefinition(PosDevicePermissionKey.EXPENSES_VIEW, "Ver gastos", "Permite consultar gastos registrados."),
    PermissionDefinition(PosDevicePermissionKey.EXPENSES_CREATE, "Crear gastos", "Permite registrar nuevos gastos."),
    PermissionDefinition(PosDevicePermissionKey.PRODUCTS_VIEW, "Ver productos", "Permite consultar catálogo e inventario."),
    PermissionDefinition(PosDevicePermissionKey.PRODUCTS_CREATE, "Crear productos", "Permite agregar o editar productos."),
    PermissionDefinition(PosDevicePermissionKey.CLIENTS_VIEW, "Ver clientes", "Permite consultar clientes."),
    PermissionDefinition(PosDevicePermissionKey.CLIENTS_CREATE, "Crear clientes", "Permite registrar o editar clientes."),
    PermissionDefinition(PosDevicePermissionKey.QUOTES_VIEW, "Ver cotizaciones", "Permite consultar cotizaciones."),
    PermissionDefinition(PosDevicePermissionKey.QUOTES_CREATE, "Crear cotizaciones", "Permite crear y modificar cotizaciones."),
    PermissionDefinition(PosDevicePermissionKey.PAYMENT_METHODS_CONFIGURE, "Configurar pagos", "Permite administrar métodos de pago."),
    PermissionDefinition(PosDevicePermissionKey.PAYMENT_YAPPY_ONSITE, "Cobro Yappy en caja", "Permite cobrar con Yappy en sitio."),
    PermissionDefinition(PosDevicePermissionKey.PAYMENT_LINK, "Link de pago", "Permite generar links de pago."),
    PermissionDefinition(PosDevicePermissionKey.PAYMENT_MANUAL_METHODS, "Pagos manuales", "Permite usar efectivo, transferencia u otros métodos manuales."),
    PermissionDefinition(PosDevicePermissionKey.REPORTS_VIEW, "Ver reportes", "Permite consultar reportes del negocio."),
)

private fun PosDevicePermissions.checked(key: PosDevicePermissionKey): Boolean =
    when (key) {
        PosDevicePermissionKey.EXPENSES_VIEW -> expensesView
        PosDevicePermissionKey.EXPENSES_CREATE -> expensesCreate
        PosDevicePermissionKey.PRODUCTS_VIEW -> productsView
        PosDevicePermissionKey.PRODUCTS_CREATE -> productsCreate
        PosDevicePermissionKey.CLIENTS_VIEW -> clientsView
        PosDevicePermissionKey.CLIENTS_CREATE -> clientsCreate
        PosDevicePermissionKey.QUOTES_VIEW -> quotesView
        PosDevicePermissionKey.QUOTES_CREATE -> quotesCreate
        PosDevicePermissionKey.PAYMENT_METHODS_CONFIGURE -> paymentMethodsConfigure
        PosDevicePermissionKey.PAYMENT_YAPPY_ONSITE -> paymentYappyOnsite
        PosDevicePermissionKey.PAYMENT_LINK -> paymentLink
        PosDevicePermissionKey.PAYMENT_MANUAL_METHODS -> paymentManualMethods
        PosDevicePermissionKey.REPORTS_VIEW -> reportsView
    }

private fun branchNameFor(state: PosDevicesState, device: PosDevice): String =
    state.branchOptions
        .firstOrNull { it.code == device.branchCode }
        ?.label
        ?.takeIf { it.isNotBlank() }
        ?: "Sucursal no disponible"

private fun billingPointNameFor(state: PosDevicesState, device: PosDevice): String =
    state.branchOptions
        .firstOrNull { it.code == device.branchCode }
        ?.billingPoints
        ?.firstOrNull { it.code == device.billingPointCode }
        ?.label
        ?.takeIf { it.isNotBlank() }
        ?: "Punto de facturación no disponible"

private fun formatDeviceDate(value: String?): String {
    val date = value?.takeIf { it.isNotBlank() } ?: return "No disponible"
    return runCatching { DateFormat.getOrdersFormattedDate(date) }.getOrElse { date }
}
