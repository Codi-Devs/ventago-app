package com.teco.ventago.features.expenses.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.teco.ventago.features.expenses.domain.compactExpenseAccountAncestors
import com.teco.ventago.features.expenses.domain.selectableExpenseAccounts
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
    compact: Boolean = false,
    selectedTextColor: Color = MaterialTheme.colorScheme.onSurface,
    placeholderTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onSelected: (Long?, String?) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hasSelection = selectedText.isNotBlank()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { showSheet = true },
        shape = RoundedCornerShape(if (compact) 14.dp else 12.dp),
        elevation = CardDefaults.cardElevation(if (compact) 0.dp else 2.dp),
        border = if (compact) {
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            )
        } else {
            null
        },
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        if (compact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasSelection) selectedText else "Sin concepto seleccionado",
                    style = bodyMedium(
                        if (hasSelection) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Seleccionar",
                    style = labelSmall(color = MaterialTheme.colorScheme.secondary)
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
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
    val options = remember(accounts, leafOnly) { selectableExpenseAccounts(accounts, leafOnly) }
    var searchQuery by remember { mutableStateOf("") }
    val normalizedSearchQuery = remember(searchQuery) { searchQuery.normalizeConceptSearchTerm() }
    val filteredOptions = remember(options, normalizedSearchQuery) {
        if (normalizedSearchQuery.isBlank()) {
            options
        } else {
            options.filter { option ->
                option.account.name.normalizeConceptSearchTerm().contains(normalizedSearchQuery) ||
                    option.ancestorNames.any { it.normalizeConceptSearchTerm().contains(normalizedSearchQuery) }
            }
        }
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
            onClick = { onSelected(null, null) }
        )

        Divider()

        LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
            if (filteredOptions.isEmpty()) {
                item {
                    Text(
                        text = "No se encontraron conceptos",
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(filteredOptions, key = { it.account.id }) { option ->
                    val path = compactExpenseAccountAncestors(option.ancestorNames)
                    AccountOptionRow(
                        title = option.account.name,
                        subtitle = path.ifBlank { null },
                        onClick = { onSelected(option.account.id, option.account.name) }
                    )
                    Divider()
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AccountOptionRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = bodyMediumBold(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        subtitle?.let {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = it,
                style = labelSmall(color = MaterialTheme.colorScheme.secondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun String.normalizeConceptSearchTerm(): String {
    return lowercase().replace(ConceptSearchWhitespaceRegex, "")
}

