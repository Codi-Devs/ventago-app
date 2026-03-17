package com.teco.ventago.features.product.ui.category.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.molecules.ItemRow
import com.teco.ventago.design_system.organism.LoadingBottomSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.Gray70
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.headlineSmall
import com.teco.ventago.design_system.theme.titleLarge
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.ui.category.edit.viewmodel.EditCategoryViewModel
import com.teco.ventago.navigation.PosScreens
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.active
import ventago.composeapp.generated.resources.cancel
import ventago.composeapp.generated.resources.delete
import ventago.composeapp.generated.resources.delete_item_ask
import ventago.composeapp.generated.resources.inactive
import ventago.composeapp.generated.resources.search


@Composable
fun EditCategoryActions(
    navigate: (PosScreens) -> Unit,
    viewModel: EditCategoryViewModel = koinViewModel<EditCategoryViewModel>(),
) {
    if (viewModel.state.canManageCategories.value) {
        IconButton(onClick = {
            navigate(PosScreens.AddItemScreen)
        }) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoryScreen(
    viewModel: EditCategoryViewModel = koinViewModel<EditCategoryViewModel>(),
    navigateBack: () -> Unit,
    navigate: (PosScreens) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    

    var list by remember { viewModel.state.items }

    val lazyListState = rememberLazyListState()
    
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })

    var showItemEdit by remember { mutableStateOf(false) }
    val itemEditSheetState = rememberModalBottomSheetState()
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = viewModel.state.selectedCategory.value?.name ?: "", style = titleLarge()
            )
            if (viewModel.state.canManageCategories.value) {
                IconButton(onClick = {
                    navigate(PosScreens.ModifyCategoryScreen)
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        DMOutlinedTextField(
            text = viewModel.state.query.value,
            label = stringResource(Res.string.search),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
            onChange = {
                viewModel.filterItems(it)
            },
            maxLines = 1,
            imeAction = ImeAction.Done,
            leadingIcon = Icons.Rounded.Search,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = vanishedBackgroundColor(), shape = RoundedCornerShape(12.dp)
                )
                .padding(top = 8.dp, bottom = 8.dp),
            state = lazyListState,
        ) {
            items(list, key = { it.itemId }) { item ->
                ItemRow(item = item,
                    currency = viewModel.state.currency.value,
                    modifier = Modifier.padding(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp
                    ),
                    reordering = false,
                    onClick = {
                        if (viewModel.state.canManageCategories.value) {
                            viewModel.selectItem(item.itemId)
                            navigate(PosScreens.EditItemScreen)
                        }
                    },
                    onOptionsClick = {
                        if (viewModel.state.canManageCategories.value) {
                            selectedItem = item
                            showItemEdit = true
                        }
                    })
            }
        }
    }

    if (viewModel.state.loadingState.value.isLoading()) {
        LoadingBottomSheet(
            loadingState = viewModel.state.loadingState, sheetState = loadingSheetState
        ) {
            viewModel.loadingDone()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            confirmButton = {
                TextButtonS(label = stringResource(Res.string.delete)) {
                    showDeleteDialog = false
                    viewModel.removeItem(selectedItem?.itemId ?: -1)
                    scope.launch { itemEditSheetState.hide() }.invokeOnCompletion {
                        if (!itemEditSheetState.isVisible) {
                            showItemEdit = false
                            selectedItem = null
                        }
                    }
                }
            },
            dismissButton = {
                TextButtonS(label = stringResource(Res.string.cancel)) {
                    showDeleteDialog = false
                }
            },
            title = {
                Text(
                    text = stringResource(Res.string.delete), style = headlineSmall()
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.delete_item_ask),
                    style = bodyMedium(MaterialTheme.colorScheme.onSurfaceVariant).merge(textAlign = TextAlign.Start),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }

    if (showItemEdit) {
        ModalBottomSheet(containerColor = MaterialTheme.colorScheme.background, onDismissRequest = {
            showItemEdit = false
            selectedItem = null
        }, sheetState = itemEditSheetState) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (selectedItem?.active == true) stringResource(Res.string.active) else stringResource(
                        Res.string.inactive
                    ),
                    modifier = Modifier.padding(bottom = 16.dp),
                    style = titleLarge()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .height(56.dp)
                            .padding(end = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                        border = BorderStroke(1.dp, Gray70),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Switch(checked = selectedItem?.active == true, onCheckedChange = {
                                viewModel.activateItem(selectedItem?.itemId ?: -1, it)
                                scope.launch { itemEditSheetState.hide() }.invokeOnCompletion {
                                    if (!itemEditSheetState.isVisible) {
                                        showItemEdit = false
                                        selectedItem = null
                                    }
                                }
                            })
                        }
                    }

                    ButtonM(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .padding(start = 8.dp),
                        onClick = {
                            showDeleteDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.error,
                    ) {
                        Text(text = stringResource(Res.string.delete))
                    }
                }
            }
        }
    }
}
