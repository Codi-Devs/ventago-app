package com.teco.ventago.features.pos.ui.customer.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelLarge
import com.teco.ventago.design_system.theme.titleMedium
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.pos.ui.customer.search.viewmodel.SearchCustomerStateUiEvent
import com.teco.ventago.features.pos.ui.customer.search.viewmodel.SearchCustomerViewModel
import com.teco.ventago.navigation.PosScreens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.email
import ventago.composeapp.generated.resources.name

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchCustomerView(
    viewModel: SearchCustomerViewModel = koinViewModel<SearchCustomerViewModel>(),
    navigate: (PosScreens) -> Unit,
) {

    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SearchCustomerStateUiEvent.CustomerNotCreated -> TODO()
                SearchCustomerStateUiEvent.CustomerNotFound -> {
                    if (viewModel.uiState.value.canAddCustomerAction) {
                        navigate(PosScreens.AddCustomerScreen)
                    } else {
                        navigate(PosScreens.CustomersScreen)
                    }
                }
                is SearchCustomerStateUiEvent.CustomersFound -> {
                    navigate(PosScreens.CustomersScreen)
                }
                SearchCustomerStateUiEvent.RucNotFound -> TODO()
            }
        }
    }

    Column(modifier = Modifier
        .verticalScroll(rememberScrollState())
        .background(vanishedBackgroundColor())
        .fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(all = 16.dp),
            elevation = CardDefaults.elevatedCardElevation(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardContainerColor(),
            ),
            shape = RoundedCornerShape(10.dp),
            onClick = { })
        {
            Text(
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                text = "Buscar Cliente",
                style = titleMedium()
            )

            DMOutlinedTextField(
                text = uiState.rucSearch,
                "RUC",
                modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                onChange = { value ->
                    viewModel.onSearchRucChanged(value)
                },
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            )

            // Business Name
            DMOutlinedTextField(
                text = uiState.nameSearch,
                stringResource(Res.string.name),
                modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                onChange = { value ->
                    viewModel.onSearchNameChanged(value)
                },
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            )


            DMOutlinedTextField(
                text = uiState.emailSearch,
                stringResource(Res.string.email),
                modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                onChange = { value ->
                    viewModel.onSearchEmailChanged(value)
                },
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            )


            ButtonM(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
                    viewModel.search()
                }
            ) {
                Text(
                    text = "Buscar",
                    style = labelLarge().copy(color = MaterialTheme.colorScheme.onPrimary)
                )
            }


            // Business Address
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButtonS(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    label = "Ver todos"
                ) {
                    viewModel.seeAll()
                }
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
}
