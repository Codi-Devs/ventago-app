package com.teco.ventago.features.expenses.ui.create

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Description
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.molecules.InstallmentDueDateFieldKmp
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.core.file.SharedFile
import com.teco.ventago.core.file.rememberDocumentPickerManager
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
import com.teco.ventago.features.expenses.ui.components.ExpenseAccountSelectorField
import com.teco.ventago.utils.formatNumberToMoney
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewExpenseScreen(
    viewModel: NewExpenseViewModel,
    isEditMode: Boolean = false,
    isDuplicateMode: Boolean = false,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = remember { currentLocalDate() }
    val tomorrow = remember(today) { today.plus(DatePeriod(days = 1)) }
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
            RequiredLabel("No. Factura")
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
            RequiredLabel("Fecha de emisión")
            InstallmentDueDateFieldKmp(
                valueIso = uiState.emissionDate,
                onDatePickedIso = { viewModel.setEmissionDate(it) },
                label = "Fecha de emisión",
                modifier = Modifier,
                maxSelectableDate = today
            )
        }

        // Issuer
        SectionCard(
            title = "Emisor",
            collapsible = isManualRegistration,
            expanded = issuerExpanded,
            onExpandedChange = { issuerExpanded = it }
        ) {
            RequiredLabel("Nombre del emisor")
            Column {
                DMOutlinedTextField(
                    text = uiState.issuerName,
                    label = "Nombre del emisor",
                    modifier = Modifier,
                    onChange = { viewModel.setIssuerName(it) }
                )
                Text(
                    text = "Escribe el nombre para ver proveedores existentes y autocompletar.",
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                )
                if (uiState.merchantSuggestions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            uiState.merchantSuggestions.forEachIndexed { index, merchant ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectMerchant(merchant) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Store,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = merchant.name,
                                            style = bodyMedium(),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!merchant.ruc.isNullOrBlank()) {
                                            Text(
                                                text = "RUC: ${merchant.ruc}${if (!merchant.dv.isNullOrBlank()) "-${merchant.dv}" else ""}",
                                                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                if (index < uiState.merchantSuggestions.lastIndex) {
                                    Divider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            DMOutlinedTextField(
                text = uiState.issuerRuc,
                label = "RUC",
                modifier = Modifier,
                onChange = { viewModel.setIssuerRuc(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DMOutlinedTextField(
                text = uiState.issuerDv,
                label = "DV",
                modifier = Modifier,
                onChange = { viewModel.setIssuerDv(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DMOutlinedTextField(
                text = uiState.issuerAddress,
                label = "Dirección (opcional)",
                modifier = Modifier,
                onChange = { viewModel.setIssuerAddress(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DMOutlinedTextField(
                text = uiState.issuerPhone,
                label = "Teléfono (opcional)",
                modifier = Modifier,
                onChange = { viewModel.setIssuerPhone(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.saveMerchant,
                    onCheckedChange = { viewModel.setSaveMerchant(it) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guardar emisor para próximos gastos", style = bodyMedium())
            }
        }

        // Receiver
        SectionCard(
            title = "Receptor",
            collapsible = isManualRegistration,
            expanded = receiverExpanded,
            onExpandedChange = { receiverExpanded = it }
        ) {
            RequiredLabel("Nombre del receptor")
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
            title = "Items",
            collapsible = isManualRegistration,
            expanded = itemsExpanded,
            onExpandedChange = { itemsExpanded = it }
        ) {
            ExpenseConceptSection(
                uiState = uiState,
                onDefaultAccountSelected = viewModel::setDefaultExpenseAccount,
                onApplyConceptPerItemChange = viewModel::setApplyConceptPerItem,
                onApplyDefaultToAll = viewModel::applyDefaultConceptToAllItems
            )

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            uiState.items.forEachIndexed { index, item ->
                if (index > 0) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
                ItemEditor(
                    item = item,
                    index = index,
                    canRemove = uiState.items.size > 1,
                    showConceptSelector = uiState.applyConceptPerItem,
                    expenseAccounts = uiState.expenseAccounts,
                    expenseAccountsLoading = uiState.expenseAccountsLoading,
                    onUpdate = { viewModel.updateItem(index, it) },
                    onExpenseAccountSelected = { accountId, accountName ->
                        viewModel.setItemExpenseAccount(index, accountId, accountName)
                    },
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
            TotalsRow("Total", formatNumberToMoney("${viewModel.getTotalAmount()}"), bold = true, useSecondaryColor = true)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "Gratis por tiempo limitado",
                        style = labelSmall(color = Color.White),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FileUploadCard(
                    fileUrl = uiState.fileUrl,
                    selectedLocalFileName = uiState.localFileName,
                    isUploading = uiState.isUploadingFile,
                    onFileSelected = { viewModel.uploadFile(it) },
                    onRemove = { viewModel.removeFile() }
                )
            }
        }

        // Initial payments (create mode only)
        if (!uiState.isEditMode) {
            SectionCard(
                title = "Pagos Iniciales (opcional)",
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
                    Text("Incluir pagos iniciales", style = bodyMedium())
                }

                if (uiState.includePayment) {
                    uiState.initialPayments.forEachIndexed { index, payment ->
                        if (index > 0) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        InitialPaymentEditor(
                            payment = payment,
                            index = index,
                            canRemove = uiState.initialPayments.size > 1,
                            today = today,
                            tomorrow = tomorrow,
                            hasExpensesQr = uiState.hasExpensesQr,
                            onUpdate = { viewModel.updateInitialPayment(index, it) },
                            onRemove = { viewModel.removeInitialPayment(index) },
                            onFileSelected = { viewModel.uploadInitialPaymentProofAt(index, it) },
                            onRemoveProof = { viewModel.removeInitialPaymentProofAt(index) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButtonS(label = "+ Agregar pago") {
                        viewModel.addInitialPayment()
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
        uiState.conceptValidationError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = bodyMedium()
            )
        }

        // Submit
        ButtonM(
            onClick = { viewModel.submit() },
            enabled = !uiState.isSubmitting,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                if (uiState.isEditMode) "Actualizar Gasto" else "Registrar Gasto"
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (uiState.loadingBottomSheet.isLoading()) {
        LoadingSheet(
            state = uiState.loadingBottomSheet,
            sheetState = loadingSheetState
        ) {
            viewModel.hideLoading()
        }
    }
}

@Composable
private fun ExpenseConceptSection(
    uiState: NewExpenseState,
    onDefaultAccountSelected: (Long?, String?) -> Unit,
    onApplyConceptPerItemChange: (Boolean) -> Unit,
    onApplyDefaultToAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Concepto de gasto",
            style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        ExpenseAccountSelectorField(
            label = "Concepto de gasto (factura completa)",
            selectedText = uiState.defaultExpenseAccountName.orEmpty(),
            placeholder = "Sin concepto de gasto",
            accounts = uiState.expenseAccounts,
            isLoading = uiState.expenseAccountsLoading,
            leafOnly = true,
            emptyOptionLabel = "Sin concepto de gasto",
            hint = if (uiState.applyConceptPerItem) {
                "Selecciona un concepto y aplicalo a todos los items, o asigna uno distinto por linea."
            } else {
                "Se aplicara al gasto completo cuando no uses asignacion por item."
            },
            onSelected = onDefaultAccountSelected
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.applyConceptPerItem,
                onCheckedChange = onApplyConceptPerItemChange
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Aplicar concepto por item", style = bodyMedium())
        }

        if (uiState.applyConceptPerItem) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButtonS(label = "Aplicar mismo concepto a todos los items") {
                onApplyDefaultToAll()
            }
        }
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
    showConceptSelector: Boolean,
    expenseAccounts: List<com.teco.ventago.features.expenses.domain.models.ExpenseAccount>,
    expenseAccountsLoading: Boolean,
    onUpdate: (EditableExpenseItem) -> Unit,
    onExpenseAccountSelected: (Long?, String?) -> Unit,
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

        RequiredLabel("Descripción")
        DMOutlinedTextField(
            text = item.description,
            label = "Descripción",
            modifier = Modifier,
            onChange = { onUpdate(item.copy(description = it)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (showConceptSelector) {
            ExpenseAccountSelectorField(
                label = "Concepto",
                selectedText = item.expenseAccountName.orEmpty(),
                placeholder = "Sin concepto",
                accounts = expenseAccounts,
                isLoading = expenseAccountsLoading,
                leafOnly = true,
                emptyOptionLabel = "Sin concepto",
                hint = null,
                onSelected = onExpenseAccountSelected
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                RequiredLabel("Cant.")
                DMOutlinedTextField(
                    text = item.quantity,
                    label = "Cant.",
                    modifier = Modifier,
                    onChange = { onUpdate(item.copy(quantity = it)) }
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                RequiredLabel("Precio unit.")
                DMOutlinedTextField(
                    text = item.unitPrice,
                    label = "Precio unit.",
                    modifier = Modifier,
                    onChange = { onUpdate(item.copy(unitPrice = it)) }
                )
            }
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
private fun TotalsRow(label: String, value: String, bold: Boolean = false, useSecondaryColor: Boolean = false) {
    val color = when {
        useSecondaryColor -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (bold) bodyMediumBold(color = color) else labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
            text = value,
            style = if (bold) bodyMediumBold(color = color) else bodyMedium()
        )
    }
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
    selectedLocalFileName: String?,
    isUploading: Boolean,
    onFileSelected: (SharedFile) -> Unit,
    onRemove: () -> Unit
) {
    var launchCamera by remember { mutableStateOf(false) }
    var launchGallery by remember { mutableStateOf(false) }
    var launchDocument by remember { mutableStateOf(false) }
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
        scope.launch {
            val bytes = image?.toByteArray()
            if (bytes != null) {
                onFileSelected(
                    SharedFile(
                        bytes = bytes,
                        fileName = "expense_${com.teco.ventago.utils.randomUUID()}.jpg",
                        contentType = "image/jpeg"
                    )
                )
            }
        }
    }
    val galleryManager = rememberGalleryManager { image ->
        scope.launch {
            val bytes = image?.toByteArray()
            if (bytes != null) {
                onFileSelected(
                    SharedFile(
                        bytes = bytes,
                        fileName = "expense_${com.teco.ventago.utils.randomUUID()}.jpg",
                        contentType = "image/jpeg"
                    )
                )
            }
        }
    }
    val documentManager = rememberDocumentPickerManager(
        acceptedMimeTypes = listOf("application/pdf")
    ) { file ->
        scope.launch {
            if (file != null) {
                onFileSelected(file)
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
    if (launchDocument) {
        documentManager.launch()
        launchDocument = false
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
        } else if (fileUrl != null || selectedLocalFileName != null) {
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
                    Text(selectedLocalFileName ?: "Archivo adjunto", style = bodyMedium())
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
                        imageVector = Icons.Rounded.CameraAlt,
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
                        imageVector = Icons.Rounded.Collections,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Galería")
                }
                OutlinedButtonM(
                    onClick = { launchDocument = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF")
                }
            }
        }
    }
}

@Composable
private fun RequiredLabel(fieldName: String) {
    Row(modifier = Modifier.padding(bottom = 2.dp)) {
        Text(
            text = "* ",
            style = labelSmall(color = MaterialTheme.colorScheme.secondary)
        )
        Text(
            text = fieldName,
            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@Composable
private fun InitialPaymentEditor(
    payment: EditableInitialPayment,
    index: Int,
    canRemove: Boolean,
    today: LocalDate,
    tomorrow: LocalDate,
    hasExpensesQr: Boolean,
    onUpdate: (EditableInitialPayment) -> Unit,
    onRemove: () -> Unit,
    onFileSelected: (com.teco.ventago.core.file.SharedFile) -> Unit,
    onRemoveProof: () -> Unit
) {
    val allMethods = remember { PaymentMethod.getAllMethods() }
    val selectedIndex = allMethods.indexOfFirst { it.value == payment.paymentMethod }.takeIf { it >= 0 } ?: 0

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pago ${index + 1}", style = bodyMediumBold())
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
        DMDropDownField(
            modifier = Modifier.fillMaxWidth(),
            label = "Método de pago",
            items = allMethods,
            selectedIndex = selectedIndex,
            onItemSelected = { _, item ->
                onUpdate(payment.copy(paymentMethod = item.value))
            },
            selectedItemToString = { it.label }
        )

        Spacer(modifier = Modifier.height(8.dp))
        DMOutlinedTextField(
            text = payment.amount,
            label = "Monto del pago",
            modifier = Modifier,
            onChange = { onUpdate(payment.copy(amount = it)) }
        )

        Spacer(modifier = Modifier.height(8.dp))
        if (payment.paymentMethod == "credit") {
            InstallmentDueDateFieldKmp(
                valueIso = payment.dueDate,
                onDatePickedIso = { onUpdate(payment.copy(dueDate = it)) },
                label = "Fecha de vencimiento",
                modifier = Modifier,
                minSelectableDate = tomorrow
            )
        } else {
            InstallmentDueDateFieldKmp(
                valueIso = payment.paymentDate,
                onDatePickedIso = { onUpdate(payment.copy(paymentDate = it)) },
                label = "Fecha de pago",
                modifier = Modifier,
                maxSelectableDate = today
            )
            if (hasExpensesQr) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Comprobante de pago (opcional)",
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(6.dp))
                FileUploadCard(
                    fileUrl = null,
                    selectedLocalFileName = payment.proofFileName,
                    isUploading = false,
                    onFileSelected = onFileSelected,
                    onRemove = onRemoveProof
                )
            }
        }
    }
}

private fun currentLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
