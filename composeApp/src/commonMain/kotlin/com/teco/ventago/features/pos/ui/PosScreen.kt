package com.teco.ventago.features.pos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teco.ventago.core.LocalStorage
import com.teco.ventago.core.flags.IFlagsService
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.molecules.flags.DgiDownAlertBanner
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.pos_customer_required_to_continue
import ventago.composeapp.generated.resources.pos_customer_type_final_desc
import ventago.composeapp.generated.resources.pos_customer_type_final_title
import ventago.composeapp.generated.resources.pos_customer_type_hint
import ventago.composeapp.generated.resources.pos_customer_type_question
import ventago.composeapp.generated.resources.pos_customer_type_registered_desc
import ventago.composeapp.generated.resources.pos_customer_type_registered_title
import ventago.composeapp.generated.resources.pos_customer_type_required_to_continue

@Composable
fun PosScreen(
    viewModel: PosViewModel,
    navigate: (PosScreens) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val flagsService: IFlagsService = koinInject()
    val flagsState by flagsService.flags().collectAsState()
    val isQuoteFlow = uiState.flowMode == FlowMode.QUOTE

    LaunchedEffect(Unit) {
        if (QuoteSelectionStore.startOrderFlowFromQuote) {
            val quoteId = QuoteSelectionStore.selected?.id
            QuoteSelectionStore.startOrderFlowFromQuote = false
            QuoteSelectionStore.startQuoteFlow = false

            if (uiState.canCreateInvoice || uiState.canCreateDraft) {
                viewModel.resetForNewSale()
                viewModel.setFlowMode(FlowMode.SALE, quoteId = null)
                if (quoteId != null) {
                    viewModel.startSaleFromQuote(quoteId)
                }
            }
            return@LaunchedEffect
        }
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

        if (flagsState.dgiDown) {
            DgiDownAlertBanner(
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

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

        val customerTypeMissing = uiState.finalCustomer == null
        val customerMissing = uiState.finalCustomer == false && uiState.customer == null
        val canContinue = !customerTypeMissing && !customerMissing

        if (customerMissing) {
            Text(
                text = stringResource(Res.string.pos_customer_required_to_continue),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                style = bodySmall(),
                color = MaterialTheme.colorScheme.error
            )
        }

        ButtonM(
            onClick = {
                navigate(PosScreens.POSProductScreen)
            },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            enabled = canContinue
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
    val storage: LocalStorage = koinInject()

    val initialHintShownCount = remember { storage.int(KEY_CUSTOMER_TYPE_HINT_SHOWN_COUNT) ?: 0 }
    var hasSelectedRegisteredOnce by rememberSaveable {
        mutableStateOf(storage.bool(KEY_HAS_SELECTED_REGISTERED_ONCE) == true)
    }
    val shouldShowHint = (uiState.finalCustomer == null) &&
        (initialHintShownCount < 2) &&
        !hasSelectedRegisteredOnce

    LaunchedEffect(shouldShowHint) {
        if (!shouldShowHint) return@LaunchedEffect
        storage.set(KEY_CUSTOMER_TYPE_HINT_SHOWN_COUNT, initialHintShownCount + 1)
    }

    fun selectCustomerType(isFinalCustomer: Boolean) {
        if (!uiState.enabledSelectionDocType) return
        if (!isFinalCustomer && !hasSelectedRegisteredOnce) {
            hasSelectedRegisteredOnce = true
            storage.set(KEY_HAS_SELECTED_REGISTERED_ONCE, true)
        }
        onFinalToggle(isFinalCustomer)
        extraOpen = false
    }

    Column(Modifier.padding(16.dp)) {
        Text(stringResource(Res.string.pos_customer_type_question), style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(8.dp))

        if (shouldShowHint) {
            CustomerTypeHintBanner(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.pos_customer_type_hint)
            )
            Spacer(Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomerTypeOptionCard(
                modifier = Modifier.weight(1f),
                selected = uiState.finalCustomer == true,
                enabled = uiState.enabledSelectionDocType,
                icon = Icons.Outlined.Person,
                title = stringResource(Res.string.pos_customer_type_final_title),
                description = stringResource(Res.string.pos_customer_type_final_desc),
                onClick = { selectCustomerType(true) }
            )
            CustomerTypeOptionCard(
                modifier = Modifier.weight(1f),
                selected = uiState.finalCustomer == false,
                enabled = uiState.enabledSelectionDocType,
                icon = Icons.Outlined.Business,
                title = stringResource(Res.string.pos_customer_type_registered_title),
                description = stringResource(Res.string.pos_customer_type_registered_desc),
                onClick = { selectCustomerType(false) }
            )
        }

        Spacer(Modifier.height(12.dp))

        when (uiState.finalCustomer) {
            null -> Unit
            false -> {
                PosCustomerSelection(
                    customer = uiState.customer
                ) { screen ->
                    if (uiState.enabledSelectionDocType) {
                        navigate(screen)
                    }
                }
            }
            true -> {
                Column {
                    Text(
                        text = uiState.finalName?.takeIf { it.isNotBlank() }
                            ?: stringResource(Res.string.pos_customer_type_final_title),
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
}

@Composable
private fun CustomerTypeHintBanner(
    modifier: Modifier = Modifier,
    text: String,
) {
    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = vanishedBackgroundColor()
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = bodySmall(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CustomerTypeOptionCard(
    modifier: Modifier = Modifier,
    selected: Boolean,
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    val borderColor = when {
        !enabled -> MaterialTheme.colorScheme.outlineVariant
        selected -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }
    val containerColor = when {
        !enabled -> MaterialTheme.colorScheme.background
        selected -> vanishedBackgroundColor()
        else -> MaterialTheme.colorScheme.background
    }

    OutlinedCard(
        modifier = modifier
            .height(118.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            ),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(title, style = bodyMedium(), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                description,
                style = bodySmall(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
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

private const val KEY_CUSTOMER_TYPE_HINT_SHOWN_COUNT = "pos.customer_type.hint_shown_count"
private const val KEY_HAS_SELECTED_REGISTERED_ONCE = "pos.customer_type.has_selected_registered_once"
