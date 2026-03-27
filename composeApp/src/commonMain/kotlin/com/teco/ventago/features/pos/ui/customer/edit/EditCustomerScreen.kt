package com.teco.ventago.features.pos.ui.customer.edit

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMChipTextField
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.features.invoicing.domain.models.FeCustomerType
import com.teco.ventago.features.invoicing.domain.models.rucNeeded
import com.teco.ventago.features.pos.ui.customer.edit.viewmodel.EditCustomerViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.name
import ventago.composeapp.generated.resources.phone
import ventago.composeapp.generated.resources.pos_clients_email
import ventago.composeapp.generated.resources.pos_clients_ruc
import ventago.composeapp.generated.resources.pos_clients_tag

@Composable
fun EditCustomerScreen(viewModel: EditCustomerViewModel = koinViewModel(), navigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.invoicingEnabled) {
        FullAddCustomerScreen(viewModel, navigateBack)
    } else {
        ReducedAddCustomerScreen(viewModel, navigateBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReducedAddCustomerScreen(viewModel: EditCustomerViewModel, navigateBack: () -> Unit) {
    val focusManager = LocalFocusManager.current

    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })

    Column(
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus() // Close keyboard when tapping outside
                })
            },
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

        ButtonM(
            modifier = Modifier
                .padding(16.dp),
            onClick = {
//                viewModel.createCustomer()
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
fun FullAddCustomerScreen(viewModel: EditCustomerViewModel, navigateBack: () -> Unit) {
    val focusManager = LocalFocusManager.current

    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })

    Column(
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus() // Close keyboard when tapping outside
                })
            },
    ) {
        if (uiState.taxInfoIncomplete) {
            OutlinedCard {
                Text(
                    text = "La informacion tributaria esta incompleta. Por favor seleccione el tipo de cliente correcto.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                        .fillMaxWidth(),
                )
            }

        }
        DMDropDownField(
            label = "Tipo de cliente",
            items = uiState.customerTypeOptions.map { type -> type.description },
            selectedIndex = uiState.customerTypeOptions.indexOfFirst { type -> type == uiState.customerType },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            onItemSelected = { index, value ->
                viewModel.onCustomerTypeChange(uiState.customerTypeOptions.first { type -> type.description == value })
            },
            isError = false,
        )

        if (uiState.customerType.rucNeeded()) {
            DMOutlinedTextField(
                text = uiState.ruc,
                label = "RUC",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
                onChange = {
                    viewModel.onTaxIdChange(it)
                },
                maxLines = 1,
                imeAction = ImeAction.Search,
                trailingIcon = Icons.Rounded.PersonSearch,
                trailingIconClick = {
                    viewModel.validateRUC()
                }
            )

            DMOutlinedTextField(
                text = uiState.rucCheckDigit ?: "",
                readOnly = true,
                label = "DV",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
                onChange = {
//                viewModel.onTaxIdChange(it)
                },
                maxLines = 1,
            )
        }

        DMOutlinedTextField(
            text = uiState.name,
            readOnly = uiState.customerType.rucNeeded(),
            label = stringResource(Res.string.name),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
            onChange = {
                viewModel.onNameChange(it)
            },
            maxLines = 1,
            imeAction = ImeAction.Next,
        )

        if (uiState.customerType == FeCustomerType.FOREIGNER) {
            DMOutlinedTextField(
                text = uiState.foreignIdNumber ?: "",
                label = "Pasaporte u otro ID",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp, top = 16.dp),
                onChange = {

                }
            )
        } else if (uiState.customerType == FeCustomerType.FINAL_CONSUMER) {
            DMOutlinedTextField(
                text = uiState.cfCedula ?: "",
                label = "Cedula",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp, top = 16.dp),
                onChange = {
                    viewModel.onCedulaChanges(it)
                },
                isError = uiState.cfCedulaError != null,
                supportingText = uiState.cfCedulaError ?: "",
            )
        }

        // Email
        DMOutlinedTextField(
            text = uiState.email,
            label = "Correo Electronico",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
            onChange = {
                viewModel.onEmailChange(it)
            },
            maxLines = 1,
            imeAction = ImeAction.Next,
        )

        // Phone
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



        if (uiState.customerType != FeCustomerType.FOREIGNER) {
            // Province
            DMDropDownField(
                label = "Provincia",
                items = uiState.provinceOptions.map { province -> province },
                selectedIndex = uiState.provinceOptions.indexOfFirst { province -> province == uiState.selectedProvince },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                onItemSelected = { index, _ -> viewModel.onProvinceChange(uiState.provinceOptions[index]) },
                isError = false,
            )

            // District
            DMDropDownField(
                label = "Distrito",
                items = uiState.districtOptions.map { province -> province },
                selectedIndex = uiState.districtOptions.indexOfFirst { district -> district == uiState.selectedDistrict },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                onItemSelected = { index, _ -> viewModel.onDistrictChange(uiState.districtOptions[index]) },
                isError = false,
                enabled = uiState.selectedProvince != null,
            )

            // Corregimiento
            DMDropDownField(
                label = "Corregimiento",
                items = uiState.corregOptions.map { province -> province },
                selectedIndex = uiState.corregOptions.indexOfFirst { correg -> correg == uiState.selectedCorreg },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                onItemSelected = { index, _ -> viewModel.onCorregimientoChange(uiState.corregOptions[index]) },
                isError = false,
                enabled = uiState.selectedDistrict != null,
            )

            // Address line
            DMOutlinedTextField(
                text = uiState.addressLine ?: "",
                label = "Direccion",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
                onChange = {
                    viewModel.onAddressLineChange(it)
                },
                maxLines = 1,
                imeAction = ImeAction.Next,
            )
        }

        // Country for foreigners
        if (uiState.customerType == FeCustomerType.FOREIGNER) {
            DMDropDownField(
                label = "Country",
                items = uiState.provinceOptions.map { province -> province },
                selectedIndex = uiState.provinceOptions.indexOfFirst { province -> province == uiState.selectedProvince },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                onItemSelected = { index, _ -> viewModel.onProvinceChange(uiState.provinceOptions[index]) },
                isError = false,
            )
        }

        ButtonM(
            onClick = {
//                viewModel.createCustomer()
            }
        ) {
            Text(text = "Crear cliente")
        }

//        DMChipTextField(
//            modifier =  Modifier
//                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
//                .fillMaxWidth(),
//            label = stringResource(Res.string.pos_clients_tag),
//            chips = uiState.tags ?: listOf(),
//            onAddChip = { tag ->
//                val tags = uiState.tags?.toMutableList() ?: mutableListOf()
//                if (!tags.contains(tag)) {
//                    tags.add(tag)
//                }
//                viewModel.onTagsChange(tags)
//            },
//            onRemoveChip = {
//                val tags = uiState.tags?.toMutableList() ?: mutableListOf()
//                if (tags.contains(it)) {
//                    tags.remove(it)
//                }
//                viewModel.onTagsChange(tags)
//            }
//        )


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
