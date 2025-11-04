package com.teco.ventago.features.auth.ui.register.business

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.organism.LoadingBottomSheet
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.features.auth.ui.register.business.viewmodel.BusinessRegisterViewModel
import com.teco.ventago.navigation.PosScreens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.business_name_hint
import ventago.composeapp.generated.resources.country
import ventago.composeapp.generated.resources.currency
import ventago.composeapp.generated.resources.name_not_valid
import ventago.composeapp.generated.resources.register_business
import ventago.composeapp.generated.resources.welcome_user_name
import ventago.composeapp.generated.resources.welcome_user_name_desc

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessRegisterScreen(
    navigate: (PosScreens) -> Unit,
) {
    val viewModel: BusinessRegisterViewModel = koinViewModel<BusinessRegisterViewModel>()

    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })

    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            modifier = Modifier.padding(bottom = 12.dp),
            text = "${stringResource(Res.string.welcome_user_name)} ${uiState.userName}",
            style = TextStyle(
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontFamily = latoFontFamily(),
                fontWeight = FontWeight(700),
                textAlign = TextAlign.Center,
            )
        )

        Text(
            modifier = Modifier.padding(bottom = 32.dp),
            text = stringResource(Res.string.welcome_user_name_desc),
            style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 24.sp,
                fontFamily = latoFontFamily(),
                fontWeight = FontWeight(400),
                textAlign = TextAlign.Center,
                letterSpacing = 0.02.sp,
            )
        )

        DMOutlinedTextField(
            text = uiState.ruc,
            label = "RUC *",
            modifier = Modifier.padding(bottom = 0.dp),
            keyboardType = KeyboardType.Ascii,
            maxLines = 1,
            imeAction = ImeAction.Search,
            onChange = { newRuc ->
                viewModel.rucChanged(newRuc)
            },
            isError = uiState.invalidRuc,
            supportingText = "Introduzca el RUC de su negocio sin el DV.",
            trailingIcon = Icons.Rounded.PersonSearch,
            trailingIconClick = {
                viewModel.validateRUC()
            }
        )

        DMOutlinedTextField(
            text = uiState.name,
            label = stringResource(Res.string.business_name_hint) + " *",
            modifier = Modifier.padding(bottom = 0.dp),
            keyboardType = KeyboardType.Text,
            maxLines = 1,
            imeAction = ImeAction.Next,
            onChange = { newBusinessName ->
                viewModel.nameChanged(newBusinessName)
            },
            readOnly = true,
            isError = uiState.invalidName,
            supportingText = if (uiState.invalidName) stringResource(Res.string.name_not_valid) else ""
        )

        DMOutlinedTextField(
            text = uiState.businessPhone,
            label = "Business Phone",
            modifier = Modifier.padding(bottom = 0.dp),
            keyboardType = KeyboardType.Phone,
            maxLines = 1,
            imeAction = ImeAction.Next,
            onChange = { value ->
                viewModel.businessPhoneChanged(value)
            },
            isError = uiState.invalidBusinessPhone,
        )

        DMOutlinedTextField(
            text = uiState.businessEmail,
            label = "Business Email",
            modifier = Modifier.padding(bottom = 0.dp),
            keyboardType = KeyboardType.Email,
            maxLines = 1,
            imeAction = ImeAction.Next,
            onChange = { value ->
                viewModel.businessEmailChanged(value)
            },
            isError = uiState.invalidBusinessEmail,
        )

        DMOutlinedTextField(
            text = uiState.businessWeb,
            label = "Website",
            modifier = Modifier.padding(bottom = 0.dp),
            keyboardType = KeyboardType.Unspecified,
            maxLines = 1,
            imeAction = ImeAction.Done,
            onChange = { value ->
                viewModel.businessWebChanged(value)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // By default we select Panama and Balboa as country and currency for now
//        DMDropDownField(
//            label = stringResource(Res.string.country),
//            items = viewModel.countries.map { country -> country.country },
//            selectedIndex = uiState.selectedCountry,
//            modifier = Modifier.padding(bottom = 24.dp),
//            onItemSelected = { index, _ -> viewModel.countrySelected(index) },
//            isError = uiState.invalidCountry,
//        )
//
//        DMDropDownField(
//            label = stringResource(Res.string.currency),
//            items = viewModel.currencies.map { currency -> currency.getLabel() },
//            selectedIndex = uiState.selectedCurrency,
//            modifier = Modifier.padding(bottom = 24.dp),
//            onItemSelected = { index, _ -> viewModel.currencySelected(index) },
//            isError = uiState.invalidCurrency,
//        )

        ButtonM(
            modifier = Modifier.padding(bottom = 32.dp),
            onClick = {
                viewModel.register()
            }) {
            Text(
                text = stringResource(Res.string.register_business),
                style = TextStyle(
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

}