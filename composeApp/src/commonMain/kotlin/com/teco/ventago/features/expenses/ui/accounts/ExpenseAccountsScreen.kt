package com.teco.ventago.features.expenses.ui.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.expenses.domain.buildExpenseAccountsTree
import com.teco.ventago.features.expenses.domain.models.ExpenseAccount
import com.teco.ventago.features.expenses.ui.components.ExpenseAccountSelectorField
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAccountsScreen(
    viewModel: ExpenseAccountsViewModel = koinViewModel<ExpenseAccountsViewModel>()
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })
    val formSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Conceptos de gasto", style = titleMediumBold())
            IconButton(onClick = { viewModel.loadAccounts(initial = false) }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Recargar", tint = MaterialTheme.colorScheme.secondary)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = uiState.showInactive,
                onCheckedChange = viewModel::setShowInactive,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.secondary,
                    checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                    checkedBorderColor = MaterialTheme.colorScheme.secondary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Mostrar inactivas", style = bodyMedium())
        }

        ButtonM(onClick = viewModel::startCreate) {
            Text("Nueva Cuenta")
        }

        if (uiState.isLoadingInitial) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val tree = buildExpenseAccountsTree(uiState.accounts)
            // Track which nodes are expanded; default all root nodes expanded
            val expandedIds = remember(tree) {
                mutableStateOf(tree.map { it.account.id }.toSet())
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tree) { node ->
                    ExpenseAccountTreeCard(
                        node = node,
                        expandedIds = expandedIds.value,
                        onToggleExpand = { id ->
                            expandedIds.value = if (id in expandedIds.value)
                                expandedIds.value - id
                            else
                                expandedIds.value + id
                        },
                        onEdit = viewModel::startEdit,
                        onDeactivate = viewModel::promptDeactivate
                    )
                }
            }
        }
    }

    if (uiState.showFormSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissForm,
            sheetState = formSheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            ExpenseAccountFormSheet(
                state = uiState,
                onCodeChange = viewModel::setFormCode,
                onNameChange = viewModel::setFormName,
                onParentSelected = viewModel::setFormParent,
                onActiveChange = viewModel::setFormActive,
                onSave = viewModel::save,
                onDismiss = viewModel::dismissForm
            )
        }
    }

    if (uiState.showDeactivateDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeactivateDialog,
            title = { Text("Desactivar concepto") },
            text = { Text("Antes de desactivar, confirma que deseas continuar.") },
            confirmButton = {
                TextButton(onClick = viewModel::deactivateSelectedAccount) {
                    Text("Desactivar")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeactivateDialog) {
                    Text("Cancelar")
                }
            }
        )
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

@Composable
private fun ExpenseAccountTreeCard(
    node: com.teco.ventago.features.expenses.domain.ExpenseAccountTreeNode,
    expandedIds: Set<Long>,
    onToggleExpand: (Long) -> Unit,
    onEdit: (ExpenseAccount) -> Unit,
    onDeactivate: (ExpenseAccount) -> Unit
) {
    val isExpanded = node.account.id in expandedIds
    val chevronAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "chevron")
    val hasChildren = node.children.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (node.depth * 8).dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chevron + name row
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (hasChildren) Modifier.clickable { onToggleExpand(node.account.id) }
                            else Modifier
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (hasChildren) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                            modifier = Modifier.size(18.dp).rotate(chevronAngle),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Spacer(modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(node.account.name, style = bodyMediumBold())
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusBadge(
                                if (node.account.isActive) "Activa" else "Inactiva",
                                if (node.account.isActive) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                            )
                            StatusBadge(
                                if (node.account.isSystem) "Sistema" else "Personalizada",
                                if (node.account.isSystem) MaterialTheme.colorScheme.primary else Color(0xFFFFA000)
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = { onEdit(node.account) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.secondary)
                    }
                    if (!node.account.isSystem && node.account.isActive) {
                        IconButton(onClick = { onDeactivate(node.account) }) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Desactivar",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded && hasChildren) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(6.dp))
                    node.children.forEach { child ->
                        ExpenseAccountTreeCard(
                            node = child,
                            expandedIds = expandedIds,
                            onToggleExpand = onToggleExpand,
                            onEdit = onEdit,
                            onDeactivate = onDeactivate
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Text(
        text = text,
        style = labelSmall(color = Color.White),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun ExpenseAccountFormSheet(
    state: ExpenseAccountsState,
    onCodeChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onParentSelected: (Long?, String?) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val selected = state.selectedAccount
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (state.isEditMode) "Editar concepto de gasto" else "Nuevo concepto de gasto",
            style = titleMediumBold()
        )

        DMOutlinedTextField(
            text = state.formCode,
            label = "Codigo",
            modifier = Modifier,
            onChange = onCodeChange,
            readOnly = state.isEditMode
        )

        DMOutlinedTextField(
            text = state.formName,
            label = "Nombre",
            modifier = Modifier,
            onChange = onNameChange,
            readOnly = selected?.isSystem == true
        )

        ExpenseAccountSelectorField(
            label = "Cuenta padre",
            selectedText = state.formParentName.orEmpty(),
            placeholder = "Sin cuenta padre",
            accounts = state.accounts,
            isLoading = false,
            leafOnly = false,
            emptyOptionLabel = "Sin cuenta padre",
            enabled = !state.isEditMode,
            onSelected = onParentSelected
        )

        if (state.isEditMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = state.formIsActive,
                    onCheckedChange = onActiveChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.secondary,
                        checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                        checkedBorderColor = MaterialTheme.colorScheme.secondary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Estado", style = bodyMedium())
            }
        }

        state.formError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = bodyMedium())
        }

        ButtonM(onClick = onSave) {
            Text("Guardar")
        }
        OutlinedButtonM(onClick = onDismiss) {
            Text("Cancelar")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
