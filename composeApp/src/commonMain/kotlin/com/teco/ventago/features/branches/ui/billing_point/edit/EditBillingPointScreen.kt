package com.teco.ventago.features.branches.ui.billing_point.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.labelLarge
import com.teco.ventago.features.branches.ui.billing_point.edit.viewmodel.EditBillingPointStateUiEvent
import com.teco.ventago.features.branches.ui.billing_point.edit.viewmodel.EditBillingPointViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBillingPointScreen(
    viewModel: EditBillingPointViewModel,
    navigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EditBillingPointStateUiEvent.GoBack -> navigateBack()
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding(),
    ) {
        DMOutlinedTextField(
            text = uiState.name,
            label = "Nombre del punto de facturación",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp, top = 16.dp),
            onChange = {
                viewModel.onNameChange(it)
            },
            maxLines = 1,
            imeAction = ImeAction.Done,
        )

        Spacer(modifier = Modifier.weight(1f))

        ButtonM(
            onClick = {
                viewModel.editBillingPoint()
            },
            enabled = uiState.buttonEnabled,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Modificar",
                style = labelLarge().copy(color = MaterialTheme.colorScheme.onPrimary)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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