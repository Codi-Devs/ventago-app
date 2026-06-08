package com.teco.ventago.features.settings.ui.name

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.organism.LoadingBottomSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.buttonMBold
import com.teco.ventago.features.settings.ui.name.viewmodel.ChangeNameViewModel
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.change_business_name
import ventago.composeapp.generated.resources.name

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeNameContent(
    viewModel: ChangeNameViewModel
) {
    var name by remember { mutableStateOf(viewModel.state.name.value) }
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        DMOutlinedTextField(
            text = name,
            stringResource(Res.string.name),
            modifier = Modifier.padding(bottom = 16.dp, top = 8.dp),
            onChange = {
                name = it
                viewModel.state.isError.value = false
            },
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
            isError = remember { viewModel.state.isError }.value
        )
        
        ButtonM(
            modifier = Modifier.padding(bottom = 8.dp),
            onClick = {
            viewModel.changeBusinessName(name)
        }) {
            Text(
                text = stringResource(Res.string.change_business_name),
                style = buttonMBold(),
            )
        }
    }

    if (viewModel.state.loadingState.value.isLoading()) {
        LoadingBottomSheet(
            loadingState = viewModel.state.loadingState,
            sheetState = loadingSheetState
        ) {
            viewModel.loadingDone()
        }
    }
}