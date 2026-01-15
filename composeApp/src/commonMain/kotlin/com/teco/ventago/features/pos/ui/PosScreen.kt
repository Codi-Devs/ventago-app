package com.teco.ventago.features.pos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.molecules.customer.PosCustomerSelection
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.design_system.theme.bodyLarge
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.labelMedium
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.pos.ui.viewmodel.PosState
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.features.pos.ui.viewmodel.FlowMode
import com.teco.ventago.features.quotes.domain.QuoteSelectionStore
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.DateFormat

@Composable
fun PosScreen(
    viewModel: PosViewModel,
    navigate: (PosScreens) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isQuoteFlow = uiState.flowMode == FlowMode.QUOTE

    LaunchedEffect(Unit) {
        if (QuoteSelectionStore.startQuoteFlow) {
            viewModel.setFlowMode(FlowMode.QUOTE, quoteId = QuoteSelectionStore.selected?.id)
            QuoteSelectionStore.startQuoteFlow = false
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    )
    {

        Spacer(modifier = Modifier.height(16.dp))

        if (!isQuoteFlow) {
            // === Branch ===
            if (uiState.branches.isNotEmpty()) {
                DMDropDownField(
                    label = "Sucursal",
                    items = uiState.branches.map { it.name },
                    selectedIndex = uiState.selectedBranchIndex,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    onItemSelected = { idx, _ -> viewModel.onBranchSelected(idx) },
                    isError = false,
                    enabled = uiState.branches.size > 1,
                )
            }

            // === Billing Point (depends on branch) ===
            if (uiState.billingPoints.isNotEmpty()) {
                DMDropDownField(
                    label = "Punto de facturación",
                    items = uiState.billingPoints.map { it.description },
                    selectedIndex = uiState.selectedBillingPointIndex,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    onItemSelected = { idx, _ -> viewModel.onBillingPointSelected(idx) },
                    isError = false,
                    enabled = uiState.billingPoints.size > 1,
                )
            }

            // === Doc Type ===
            DMDropDownField(
                label = "Tipo de factura",
                items = viewModel.docTypeOptions(),              // e.g., ["01 - Factura de Operación Interna", ...]
                selectedIndex = uiState.selectedDocTypeIndex,    // default points to "01"
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                onItemSelected = { idx, _ -> viewModel.onDocTypeSelected(idx) },
                isError = false,
                enabled = uiState.enabledSelectionDocType
            )

            // === Operation Nature ===
            DMDropDownField(
                label = "Naturaleza de la operación",
                items = viewModel.operationNatureOptions(),           // e.g., ["01 - Venta", "02 - Exportación", ...]
                selectedIndex = uiState.selectedOperationNatureIndex, // default to "01"
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                onItemSelected = { idx, _ -> viewModel.onOperationNatureSelected(idx) },
                isError = false,
                enabled = uiState.enabledOperationNature
            )
        }

        if (uiState.referencedNoteCUFE.isNotBlank()) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = vanishedBackgroundColor()
                ),
            ) {
                Text("CUFE Referenciado: ${uiState.referencedNoteCUFE}", style = bodySmall(), modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp))
                Text("Creado en: ${DateFormat.getOrdersFormattedDate(uiState.referencedCreatedAt)}",style = bodySmall(), modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp))
            }
        }


        // === Customer selector ===
        CustomerSelectorCard(
            uiState = uiState,
            viewModel = viewModel,
            onPickCustomer = { viewModel.onPickCustomerClick() }, // open your customer picker flow
            onFinalToggle = { isFinal -> viewModel.onFinalCustomerToggle(isFinal) },
            onFinalName = { viewModel.onFinalNameChanged(it) },
            onFinalEmail = { viewModel.onFinalEmailChanged(it) },
            onFinalPhone = { viewModel.onFinalPhoneChanged(it) },
            onIdType = { idx -> viewModel.onFinalIdTypeSelected(idx) },   // "cedula" | "passport" | "foreing_taxid"
            onIdNumber = { viewModel.onFinalIdNumberChanged(it) },
            onPassportCountry = { viewModel.onFinalPassportCountryChanged(it) },
            navigate = navigate
        )

        Spacer(modifier = Modifier.weight(1f))

        ButtonM(
            onClick = {
                navigate(PosScreens.POSProductScreen)
            },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Text(
                "Siguiente"
            )
        }

    }

}

@Composable
private fun ReadOnlyInfoRow(label: String, value: String) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = label,
            style = labelMedium(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = bodyLarge())
    }
}


@Composable
private fun CustomerSelectorCard(
    uiState: PosState, // or PosState in your codebase
    viewModel: PosViewModel,
    onPickCustomer: () -> Unit,
    onFinalToggle: (Boolean) -> Unit,
    onFinalName: (String) -> Unit,
    onFinalEmail: (String) -> Unit,
    onFinalPhone: (String) -> Unit,
    onIdType: (Int) -> Unit,
    onIdNumber: (String) -> Unit,
    onPassportCountry: (String) -> Unit,
    navigate: (PosScreens) -> Unit
) {
    // local state only for collapsing the optional fields (persist across rotations)
    var extraOpen by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.padding(16.dp)) {
        Text("Cliente", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(8.dp))

        // Switch between Registered vs Final Consumer
        val selectedIndex = if (uiState.finalCustomer) 0 else 1
        TabRow(
            selectedTabIndex = selectedIndex,
            modifier = Modifier,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = MaterialTheme.colorScheme.secondary
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Tab(
                selected = uiState.finalCustomer,
                onClick = {
                    onFinalToggle(true)
                    extraOpen = false
                },
                text = { Text("Consumidor Final") },
                enabled = uiState.enabledSelectionDocType
            )

            Tab(
                selected = !uiState.finalCustomer,
                onClick = {
                    extraOpen = false
                    onFinalToggle(false)
                },
                text = { Text("Cliente Registrado") },
                enabled = uiState.enabledSelectionDocType
            )
        }

        Spacer(Modifier.height(12.dp))

        if (!uiState.finalCustomer) {
            // Registered customer flow
            PosCustomerSelection(
                customer = uiState.customer
            ) { screen ->
                if (uiState.enabledSelectionDocType) {
                    navigate(screen)
                }
            }

        } else {
            // Final consumer — show a compact “summary row” and a collapsible for extra data
            // Summary (always visible)
            Column {
                Text(
                    text = uiState.finalName?.takeIf { it.isNotBlank() } ?: "Consumidor Final",
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!uiState.finalEmail.isNullOrBlank()) {
                    Text(
                        uiState.finalEmail!!,
                        style = bodyMedium(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!uiState.finalPhone.isNullOrBlank()) {
                    Text(
                        uiState.finalPhone!!,
                        style = bodyMedium(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            AdditionalInfoCollapsibleCard(
                expanded = extraOpen,
                onToggle = { extraOpen = !extraOpen },
                finalName = uiState.finalName,
                finalEmail = uiState.finalEmail,
                finalPhone = uiState.finalPhone,
                finalIdTypeIndex = uiState.finalIdTypeIndex,
                finalIdType = uiState.finalIdType,
                finalIdNumber = uiState.finalIdNumber,
                finalPassportCountry = uiState.finalPassportCountry,
                idTypeDisplayNames = viewModel.finalIdTypeDisplayNames(),
                onFinalName = onFinalName,
                onFinalEmail = onFinalEmail,
                onFinalPhone = onFinalPhone,
                onIdType = onIdType,
                onIdNumber = onIdNumber,
                onPassportCountry = onPassportCountry
            )
        }
    }
}

@Composable
private fun AdditionalInfoCollapsibleCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    // state + handlers
    finalName: String?,
    finalEmail: String?,
    finalPhone: String?,
    finalIdTypeIndex: Int,
    finalIdType: String,             // "cedula" | "passport" | "foreing_taxid"
    finalIdNumber: String?,
    finalPassportCountry: String?,
    idTypeDisplayNames: List<String>,
    onFinalName: (String) -> Unit,
    onFinalEmail: (String) -> Unit,
    onFinalPhone: (String) -> Unit,
    onIdType: (Int) -> Unit,
    onIdNumber: (String) -> Unit,
    onPassportCountry: (String) -> Unit,
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = vanishedBackgroundColor())
            .animateContentSize(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = vanishedBackgroundColor()
        ),
    ) {
        // Header row (always visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("Información adicional", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (expanded) "Ocultar detalles" else "Opcional: nombre, contacto e identificación",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.rotate(rotation)
            )
        }

        // Body (only when expanded)
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                DMOutlinedTextField(
                    text = finalName ?: "",
                    label = "Nombre (opcional)",
                    modifier = Modifier.padding(vertical = 6.dp),
                    onChange = onFinalName,
                    maxLines = 1,
                    imeAction = ImeAction.Next
                )
                DMOutlinedTextField(
                    text = finalEmail ?: "",
                    label = "Email (opcional)",
                    modifier = Modifier.padding(vertical = 6.dp),
                    onChange = onFinalEmail,
                    maxLines = 1,
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Email
                )
                DMOutlinedTextField(
                    text = finalPhone ?: "",
                    label = "Teléfono (opcional)",
                    modifier = Modifier.padding(vertical = 6.dp),
                    onChange = onFinalPhone,
                    maxLines = 1,
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Phone
                )

                DMDropDownField(
                    label = "Tipo de identificación",
                    items = idTypeDisplayNames,
                    selectedIndex = finalIdTypeIndex,
                    modifier = Modifier.padding(vertical = 6.dp),
                    onItemSelected = { idx, _ -> onIdType(idx) },
                    isError = false
                )

                DMOutlinedTextField(
                    text = finalIdNumber ?: "",
                    label = "Número de identificación",
                    modifier = Modifier.padding(vertical = 6.dp),
                    onChange = onIdNumber,
                    maxLines = 1,
                    imeAction = ImeAction.Next
                )

                if (finalIdType == "passport") {
                    DMOutlinedTextField(
                        text = finalPassportCountry ?: "",
                        label = "País del pasaporte",
                        modifier = Modifier.padding(vertical = 6.dp),
                        onChange = onPassportCountry,
                        maxLines = 1,
                        imeAction = ImeAction.Done
                    )
                }
            }
        }
    }
}
