package com.teco.ventago.features.expenses.ui.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.features.expenses.domain.models.PaymentMethod
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.core.camera.PermissionCallback
import com.teco.ventago.core.camera.PermissionStatus
import com.teco.ventago.core.camera.PermissionType
import com.teco.ventago.core.camera.createPermissionsManager
import com.teco.ventago.core.camera.rememberCameraManager
import com.teco.ventago.core.camera.rememberGalleryManager
import com.teco.ventago.features.expenses.domain.ExpensesSelectionStore
import com.teco.ventago.utils.formatNumberToMoney
import kotlinx.coroutines.launch

@Composable
fun NewExpenseScreen(
    viewModel: NewExpenseViewModel,
    isEditMode: Boolean = false,
    isDuplicateMode: Boolean = false,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isManualRegistration = !uiState.isEditMode && !isDuplicateMode
    var invoiceInfoExpanded by remember(isManualRegistration) { mutableStateOf(true) }
    var issuerExpanded by remember(isManualRegistration) { mutableStateOf(!isManualRegistration) }
    var receiverExpanded by remember(isManualRegistration) { mutableStateOf(!isManualRegistration) }
    var itemsExpanded by remember(isManualRegistration) { mutableStateOf(true) }
    var notesExpanded by remember(isManualRegistration) { mutableStateOf(!isManualRegistration) }
    var fileExpanded by remember(isManualRegistration) { mutableStateOf(!isManualRegistration) }
    var initialPaymentExpanded by remember(isManualRegistration) { mutableStateOf(!isManualRegistration) }

    LaunchedEffect(Unit) {
        val selected = ExpensesSelectionStore.selected
        when {
            isEditMode && selected != null -> viewModel.initForEdit(selected)
            isDuplicateMode && selected != null -> viewModel.initForDuplicate(selected)
            else -> viewModel.initForCreate()
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Invoice info
        SectionCard(
            title = "Información de Factura",
            collapsible = isManualRegistration,
            expanded = invoiceInfoExpanded,
            onExpandedChange = { invoiceInfoExpanded = it }
        ) {
            DMOutlinedTextField(
                text = uiState.invoiceNumber,
                label = "No. Factura",
                modifier = Modifier,
                onChange = { viewModel.setInvoiceNumber(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DMOutlinedTextField(
                text = uiState.cufe,
                label = "CUFE (opcional)",
                modifier = Modifier,
                onChange = { viewModel.setCufe(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DMOutlinedTextField(
                text = uiState.emissionDate,
                label = "Fecha de emisión (YYYY-MM-DD)",
                modifier = Modifier,
                onChange = { viewModel.setEmissionDate(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            PaymentMethodSelector(
                selected = uiState.paymentMethod,
                onSelect = { viewModel.setPaymentMethod(it) }
            )
        }

        // Issuer
        SectionCard(
            title = "Emisor",
            collapsible = isManualRegistration,
            expanded = issuerExpanded,
            onExpandedChange = { issuerExpanded = it }
        ) {
            DMOutlinedTextField(
                text = uiState.issuerName,
                label = "Nombre del emisor",
                modifier = Modifier,
                onChange = { viewModel.setIssuerName(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DMOutlinedTextField(
                text = uiState.issuerRuc,
                label = "RUC",
                modifier = Modifier,
                onChange = { viewModel.setIssuerRuc(it) }
            )
        }

        // Receiver
        SectionCard(
            title = "Receptor",
            collapsible = isManualRegistration,
            expanded = receiverExpanded,
            onExpandedChange = { receiverExpanded = it }
        ) {
            DMOutlinedTextField(
                text = uiState.receiverName,
                label = "Nombre del receptor",
                modifier = Modifier,
                onChange = { viewModel.setReceiverName(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DMOutlinedTextField(
                text = uiState.receiverRuc,
                label = "RUC",
                modifier = Modifier,
                onChange = { viewModel.setReceiverRuc(it) }
            )
        }

        // Items
        SectionCard(
            title = "Artículos",
            collapsible = isManualRegistration,
            expanded = itemsExpanded,
            onExpandedChange = { itemsExpanded = it }
        ) {
            uiState.items.forEachIndexed { index, item ->
                if (index > 0) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
                ItemEditor(
                    item = item,
                    index = index,
                    canRemove = uiState.items.size > 1,
                    onUpdate = { viewModel.updateItem(index, it) },
                    onRemove = { viewModel.removeItem(index) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButtonS(label = "+ Agregar artículo") {
                viewModel.addItem()
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // Totals
            TotalsRow("Subtotal", formatNumberToMoney("${viewModel.getSubtotal()}"))
            TotalsRow("ITBMS", formatNumberToMoney("${viewModel.getItbmsTotal()}"))
            TotalsRow("Total", formatNumberToMoney("${viewModel.getTotalAmount()}"), bold = true)
        }

        // Notes
        SectionCard(
            title = "Notas",
            collapsible = isManualRegistration,
            expanded = notesExpanded,
            onExpandedChange = { notesExpanded = it }
        ) {
            DMOutlinedTextField(
                text = uiState.notes,
                label = "Notas (opcional)",
                modifier = Modifier,
                onChange = { viewModel.setNotes(it) }
            )
        }

        // Invoice file upload (beta: expenses_qr)
        if (uiState.hasExpensesQr) {
            SectionCard(
                title = "Archivo de Factura (opcional)",
                collapsible = isManualRegistration,
                expanded = fileExpanded,
                onExpandedChange = { fileExpanded = it }
            ) {
                FileUploadCard(
                    fileUrl = uiState.fileUrl,
                    isUploading = uiState.isUploadingFile,
                    onFileSelected = { viewModel.uploadFile(it) },
                    onRemove = { viewModel.removeFile() }
                )
            }
        }

        // Initial payment (create mode only)
        if (!uiState.isEditMode) {
            SectionCard(
                title = "Pago Inicial (opcional)",
                collapsible = isManualRegistration,
                expanded = initialPaymentExpanded,
                onExpandedChange = { initialPaymentExpanded = it }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.includePayment,
                        onCheckedChange = { viewModel.setIncludePayment(it) }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Incluir pago inicial", style = bodyMedium())
                }

                if (uiState.includePayment) {
                    Spacer(modifier = Modifier.height(8.dp))
                    InitialPaymentMethodSelector(
                        selected = uiState.paymentMethodForPayment,
                        onSelect = { viewModel.setPaymentMethodForPayment(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DMOutlinedTextField(
                        text = uiState.paymentAmount,
                        label = "Monto del pago",
                        modifier = Modifier,
                        onChange = { viewModel.setPaymentAmount(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.paymentMethodForPayment == "credit") {
                        DMOutlinedTextField(
                            text = uiState.paymentDueDate,
                            label = "Fecha de vencimiento (YYYY-MM-DD)",
                            modifier = Modifier,
                            onChange = { viewModel.setPaymentDueDate(it) }
                        )
                    } else {
                        DMOutlinedTextField(
                            text = uiState.paymentDate,
                            label = "Fecha de pago (YYYY-MM-DD)",
                            modifier = Modifier,
                            onChange = { viewModel.setPaymentDate(it) }
                        )
                    }
                }
            }
        }

        // Error
        uiState.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = bodyMedium()
            )
        }

        // Submit
        ButtonM(
            onClick = { viewModel.submit() },
            enabled = !uiState.isSubmitting
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                if (uiState.isEditMode) "Actualizar Gasto" else "Registrar Gasto"
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionCard(
    title: String,
    collapsible: Boolean = false,
    expanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = collapsible) { onExpandedChange(!expanded) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, style = titleMediumBold())
                if (collapsible) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (expanded) "Colapsar" else "Expandir",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (!collapsible || expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun ItemEditor(
    item: EditableExpenseItem,
    index: Int,
    canRemove: Boolean,
    onUpdate: (EditableExpenseItem) -> Unit,
    onRemove: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Artículo ${index + 1}",
                style = bodyMediumBold()
            )
            if (canRemove) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        DMOutlinedTextField(
            text = item.description,
            label = "Descripción",
            modifier = Modifier,
            onChange = { onUpdate(item.copy(description = it)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DMOutlinedTextField(
                text = item.quantity,
                label = "Cant.",
                modifier = Modifier.weight(1f),
                onChange = { onUpdate(item.copy(quantity = it)) }
            )
            DMOutlinedTextField(
                text = item.unitPrice,
                label = "Precio unit.",
                modifier = Modifier.weight(1f),
                onChange = { onUpdate(item.copy(unitPrice = it)) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DMOutlinedTextField(
                text = item.discountAmount,
                label = "Descuento",
                modifier = Modifier.weight(1f),
                onChange = { onUpdate(item.copy(discountAmount = it)) }
            )
            DMOutlinedTextField(
                text = item.itbmsAmount,
                label = "ITBMS",
                modifier = Modifier.weight(1f),
                onChange = { onUpdate(item.copy(itbmsAmount = it)) }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Total: ${formatNumberToMoney("${item.totalValue}")}",
                style = bodyMediumBold()
            )
        }
    }
}

@Composable
private fun TotalsRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (bold) bodyMediumBold() else labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
            text = value,
            style = if (bold) bodyMediumBold() else bodyMedium()
        )
    }
}

@Composable
private fun PaymentMethodSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    val allMethods = remember { PaymentMethod.getAllMethods() }
    val selectedIndex = allMethods.indexOfFirst { it.value == selected }.takeIf { it >= 0 } ?: -1

    DMDropDownField(
        modifier = Modifier.fillMaxWidth(),
        label = "Método de pago",
        notSetLabel = "Seleccionar método",
        items = allMethods,
        selectedIndex = selectedIndex,
        onItemSelected = { _, item ->
            onSelect(item.value)
        },
        selectedItemToString = { it.label }
    )
}

@Composable
private fun InitialPaymentMethodSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    val allMethods = remember { PaymentMethod.getAllMethods() }
    val selectedIndex = allMethods.indexOfFirst { it.value == selected }.takeIf { it >= 0 } ?: 0

    DMDropDownField(
        modifier = Modifier.fillMaxWidth(),
        label = "Método de pago",
        items = allMethods,
        selectedIndex = selectedIndex,
        onItemSelected = { _, item ->
            onSelect(item.value)
        },
        selectedItemToString = { it.label }
    )
}

@Composable
private fun FileUploadCard(
    fileUrl: String?,
    isUploading: Boolean,
    onFileSelected: (com.teco.ventago.core.camera.SharedImage) -> Unit,
    onRemove: () -> Unit
) {
    var launchCamera by remember { mutableStateOf(false) }
    var launchGallery by remember { mutableStateOf(false) }
    var launchSetting by remember { mutableStateOf(false) }

    val permissionsManager = createPermissionsManager(object : PermissionCallback {
        override fun onPermissionStatus(permissionType: PermissionType, status: PermissionStatus) {
            when (status) {
                PermissionStatus.GRANTED -> when (permissionType) {
                    PermissionType.CAMERA -> launchCamera = true
                    PermissionType.GALLERY -> launchGallery = true
                }
                else -> {}
            }
        }
    })

    val scope = rememberCoroutineScope()
    val cameraManager = rememberCameraManager { image ->
        scope.launch { if (image != null) onFileSelected(image) }
    }
    val galleryManager = rememberGalleryManager { image ->
        scope.launch { if (image != null) onFileSelected(image) }
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

    Column {
        if (isUploading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Subiendo archivo...", style = bodyMedium())
            }
        } else if (fileUrl != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Archivo adjunto", style = bodyMedium())
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButtonM(
                    onClick = { launchCamera = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cámara")
                }
                OutlinedButtonM(
                    onClick = { launchGallery = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Galería")
                }
            }
        }
    }
}
