package com.teco.ventago.features.product.ui.category.manage

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
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.molecules.CategoryItem
import com.teco.ventago.design_system.organism.LoadingBottomSheet
import com.teco.ventago.design_system.theme.Gray70
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.headlineSmall
import com.teco.ventago.design_system.theme.titleLarge
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.product.domain.model.Category
import com.teco.ventago.features.product.ui.category.manage.viewmodel.CategoriesManageViewModel
import com.teco.ventago.navigation.PosScreens
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.active
import ventago.composeapp.generated.resources.cancel
import ventago.composeapp.generated.resources.delete
import ventago.composeapp.generated.resources.delete_category
import ventago.composeapp.generated.resources.delete_category_ask
import ventago.composeapp.generated.resources.inactive


@Composable
fun CategoriesManageActions(navigate: (PosScreens) -> Unit) {
    IconButton(onClick = {
        navigate(PosScreens.AddCategoryScreen)
    }) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesManageScreen(
    viewModel: CategoriesManageViewModel = koinViewModel<CategoriesManageViewModel>(),
    navigate: (PosScreens) -> Unit
) {

    var list by remember { viewModel.state.categories }

    val snackbarHostState = remember { SnackbarHostState() }

    val lazyListState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(confirmValueChange = { false })
    var showCategoryEdit by remember { mutableStateOf(false) }
    val categoryEditSheetState = rememberModalBottomSheetState()
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    val scope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = vanishedBackgroundColor(), shape = RoundedCornerShape(12.dp)
                )
                .padding(top = 8.dp, bottom = 8.dp),
            state = lazyListState,
        ) {
            items(list, key = { it.id }) { item ->
                CategoryItem(modifier = Modifier.padding(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp
                ), category = item, reordering = false, onClick = {
                    viewModel.selectCategory(item)
                    navigate(PosScreens.EditCategoryScreen)
                }, onOptionsClick = {
                    selectedCategory = item
                    showCategoryEdit = true
                })
            }
        }
    }

    if (viewModel.state.loadingState.value.isLoading()) {
        LoadingBottomSheet(loadingState = viewModel.state.loadingState, sheetState = sheetState) {
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
                    viewModel.removeCategory(selectedCategory?.id ?: -1)
                    scope.launch { categoryEditSheetState.hide() }.invokeOnCompletion {
                        if (!categoryEditSheetState.isVisible) {
                            showCategoryEdit = false
                            selectedCategory = null
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
                    text = stringResource(Res.string.delete_category), style = headlineSmall()
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.delete_category_ask),
                    style = bodyMedium(MaterialTheme.colorScheme.onSurfaceVariant).merge(textAlign = TextAlign.Start),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }

    if (showCategoryEdit) {
        ModalBottomSheet(containerColor = MaterialTheme.colorScheme.background, onDismissRequest = {
            showCategoryEdit = false
            selectedCategory = null
        }, sheetState = categoryEditSheetState) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (selectedCategory?.active == true) stringResource(Res.string.active) else stringResource(Res.string.inactive),
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
                            Switch(enabled = !selectedCategory?.items.isNullOrEmpty(), checked = selectedCategory?.active == true, onCheckedChange = {
                                viewModel.activateCategory(selectedCategory?.id ?: -1, it)
                                scope.launch { categoryEditSheetState.hide() }.invokeOnCompletion {
                                    if (!categoryEditSheetState.isVisible) {
                                        showCategoryEdit = false
                                        selectedCategory = null
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