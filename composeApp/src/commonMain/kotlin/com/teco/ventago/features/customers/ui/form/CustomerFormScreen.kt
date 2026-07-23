package com.teco.ventago.features.customers.ui.form

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.customers.ui.form.viewmodel.CustomerForeignIdType
import com.teco.ventago.features.customers.ui.form.viewmodel.CustomerFormMode
import com.teco.ventago.features.customers.ui.form.viewmodel.CustomerFormStep
import com.teco.ventago.features.customers.ui.form.viewmodel.CustomerFormUiEvent
import com.teco.ventago.features.customers.ui.form.viewmodel.CustomerFormViewModel
import com.teco.ventago.features.invoicing.domain.models.FeCustomerType
import com.teco.ventago.features.invoicing.domain.models.rucNeeded
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.country
import ventago.composeapp.generated.resources.customer
import ventago.composeapp.generated.resources.customers_address_line
import ventago.composeapp.generated.resources.customers_cedula
import ventago.composeapp.generated.resources.customers_corregimiento
import ventago.composeapp.generated.resources.customers_customer_type
import ventago.composeapp.generated.resources.customers_duplicate_dialog_title
import ventago.composeapp.generated.resources.customers_district
import ventago.composeapp.generated.resources.customers_dv
import ventago.composeapp.generated.resources.customers_foreign_document_number
import ventago.composeapp.generated.resources.customers_foreign_document_type
import ventago.composeapp.generated.resources.customers_province
import ventago.composeapp.generated.resources.customers_ruc
import ventago.composeapp.generated.resources.customers_ruc_prefix
import ventago.composeapp.generated.resources.customers_save_changes
import ventago.composeapp.generated.resources.customers_tax_exempt
import ventago.composeapp.generated.resources.customers_tax_retention
import ventago.composeapp.generated.resources.customers_tax_retention_percent
import ventago.composeapp.generated.resources.email
import ventago.composeapp.generated.resources.name
import ventago.composeapp.generated.resources.phone
import ventago.composeapp.generated.resources.pos_add_client
import ventago.composeapp.generated.resources.understood

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormScreen(
    customerId: Long? = null,
    viewModel: CustomerFormViewModel = koinViewModel(),
    onSaved: () -> Unit,
    onValidationError: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(customerId) {
        if (customerId == null) {
            viewModel.initCreate()
        } else {
            viewModel.loadForEdit(customerId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CustomerFormUiEvent.Saved -> onSaved()
                is CustomerFormUiEvent.ValidationError -> onValidationError(event.message)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.isFetching) {
            CustomerFormLoading()
            return@Column
        }

        if (uiState.mode == CustomerFormMode.CREATE) {
            GuidedCustomerCreateContent(viewModel)
            return@Column
        }

        if (uiState.mode == CustomerFormMode.CREATE) {
            DMDropDownField(
                label = stringResource(Res.string.customers_customer_type),
                items = uiState.customerTypeOptions.map { it.description },
                selectedIndex = uiState.customerTypeOptions.indexOf(uiState.customerType),
                onItemSelected = { index, _ ->
                    viewModel.onCustomerTypeChange(uiState.customerTypeOptions[index])
                },
                isError = false,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = uiState.name.ifBlank { stringResource(Res.string.customer) },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (uiState.ruc.isNotBlank()) {
                Text(
                    text = stringResource(Res.string.customers_ruc_prefix, uiState.ruc),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (uiState.mode == CustomerFormMode.CREATE && uiState.customerType.rucNeeded()) {
            DMOutlinedTextField(
                text = uiState.ruc,
                label = stringResource(Res.string.customers_ruc),
                onChange = viewModel::onRucChange,
                trailingIcon = Icons.Rounded.PersonSearch,
                trailingIconClick = viewModel::validateRuc,
                modifier = Modifier.fillMaxWidth(),
                imeAction = ImeAction.Next,
            )

            DMOutlinedTextField(
                text = uiState.rucCheckDigit,
                label = stringResource(Res.string.customers_dv),
                onChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (uiState.mode == CustomerFormMode.CREATE) {
            DMOutlinedTextField(
                text = uiState.name,
                label = stringResource(Res.string.name),
                onChange = viewModel::onNameChange,
                readOnly = uiState.customerType.rucNeeded(),
                modifier = Modifier.fillMaxWidth(),
                imeAction = ImeAction.Next,
                isError = uiState.nameError != null,
                supportingText = uiState.nameError ?: "",
            )
        }

        if (uiState.mode == CustomerFormMode.CREATE && uiState.customerType == FeCustomerType.FINAL_CONSUMER) {
            DMOutlinedTextField(
                text = uiState.cedulaCF,
                label = stringResource(Res.string.customers_cedula),
                onChange = viewModel::onCedulaChange,
                modifier = Modifier.fillMaxWidth(),
                imeAction = ImeAction.Next,
                isError = uiState.cedulaError != null,
                supportingText = uiState.cedulaError ?: "",
            )
        }

        if (uiState.mode == CustomerFormMode.CREATE && uiState.customerType == FeCustomerType.FOREIGNER) {
            val foreignIdTypeOptions = CustomerForeignIdType.entries
            DMDropDownField(
                label = stringResource(Res.string.customers_foreign_document_type),
                items = foreignIdTypeOptions.map { it.description },
                selectedIndex = foreignIdTypeOptions.indexOf(uiState.foreignIdType),
                onItemSelected = { index, _ ->
                    viewModel.onForeignIdTypeChange(foreignIdTypeOptions[index])
                },
                isError = false,
                modifier = Modifier.fillMaxWidth(),
            )

            DMOutlinedTextField(
                text = uiState.foreignIdNumber,
                label = stringResource(Res.string.customers_foreign_document_number),
                onChange = viewModel::onForeignIdNumberChange,
                modifier = Modifier.fillMaxWidth(),
                imeAction = ImeAction.Next,
            )

            DMDropDownField(
                label = stringResource(Res.string.country),
                items = uiState.countryOptions.map { "${it.name} (${it.code})" },
                selectedIndex = uiState.countryOptions.indexOfFirst { it.code == uiState.selectedCountryCode },
                onItemSelected = { index, _ ->
                    viewModel.onCountryCodeChange(uiState.countryOptions[index].code)
                },
                isError = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        DMOutlinedTextField(
            text = uiState.email,
            label = stringResource(Res.string.email),
            onChange = viewModel::onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Email,
        )

        DMOutlinedTextField(
            text = uiState.phone,
            label = stringResource(Res.string.phone),
            onChange = viewModel::onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Phone,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.taxExempt,
                onCheckedChange = viewModel::onTaxExemptChange,
            )
            Text(
                text = stringResource(Res.string.customers_tax_exempt),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        DMDropDownField(
            label = stringResource(Res.string.customers_tax_retention),
            items = viewModel.taxRetentionLabels(),
            selectedIndex = viewModel.selectedTaxRetentionIndex(),
            onItemSelected = { index, _ -> viewModel.onTaxRetentionSelected(index) },
            isError = false,
            modifier = Modifier.fillMaxWidth(),
        )

        if (viewModel.selectedTaxRetentionRequiresManualPercent()) {
            DMOutlinedTextField(
                text = uiState.taxRetentionPercent,
                label = stringResource(Res.string.customers_tax_retention_percent),
                onChange = viewModel::onTaxRetentionPercentChange,
                modifier = Modifier.fillMaxWidth(),
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Number,
            )
        }

        if (uiState.customerType != FeCustomerType.FOREIGNER) {
            DMDropDownField(
                label = stringResource(Res.string.customers_province),
                items = uiState.provinceOptions,
                selectedIndex = uiState.provinceOptions.indexOfFirst { it == uiState.selectedProvince },
                onItemSelected = { index, _ -> viewModel.onProvinceChange(uiState.provinceOptions[index]) },
                isError = uiState.provinceError != null,
                modifier = Modifier.fillMaxWidth(),
            )
            uiState.provinceError?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            DMDropDownField(
                label = stringResource(Res.string.customers_district),
                items = uiState.districtOptions,
                selectedIndex = uiState.districtOptions.indexOfFirst { it == uiState.selectedDistrict },
                onItemSelected = { index, _ -> viewModel.onDistrictChange(uiState.districtOptions[index]) },
                isError = uiState.districtError != null,
                enabled = uiState.selectedProvince != null,
                modifier = Modifier.fillMaxWidth(),
            )
            uiState.districtError?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            DMDropDownField(
                label = stringResource(Res.string.customers_corregimiento),
                items = uiState.corregimientoOptions,
                selectedIndex = uiState.corregimientoOptions.indexOfFirst { it == uiState.selectedCorregimiento },
                onItemSelected = { index, _ -> viewModel.onCorregimientoChange(uiState.corregimientoOptions[index]) },
                isError = uiState.corregimientoError != null,
                enabled = uiState.selectedDistrict != null,
                modifier = Modifier.fillMaxWidth(),
            )
            uiState.corregimientoError?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            DMOutlinedTextField(
                text = uiState.addressLine,
                label = stringResource(Res.string.customers_address_line),
                onChange = viewModel::onAddressLineChange,
                modifier = Modifier.fillMaxWidth(),
                imeAction = ImeAction.Done,
                isError = uiState.addressLineError != null,
                supportingText = uiState.addressLineError ?: "",
            )
        }

        Spacer(Modifier.height(8.dp))

        ButtonM(
            onClick = viewModel::saveCustomer,
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Text(
                text = if (uiState.isEditMode) {
                    stringResource(Res.string.customers_save_changes)
                } else {
                    stringResource(Res.string.pos_add_client)
                },
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

    }

    if (uiState.loadingBottomSheet.isLoading()) {
        LoadingSheet(
            state = uiState.loadingBottomSheet,
            sheetState = loadingSheetState,
            onDismissRequest = viewModel::hideLoading
        )
    }

    uiState.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::clearErrorMessage,
            title = { Text(text = stringResource(Res.string.customers_duplicate_dialog_title)) },
            text = { Text(text = error) },
            confirmButton = {
                TextButton(onClick = viewModel::clearErrorMessage) {
                    Text(text = stringResource(Res.string.understood))
                }
            }
        )
    }
}

@Composable
private fun GuidedCustomerCreateContent(viewModel: CustomerFormViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    GuidedCustomerProgressHeader(
        currentStep = uiState.currentStep,
        selectedType = uiState.customerType,
        showSelectedType = uiState.customerTypeSelected,
    )

    when (uiState.currentStep) {
        CustomerFormStep.TYPE -> CustomerFormTypeStep(viewModel)
        CustomerFormStep.MAIN_INFO -> CustomerFormMainInfoStep(viewModel)
        CustomerFormStep.OPTIONAL_INFO -> CustomerFormOptionalInfoStep(viewModel)
    }
}

@Composable
private fun GuidedCustomerProgressHeader(
    currentStep: CustomerFormStep,
    selectedType: FeCustomerType,
    showSelectedType: Boolean,
) {
    val stepNumber = when (currentStep) {
        CustomerFormStep.TYPE -> 1
        CustomerFormStep.MAIN_INFO -> 2
        CustomerFormStep.OPTIONAL_INFO -> 3
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Paso $stepNumber de 3",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = when (currentStep) {
                CustomerFormStep.TYPE -> stringResource(Res.string.customers_customer_type)
                CustomerFormStep.MAIN_INFO -> "Informacion principal"
                CustomerFormStep.OPTIONAL_INFO -> "Informacion opcional"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (showSelectedType && currentStep != CustomerFormStep.TYPE) {
            Text(
                text = selectedType.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CustomerFormTypeStep(viewModel: CustomerFormViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        uiState.customerTypeOptions.forEach { type ->
            CustomerFormTypeCard(
                type = type,
                selected = uiState.customerTypeSelected && uiState.customerType == type,
                onClick = { viewModel.onCustomerTypeSelected(type) },
            )
        }
    }
}

@Composable
private fun CustomerFormTypeCard(
    type: FeCustomerType,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.secondary
    val container = if (selected) {
        vanishedBackgroundColor()
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = customerFormTypeIcon(type),
                contentDescription = null,
                tint = accent,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = customerFormTypeDescription(type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = accent,
                )
            }
        }
    }
}

@Composable
private fun CustomerFormMainInfoStep(viewModel: CustomerFormViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            uiState.customerType.rucNeeded() -> CustomerFormRucMainInfo(viewModel)
            uiState.customerType == FeCustomerType.FOREIGNER -> CustomerFormForeignMainInfo(viewModel)
            else -> CustomerFormFinalConsumerMainInfo(viewModel)
        }

        CustomerFormValidationMessage(uiState.validationMessage)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButtonM(
                modifier = Modifier.weight(1f),
                onClick = viewModel::onStepBack,
            ) {
                Text(text = "Atras")
            }
            ButtonM(
                modifier = Modifier.weight(1f),
                onClick = viewModel::goToOptionalInfo,
            ) {
                Text(text = "Siguiente")
            }
        }
    }
}

@Composable
private fun CustomerFormFinalConsumerMainInfo(viewModel: CustomerFormViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    DMOutlinedTextField(
        text = uiState.name,
        label = stringResource(Res.string.name),
        onChange = viewModel::onNameChange,
        modifier = Modifier.fillMaxWidth(),
        imeAction = ImeAction.Next,
        isError = uiState.nameError != null,
        supportingText = uiState.nameError ?: "",
    )

    DMOutlinedTextField(
        text = uiState.cedulaCF,
        label = stringResource(Res.string.customers_cedula),
        onChange = viewModel::onCedulaChange,
        modifier = Modifier.fillMaxWidth(),
        imeAction = ImeAction.Done,
        isError = uiState.cedulaError != null,
        supportingText = uiState.cedulaError ?: "",
    )
}

@Composable
private fun CustomerFormRucMainInfo(viewModel: CustomerFormViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    DMOutlinedTextField(
        text = uiState.ruc,
        label = stringResource(Res.string.customers_ruc),
        onChange = viewModel::onRucChange,
        trailingIcon = Icons.Rounded.PersonSearch,
        trailingIconClick = viewModel::validateRuc,
        modifier = Modifier.fillMaxWidth(),
        imeAction = ImeAction.Search,
        isError = uiState.rucError != null,
        supportingText = uiState.rucError ?: "",
    )

    DMOutlinedTextField(
        text = uiState.rucCheckDigit,
        label = stringResource(Res.string.customers_dv),
        onChange = {},
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
    )

    DMOutlinedTextField(
        text = uiState.name,
        label = "Nombre legal",
        onChange = {},
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        isError = uiState.nameError != null,
        supportingText = uiState.nameError ?: "",
    )
}

@Composable
private fun CustomerFormForeignMainInfo(viewModel: CustomerFormViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val foreignIdTypeOptions = CustomerForeignIdType.entries

    DMOutlinedTextField(
        text = uiState.name,
        label = stringResource(Res.string.name),
        onChange = viewModel::onNameChange,
        modifier = Modifier.fillMaxWidth(),
        imeAction = ImeAction.Next,
        isError = uiState.nameError != null,
        supportingText = uiState.nameError ?: "",
    )

    DMDropDownField(
        label = stringResource(Res.string.customers_foreign_document_type),
        items = foreignIdTypeOptions.map { it.description },
        selectedIndex = foreignIdTypeOptions.indexOf(uiState.foreignIdType),
        onItemSelected = { index, _ ->
            viewModel.onForeignIdTypeChange(foreignIdTypeOptions[index])
        },
        isError = false,
        modifier = Modifier.fillMaxWidth(),
    )

    DMOutlinedTextField(
        text = uiState.foreignIdNumber,
        label = stringResource(Res.string.customers_foreign_document_number),
        onChange = viewModel::onForeignIdNumberChange,
        modifier = Modifier.fillMaxWidth(),
        imeAction = ImeAction.Next,
        isError = uiState.foreignIdNumberError != null,
        supportingText = uiState.foreignIdNumberError ?: "",
    )

    DMDropDownField(
        label = stringResource(Res.string.country),
        items = uiState.countryOptions.map { "${it.name} (${it.code})" },
        selectedIndex = uiState.countryOptions.indexOfFirst { it.code == uiState.selectedCountryCode },
        onItemSelected = { index, _ ->
            viewModel.onCountryCodeChange(uiState.countryOptions[index].code)
        },
        isError = false,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CustomerFormOptionalInfoStep(viewModel: CustomerFormViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DMOutlinedTextField(
            text = uiState.email,
            label = stringResource(Res.string.email),
            onChange = viewModel::onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Email,
        )

        DMOutlinedTextField(
            text = uiState.phone,
            label = stringResource(Res.string.phone),
            onChange = viewModel::onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Phone,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.taxExempt,
                onCheckedChange = viewModel::onTaxExemptChange,
            )
            Text(
                text = stringResource(Res.string.customers_tax_exempt),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        DMDropDownField(
            label = stringResource(Res.string.customers_tax_retention),
            items = viewModel.taxRetentionLabels(),
            selectedIndex = viewModel.selectedTaxRetentionIndex(),
            onItemSelected = { index, _ -> viewModel.onTaxRetentionSelected(index) },
            isError = false,
            modifier = Modifier.fillMaxWidth(),
        )

        if (viewModel.selectedTaxRetentionRequiresManualPercent()) {
            DMOutlinedTextField(
                text = uiState.taxRetentionPercent,
                label = stringResource(Res.string.customers_tax_retention_percent),
                onChange = viewModel::onTaxRetentionPercentChange,
                modifier = Modifier.fillMaxWidth(),
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Number,
                isError = uiState.taxRetentionPercentError != null,
                supportingText = uiState.taxRetentionPercentError ?: "",
            )
        }

        if (uiState.customerType != FeCustomerType.FOREIGNER) {
            CustomerFormAddressCard(viewModel)
        }

        CustomerFormValidationMessage(uiState.validationMessage)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButtonM(
                modifier = Modifier.weight(1f),
                onClick = viewModel::onStepBack,
            ) {
                Text(text = "Atras")
            }
            ButtonM(
                modifier = Modifier.weight(1f),
                onClick = viewModel::saveCustomer,
            ) {
                Text(text = stringResource(Res.string.pos_add_client))
            }
        }
    }
}

@Composable
private fun CustomerFormAddressCard(viewModel: CustomerFormViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = viewModel::toggleAddressExpanded),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.customers_address_line),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${uiState.selectedProvince ?: "-"} / ${uiState.selectedDistrict ?: "-"} / ${uiState.selectedCorregimiento ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = uiState.addressLine.ifBlank { "-" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = viewModel::toggleAddressExpanded) {
                    Icon(
                        imageVector = if (uiState.addressExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                    )
                }
            }

            if (uiState.addressExpanded) {
                DMDropDownField(
                    label = stringResource(Res.string.customers_province),
                    items = uiState.provinceOptions,
                    selectedIndex = uiState.provinceOptions.indexOfFirst { it == uiState.selectedProvince },
                    onItemSelected = { index, _ -> viewModel.onProvinceChange(uiState.provinceOptions[index]) },
                    isError = uiState.provinceError != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                CustomerFormFieldError(uiState.provinceError)

                DMDropDownField(
                    label = stringResource(Res.string.customers_district),
                    items = uiState.districtOptions,
                    selectedIndex = uiState.districtOptions.indexOfFirst { it == uiState.selectedDistrict },
                    onItemSelected = { index, _ -> viewModel.onDistrictChange(uiState.districtOptions[index]) },
                    isError = uiState.districtError != null,
                    enabled = uiState.selectedProvince != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                CustomerFormFieldError(uiState.districtError)

                DMDropDownField(
                    label = stringResource(Res.string.customers_corregimiento),
                    items = uiState.corregimientoOptions,
                    selectedIndex = uiState.corregimientoOptions.indexOfFirst { it == uiState.selectedCorregimiento },
                    onItemSelected = { index, _ -> viewModel.onCorregimientoChange(uiState.corregimientoOptions[index]) },
                    isError = uiState.corregimientoError != null,
                    enabled = uiState.selectedDistrict != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                CustomerFormFieldError(uiState.corregimientoError)

                DMOutlinedTextField(
                    text = uiState.addressLine,
                    label = stringResource(Res.string.customers_address_line),
                    onChange = viewModel::onAddressLineChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    imeAction = ImeAction.Done,
                    isError = uiState.addressLineError != null,
                    supportingText = uiState.addressLineError ?: "",
                )
            }
        }
    }
}

@Composable
private fun CustomerFormValidationMessage(message: String?) {
    message?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CustomerFormFieldError(message: String?) {
    message?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun customerFormTypeIcon(type: FeCustomerType): ImageVector {
    return when (type) {
        FeCustomerType.FINAL_CONSUMER -> Icons.Rounded.Person
        FeCustomerType.CONTRIBUTING -> Icons.Rounded.Business
        FeCustomerType.GOVERNMENT -> Icons.Rounded.AccountBalance
        FeCustomerType.FOREIGNER -> Icons.Rounded.Public
    }
}

private fun customerFormTypeDescription(type: FeCustomerType): String {
    return when (type) {
        FeCustomerType.FINAL_CONSUMER -> "Cliente personal con nombre y cedula opcional"
        FeCustomerType.CONTRIBUTING -> "Empresa o persona registrada con RUC"
        FeCustomerType.GOVERNMENT -> "Entidad gubernamental con RUC"
        FeCustomerType.FOREIGNER -> "Cliente fuera de Panama con documento extranjero"
    }
}

@Composable
private fun CustomerFormLoading() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(8) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (index == 0) 52.dp else 56.dp)
                    .background(shimmerBrush(), RoundedCornerShape(10.dp))
            )
        }
    }
}
