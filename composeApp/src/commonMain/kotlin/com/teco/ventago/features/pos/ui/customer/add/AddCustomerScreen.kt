package com.teco.ventago.features.pos.ui.customer.add

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMChipTextField
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.invoicing.domain.models.FeCustomerType
import com.teco.ventago.features.invoicing.domain.models.rucNeeded
import com.teco.ventago.features.pos.ui.dismissKeyboardOnOutsideTap
import com.teco.ventago.features.pos.ui.customer.add.viewmodel.AddCustomerStep
import com.teco.ventago.features.pos.ui.customer.add.viewmodel.AddCustomerStateUiEvent
import com.teco.ventago.features.pos.ui.customer.add.viewmodel.AddCustomerViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.customers_duplicate_dialog_title
import ventago.composeapp.generated.resources.customers_address_line
import ventago.composeapp.generated.resources.customers_corregimiento
import ventago.composeapp.generated.resources.customers_district
import ventago.composeapp.generated.resources.customers_province
import ventago.composeapp.generated.resources.name
import ventago.composeapp.generated.resources.phone
import ventago.composeapp.generated.resources.pos_clients_email
import ventago.composeapp.generated.resources.pos_clients_ruc
import ventago.composeapp.generated.resources.pos_clients_tag
import ventago.composeapp.generated.resources.understood

@Composable
fun AddCustomerScreen(viewModel: AddCustomerViewModel = koinViewModel(), navigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect {
            when (it) {
                is AddCustomerStateUiEvent.CustomerCreated -> {
                    navigateBack()
                }

                is AddCustomerStateUiEvent.InvalidRucNumber -> {
                    // Error is rendered from state.errorMessage
                }
                is AddCustomerStateUiEvent.ValidationError -> Unit
            }
        }
    }

    if (uiState.invoicingEnabled) {
        FullAddCustomerScreen(viewModel, navigateBack)
    } else {
        ReducedAddCustomerScreen(viewModel, navigateBack)
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReducedAddCustomerScreen(viewModel: AddCustomerViewModel, navigateBack: () -> Unit) {
    val focusManager = LocalFocusManager.current

    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .dismissKeyboardOnOutsideTap(focusManager),
    ) {
        DMOutlinedTextField(
            text = uiState.name,
            label = stringResource(Res.string.name),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp, top = 16.dp),
            onChange = {
                viewModel.onNameChange(it)
            },
            maxLines = 1,
            imeAction = ImeAction.Next,
            isError = uiState.nameError != null,
            supportingText = uiState.nameError ?: "",
        )

        DMOutlinedTextField(
            text = uiState.phone,
            label = stringResource(Res.string.phone),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
            onChange = {
                viewModel.onPhoneChange(it)
            },
            maxLines = 1,
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Number,
        )

        DMOutlinedTextField(
            text = uiState.email,
            label = stringResource(Res.string.pos_clients_email),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
            onChange = {
                viewModel.onEmailChange(it)
            },
            maxLines = 1,
            imeAction = ImeAction.Next,
        )

        DMDropDownField(
            label = stringResource(Res.string.customers_province),
            items = uiState.provinceOptions,
            selectedIndex = uiState.provinceOptions.indexOfFirst { province -> province == uiState.selectedProvince },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            onItemSelected = { index, _ -> viewModel.onProvinceChange(uiState.provinceOptions[index]) },
            isError = uiState.provinceError != null,
        )
        uiState.provinceError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }

        DMDropDownField(
            label = stringResource(Res.string.customers_district),
            items = uiState.districtOptions,
            selectedIndex = uiState.districtOptions.indexOfFirst { district -> district == uiState.selectedDistrict },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            onItemSelected = { index, _ -> viewModel.onDistrictChange(uiState.districtOptions[index]) },
            isError = uiState.districtError != null,
            enabled = uiState.selectedProvince != null,
        )
        uiState.districtError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }

        DMDropDownField(
            label = stringResource(Res.string.customers_corregimiento),
            items = uiState.corregOptions,
            selectedIndex = uiState.corregOptions.indexOfFirst { correg -> correg == uiState.selectedCorreg },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            onItemSelected = { index, _ -> viewModel.onCorregimientoChange(uiState.corregOptions[index]) },
            isError = uiState.corregimientoError != null,
            enabled = uiState.selectedDistrict != null,
        )
        uiState.corregimientoError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }

        DMOutlinedTextField(
            text = uiState.addressLine ?: "",
            label = stringResource(Res.string.customers_address_line),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
            onChange = {
                viewModel.onAddressLineChange(it)
            },
            maxLines = 1,
            imeAction = ImeAction.Next,
            isError = uiState.addressLineError != null,
            supportingText = uiState.addressLineError ?: "",
        )

        DMOutlinedTextField(
            text = uiState.ruc,
            label = stringResource(Res.string.pos_clients_ruc),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
            onChange = {
                viewModel.onTaxIdChange(it)
            },
            maxLines = 1,
            imeAction = ImeAction.Next,
        )

        DMChipTextField(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                .fillMaxWidth(),
            label = stringResource(Res.string.pos_clients_tag),
            chips = uiState.tags,
            onAddChip = { tag ->
                val tags = uiState.tags.toMutableList()
                if (!tags.contains(tag)) {
                    tags.add(tag)
                }
                viewModel.onTagsChange(tags)
            },
            onRemoveChip = {
                val tags = uiState.tags.toMutableList()
                if (tags.contains(it)) {
                    tags.remove(it)
                }
                viewModel.onTagsChange(tags)
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        uiState.validationMessage?.let { validationMessage ->
            Text(
                text = validationMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp)
            )
        }

        ButtonM(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 16.dp),
            onClick = {
                viewModel.createCustomer()
            },
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Text(text = "Crear cliente")
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullAddCustomerScreen(viewModel: AddCustomerViewModel, navigateBack: () -> Unit) {
    val focusManager = LocalFocusManager.current

    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .dismissKeyboardOnOutsideTap(focusManager),
    ) {
        GuidedProgressHeader(
            currentStep = uiState.currentStep,
            selectedType = uiState.customerType,
        )

        when (uiState.currentStep) {
            AddCustomerStep.TYPE -> CustomerTypeStep(viewModel = viewModel)
            AddCustomerStep.MAIN_INFO -> MainInfoStep(viewModel = viewModel)
            AddCustomerStep.OPTIONAL_INFO -> OptionalInfoStep(viewModel = viewModel)
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
}

@Composable
private fun GuidedProgressHeader(
    currentStep: AddCustomerStep,
    selectedType: FeCustomerType,
) {
    val stepNumber = when (currentStep) {
        AddCustomerStep.TYPE -> 1
        AddCustomerStep.MAIN_INFO -> 2
        AddCustomerStep.OPTIONAL_INFO -> 3
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Paso $stepNumber de 3",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = when (currentStep) {
                AddCustomerStep.TYPE -> "Tipo de cliente"
                AddCustomerStep.MAIN_INFO -> "Informacion principal"
                AddCustomerStep.OPTIONAL_INFO -> "Informacion opcional"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (currentStep != AddCustomerStep.TYPE) {
            Text(
                text = selectedType.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CustomerTypeStep(viewModel: AddCustomerViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        uiState.customerTypeOptions.forEach { type ->
            CustomerTypeCard(
                type = type,
                selected = uiState.customerTypeSelected && uiState.customerType == type,
                onClick = { viewModel.onCustomerTypeSelected(type) },
            )
        }
    }
}

@Composable
private fun CustomerTypeCard(
    type: FeCustomerType,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.secondary
    val container = if (selected) {
        vanishedBackgroundColor()
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (selected) primary else MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, borderColor),
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
                imageVector = customerTypeIcon(type),
                contentDescription = null,
                tint = primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = customerTypeDescription(type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = primary,
                )
            }
        }
    }
}

@Composable
private fun MainInfoStep(viewModel: AddCustomerViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            uiState.customerType.rucNeeded() -> RucMainInfo(viewModel)
            uiState.customerType == FeCustomerType.FOREIGNER -> ForeignMainInfo(viewModel)
            else -> FinalConsumerMainInfo(viewModel)
        }

        StepValidationMessage(uiState.validationMessage)

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
private fun FinalConsumerMainInfo(viewModel: AddCustomerViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    DMOutlinedTextField(
        text = uiState.name,
        label = stringResource(Res.string.name),
        modifier = Modifier.fillMaxWidth(),
        onChange = viewModel::onNameChange,
        maxLines = 1,
        imeAction = ImeAction.Next,
        isError = uiState.nameError != null,
        supportingText = uiState.nameError ?: "",
    )

    DMOutlinedTextField(
        text = uiState.cfCedula ?: "",
        label = "Cedula",
        modifier = Modifier.fillMaxWidth(),
        onChange = viewModel::onCedulaChanges,
        maxLines = 1,
        imeAction = ImeAction.Done,
        isError = uiState.cfCedulaError != null,
        supportingText = uiState.cfCedulaError ?: "",
    )
}

@Composable
private fun RucMainInfo(viewModel: AddCustomerViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    DMOutlinedTextField(
        text = uiState.ruc,
        label = "RUC",
        modifier = Modifier.fillMaxWidth(),
        onChange = viewModel::onTaxIdChange,
        maxLines = 1,
        imeAction = ImeAction.Search,
        trailingIcon = Icons.Rounded.PersonSearch,
        trailingIconClick = viewModel::validateRUC,
        isError = uiState.rucError != null,
        supportingText = uiState.rucError ?: "",
    )

    DMOutlinedTextField(
        text = uiState.rucCheckDigit ?: "",
        readOnly = true,
        label = "DV",
        modifier = Modifier.fillMaxWidth(),
        onChange = {},
        maxLines = 1,
    )

    DMOutlinedTextField(
        text = uiState.name,
        readOnly = true,
        label = "Nombre legal",
        modifier = Modifier.fillMaxWidth(),
        onChange = {},
        maxLines = 1,
        isError = uiState.nameError != null,
        supportingText = uiState.nameError ?: "",
    )
}

@Composable
private fun ForeignMainInfo(viewModel: AddCustomerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val countryNames = uiState.countryOptions.map { "${it.name} (${it.code})" }

    DMOutlinedTextField(
        text = uiState.name,
        label = stringResource(Res.string.name),
        modifier = Modifier.fillMaxWidth(),
        onChange = viewModel::onNameChange,
        maxLines = 1,
        imeAction = ImeAction.Next,
        isError = uiState.nameError != null,
        supportingText = uiState.nameError ?: "",
    )

    DMDropDownField(
        label = "Tipo de documento",
        items = uiState.foreignIdTypeOptions.map { it.description },
        selectedIndex = uiState.foreignIdTypeOptions.indexOf(uiState.selectedForeignIdType),
        modifier = Modifier.fillMaxWidth(),
        onItemSelected = { index, _ ->
            viewModel.onForeignIdTypeSelected(uiState.foreignIdTypeOptions[index])
        },
        isError = false
    )

    DMOutlinedTextField(
        text = uiState.foreignIdNumber ?: "",
        label = "Numero de documento",
        modifier = Modifier.fillMaxWidth(),
        onChange = viewModel::onForeignIdNumberChange,
        maxLines = 1,
        imeAction = ImeAction.Next,
        isError = uiState.foreignIdNumberError != null,
        supportingText = uiState.foreignIdNumberError ?: "",
    )

    DMDropDownField(
        label = "Pais",
        items = countryNames,
        selectedIndex = uiState.countryOptions.indexOfFirst { it.code == uiState.selectedCountryCode },
        modifier = Modifier.fillMaxWidth(),
        onItemSelected = { index, _ ->
            viewModel.onCountrySelected(uiState.countryOptions[index].code)
        },
        isError = false,
    )
}

@Composable
private fun OptionalInfoStep(viewModel: AddCustomerViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DMOutlinedTextField(
            text = uiState.email,
            label = "Correo Electronico",
            modifier = Modifier.fillMaxWidth(),
            onChange = viewModel::onEmailChange,
            maxLines = 1,
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Email,
        )

        DMOutlinedTextField(
            text = uiState.phone,
            label = stringResource(Res.string.phone),
            modifier = Modifier.fillMaxWidth(),
            onChange = viewModel::onPhoneChange,
            maxLines = 1,
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Phone,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = uiState.taxExempt,
                onCheckedChange = viewModel::onTaxExemptChange,
            )
            Text(
                text = "ITBMS exento",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        DMDropDownField(
            label = "Retencion",
            items = viewModel.taxRetentionLabels(),
            selectedIndex = viewModel.selectedTaxRetentionIndex(),
            modifier = Modifier.fillMaxWidth(),
            onItemSelected = { index, _ -> viewModel.onTaxRetentionSelected(index) },
            isError = false,
        )

        if (viewModel.selectedTaxRetentionRequiresManualPercent()) {
            DMOutlinedTextField(
                text = uiState.taxRetentionPercent,
                label = "Porcentaje de retencion",
                modifier = Modifier.fillMaxWidth(),
                onChange = viewModel::onTaxRetentionPercentChange,
                maxLines = 1,
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Number,
                isError = uiState.taxRetentionPercentError != null,
                supportingText = uiState.taxRetentionPercentError ?: "",
            )
        }

        if (uiState.customerType != FeCustomerType.FOREIGNER) {
            AddressCard(viewModel)
        }

        StepValidationMessage(uiState.validationMessage)

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
                onClick = viewModel::createCustomer,
            ) {
                Text(text = "Crear")
            }
        }
    }
}

@Composable
private fun AddressCard(viewModel: AddCustomerViewModel) {
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
                        text = "Direccion",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${uiState.selectedProvince ?: "-"} / ${uiState.selectedDistrict ?: "-"} / ${uiState.selectedCorreg ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = uiState.addressLine ?: "-",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    onItemSelected = { index, _ -> viewModel.onProvinceChange(uiState.provinceOptions[index]) },
                    isError = uiState.provinceError != null,
                )
                FieldError(uiState.provinceError)

                DMDropDownField(
                    label = stringResource(Res.string.customers_district),
                    items = uiState.districtOptions,
                    selectedIndex = uiState.districtOptions.indexOfFirst { it == uiState.selectedDistrict },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    onItemSelected = { index, _ -> viewModel.onDistrictChange(uiState.districtOptions[index]) },
                    isError = uiState.districtError != null,
                    enabled = uiState.selectedProvince != null,
                )
                FieldError(uiState.districtError)

                DMDropDownField(
                    label = stringResource(Res.string.customers_corregimiento),
                    items = uiState.corregOptions,
                    selectedIndex = uiState.corregOptions.indexOfFirst { it == uiState.selectedCorreg },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    onItemSelected = { index, _ -> viewModel.onCorregimientoChange(uiState.corregOptions[index]) },
                    isError = uiState.corregimientoError != null,
                    enabled = uiState.selectedDistrict != null,
                )
                FieldError(uiState.corregimientoError)

                DMOutlinedTextField(
                    text = uiState.addressLine ?: "",
                    label = stringResource(Res.string.customers_address_line),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    onChange = viewModel::onAddressLineChange,
                    maxLines = 1,
                    imeAction = ImeAction.Next,
                    isError = uiState.addressLineError != null,
                    supportingText = uiState.addressLineError ?: "",
                )
            }
        }
    }
}

@Composable
private fun StepValidationMessage(message: String?) {
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
private fun FieldError(message: String?) {
    message?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun customerTypeIcon(type: FeCustomerType): ImageVector {
    return when (type) {
        FeCustomerType.FINAL_CONSUMER -> Icons.Rounded.Person
        FeCustomerType.CONTRIBUTING -> Icons.Rounded.Business
        FeCustomerType.GOVERNMENT -> Icons.Rounded.AccountBalance
        FeCustomerType.FOREIGNER -> Icons.Rounded.Public
    }
}

private fun customerTypeDescription(type: FeCustomerType): String {
    return when (type) {
        FeCustomerType.FINAL_CONSUMER -> "Cliente personal con nombre y cedula opcional"
        FeCustomerType.CONTRIBUTING -> "Empresa o persona registrada con RUC"
        FeCustomerType.GOVERNMENT -> "Entidad gubernamental con RUC"
        FeCustomerType.FOREIGNER -> "Cliente fuera de Panama con documento extranjero"
    }
}
