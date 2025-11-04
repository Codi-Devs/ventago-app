package com.teco.ventago.features.product.ui.item.edit

import androidx.compose.runtime.Composable
import com.teco.ventago.design_system.organism.ItemScreenContent
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.edit
import ventago.composeapp.generated.resources.edit_name

@Composable
fun EditItemScreen(
    viewModel: EditItemViewModel = koinViewModel<EditItemViewModel>(),
    navigateBack: () -> Unit,
    ) {
    ItemScreenContent(
        viewModel,
        navigateBack,
        stringResource(Res.string.edit)
    )
}