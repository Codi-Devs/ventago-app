package com.teco.ventago.features.expenses.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.features.expenses.domain.ExpenseAccountTreeNode
import com.teco.ventago.features.expenses.domain.buildExpenseAccountsTree
import com.teco.ventago.features.expenses.domain.models.ExpenseAccount

private val ConceptSearchWhitespaceRegex = "\\s+".toRegex()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAccountSelectorField(
    label: String,
    selectedText: String,
    placeholder: String,
    accounts: List<ExpenseAccount>,
    isLoading: Boolean,
    leafOnly: Boolean,
    emptyOptionLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hint: String? = null,
    clickHintLabel: String? = null,
    selectedTextColor: Color = MaterialTheme.colorScheme.onSurface,
    placeholderTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onSelected: (Long?, String?) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { showSheet = true },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = bodyMediumBold())
            clickHintLabel?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, style = labelSmall(color = MaterialTheme.colorScheme.secondary))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = selectedText.ifBlank { placeholder },
                style = bodyMedium(
                    if (selectedText.isBlank()) placeholderTextColor
                    else selectedTextColor
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            hint?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            ExpenseAccountPickerSheetContent(
                accounts = accounts,
                isLoading = isLoading,
                leafOnly = leafOnly,
                emptyOptionLabel = emptyOptionLabel,
                onSelected = { accountId, accountName ->
                    onSelected(accountId, accountName)
                    showSheet = false
                }
            )
        }
    }
}

@Composable
fun ExpenseAccountPickerSheetContent(
    accounts: List<ExpenseAccount>,
    isLoading: Boolean,
    leafOnly: Boolean,
    emptyOptionLabel: String,
    onSelected: (Long?, String?) -> Unit
) {
    val tree = remember(accounts) { buildExpenseAccountsTree(accounts) }
    var searchQuery by remember { mutableStateOf("") }
    val normalizedSearchQuery = remember(searchQuery) { searchQuery.normalizeConceptSearchTerm() }
    val filteredTree = remember(tree, normalizedSearchQuery) {
        filterExpenseAccountTree(tree, normalizedSearchQuery)
    }
    var expandedParentIds by remember(tree) { mutableStateOf(collectExpandableParentIds(tree)) }
    val expandedSearchParentIds = remember(filteredTree, normalizedSearchQuery) {
        if (normalizedSearchQuery.isBlank()) emptySet() else collectExpandableParentIds(filteredTree)
    }
    val visibleExpandedParentIds = if (normalizedSearchQuery.isBlank()) {
        expandedParentIds
    } else {
        expandedSearchParentIds
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Selecciona un concepto", style = bodyMediumBold())
        DMOutlinedTextField(
            text = searchQuery,
            label = "Buscar por nombre",
            modifier = Modifier.fillMaxWidth(),
            onChange = { searchQuery = it },
            leadingIcon = Icons.Rounded.Search,
            trailingIcon = if (searchQuery.isNotBlank()) Icons.Rounded.Close else null,
            trailingIconClick = { searchQuery = "" }
        )

        if (isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        AccountOptionRow(
            title = emptyOptionLabel,
            subtitle = null,
            depth = 0,
            enabled = true,
            isParent = false,
            onClick = { onSelected(null, null) }
        )

        Divider()

        LazyColumn {
            item {
                if (filteredTree.isEmpty()) {
                    Text(
                        text = "No se encontraron conceptos",
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    filteredTree.forEachIndexed { index, node ->
                        ExpenseAccountTreeNodeView(
                            node = node,
                            leafOnly = leafOnly,
                            expandedParentIds = visibleExpandedParentIds,
                            onToggleParent = { accountId ->
                                expandedParentIds = if (expandedParentIds.contains(accountId)) {
                                    expandedParentIds - accountId
                                } else {
                                    expandedParentIds + accountId
                                }
                            },
                            onSelected = onSelected
                        )
                        if (index < filteredTree.lastIndex) {
                            Divider()
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ExpenseAccountTreeNodeView(
    node: ExpenseAccountTreeNode,
    leafOnly: Boolean,
    expandedParentIds: Set<Long>,
    onToggleParent: (Long) -> Unit,
    onSelected: (Long?, String?) -> Unit
) {
    val isParent = node.children.isNotEmpty()
    val isExpanded = !isParent || expandedParentIds.contains(node.account.id)
    val enabled = if (leafOnly) node.isLeaf else true
    AccountOptionRow(
        title = node.account.name,
        subtitle = if (isParent) "Cuenta padre" else null,
        depth = node.depth,
        enabled = enabled,
        isParent = isParent,
        isExpanded = isExpanded,
        onClick = {
            when {
                enabled -> onSelected(node.account.id, node.account.name)
                isParent -> onToggleParent(node.account.id)
            }
        },
        onToggleParent = if (isParent) {
            { onToggleParent(node.account.id) }
        } else {
            null
        }
    )

    if (isParent && isExpanded) {
        Spacer(modifier = Modifier.height(4.dp))
        node.children.forEachIndexed { index, child ->
            ExpenseAccountTreeNodeView(
                node = child,
                leafOnly = leafOnly,
                expandedParentIds = expandedParentIds,
                onToggleParent = onToggleParent,
                onSelected = onSelected
            )
            if (index < node.children.lastIndex) {
                Divider(modifier = Modifier.padding(start = ((child.depth + 1) * 16).dp))
            }
        }
    }
}

@Composable
private fun AccountOptionRow(
    title: String,
    subtitle: String?,
    depth: Int,
    enabled: Boolean,
    isParent: Boolean,
    isExpanded: Boolean = false,
    onClick: () -> Unit,
    onToggleParent: (() -> Unit)? = null
) {
    val rowClickable = enabled || onToggleParent != null
    val titleColor = when {
        rowClickable -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val backgroundColor = if (isParent) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .clickable(enabled = rowClickable, onClick = onClick)
            .padding(start = (depth * 16).dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = bodyMediumBold().copy(color = titleColor),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(it, style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }
        }
        if (isParent && onToggleParent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onToggleParent,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Rounded.KeyboardArrowDown
                    } else {
                        Icons.Rounded.KeyboardArrowRight
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun String.normalizeConceptSearchTerm(): String {
    return lowercase().replace(ConceptSearchWhitespaceRegex, "")
}

private fun filterExpenseAccountTree(
    nodes: List<ExpenseAccountTreeNode>,
    normalizedSearchQuery: String
): List<ExpenseAccountTreeNode> {
    if (normalizedSearchQuery.isBlank()) return nodes
    return nodes.mapNotNull { node ->
        filterExpenseAccountNode(node, normalizedSearchQuery)
    }
}

private fun filterExpenseAccountNode(
    node: ExpenseAccountTreeNode,
    normalizedSearchQuery: String
): ExpenseAccountTreeNode? {
    val filteredChildren = node.children.mapNotNull { child ->
        filterExpenseAccountNode(child, normalizedSearchQuery)
    }
    val nameMatches = node.account.name.normalizeConceptSearchTerm().contains(normalizedSearchQuery)

    return when {
        nameMatches -> node
        filteredChildren.isNotEmpty() -> node.copy(children = filteredChildren, isLeaf = false)
        else -> null
    }
}

private fun collectExpandableParentIds(nodes: List<ExpenseAccountTreeNode>): Set<Long> {
    val ids = mutableSetOf<Long>()

    fun traverse(node: ExpenseAccountTreeNode) {
        if (node.children.isNotEmpty()) {
            ids.add(node.account.id)
            node.children.forEach(::traverse)
        }
    }

    nodes.forEach(::traverse)
    return ids
}
