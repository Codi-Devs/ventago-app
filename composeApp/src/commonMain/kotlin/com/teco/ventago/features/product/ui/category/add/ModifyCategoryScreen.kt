package com.teco.ventago.features.product.ui.category.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.organism.LoadingBottomSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.headlineSmall
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.features.product.ui.category.add.viewmodel.ModifyCategoryViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.description_optional
import ventago.composeapp.generated.resources.edit
import ventago.composeapp.generated.resources.name
import ventago.composeapp.generated.resources.name_not_valid


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModifyCategoryScreen(
    viewModel: ModifyCategoryViewModel = koinViewModel<ModifyCategoryViewModel>(),
    navigateBack: () -> Unit) {

    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        DMOutlinedTextField(
            text = viewModel.state.name.value,
            label = stringResource(Res.string.name),
            modifier = Modifier.padding(bottom = 8.dp),
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            onChange = { newName ->
                viewModel.onNameChanged(newName)
            },
            isError = viewModel.state.wrongName.value,
            supportingText = if (viewModel.state.wrongName.value) stringResource(Res.string.name_not_valid) else "",
        )
        DMOutlinedTextField(
            text = viewModel.state.description.value,
            label = stringResource(Res.string.description_optional),
            modifier = Modifier.padding(bottom = 8.dp),
            keyboardType = KeyboardType.Text,
            maxLines = 6,
            imeAction = ImeAction.Done,
            onChange = { newDesc ->
                viewModel.onDescriptionChanged(newDesc)
            },
            isError = false)

        if (viewModel.state.canManageCategories.value) {
            ButtonM(onClick = {
                viewModel.saveCategory()
            }) {
                Text(
                    text = stringResource(Res.string.edit), style = TextStyle(
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

    }

    if (viewModel.state.loadingState.value.isLoading()) {
        LoadingBottomSheet(loadingState = viewModel.state.loadingState, sheetState = loadingSheetState) {
            viewModel.loadingDone()
            navigateBack()
        }
    }
}
