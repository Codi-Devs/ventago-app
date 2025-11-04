package com.teco.ventago.features.product.ui.category.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.organism.LoadingBottomSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.headlineSmall
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.features.product.ui.category.add.viewmodel.AddCategoryViewModel
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.add_new_category
import ventago.composeapp.generated.resources.description_optional
import ventago.composeapp.generated.resources.description_optional_desc
import ventago.composeapp.generated.resources.help
import ventago.composeapp.generated.resources.name
import ventago.composeapp.generated.resources.name_not_valid
import ventago.composeapp.generated.resources.understood


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryScreen(
    viewModel: AddCategoryViewModel = koinViewModel<AddCategoryViewModel>(),
    navigateBack: () -> Unit,
) {
    var showHelpDialog by remember { mutableStateOf(false) }
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })
    val analytics = koinInject<AnalyticsService>()

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
            isError = false,
            trailingIcon = vectorResource(Res.drawable.help),
            trailingIconClick = {
                showHelpDialog = true
            })

        ButtonM(onClick = {
            analytics.logEvent("add_category")
            viewModel.createCategory()
        }) {
            Text(
                text = stringResource(Res.string.add_new_category), style = TextStyle(
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

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            confirmButton = {
                TextButtonS(label = stringResource(Res.string.understood)) {
                    showHelpDialog = false
                }
            },
            title = {
                Text(
                    text = stringResource(Res.string.description_optional),
                    style = headlineSmall()
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.description_optional_desc),
                    style = bodyMedium(MaterialTheme.colorScheme.onSurfaceVariant).merge(textAlign = TextAlign.Start),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }

    if (viewModel.state.loadingState.value.isLoading()) {
        LoadingBottomSheet(
            loadingState = viewModel.state.loadingState,
            sheetState = loadingSheetState
        ) {
            viewModel.loadingDone()
            navigateBack()
        }
    }
}

