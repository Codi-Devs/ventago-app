package com.teco.ventago.features.inventory.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.labelSmall

@Composable
fun InventoryProductAutocompleteField(
    query: String,
    suggestions: List<InventoryProductOption>,
    onQueryChange: (String) -> Unit,
    onSelect: (InventoryProductOption) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Producto",
    helperText: String = "Busca por nombre, SKU o código de barras.",
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DMOutlinedTextField(
            text = query,
            label = label,
            onChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            helperText,
            style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(top = 2.dp, start = 4.dp),
        )
        when {
            suggestions.isNotEmpty() -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        suggestions.forEachIndexed { index, option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(option) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Inventory2,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = InventoryOpsSupport.productLabel(option),
                                        style = bodyMedium(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    InventoryOpsSupport.productSecondaryLine(option)?.let { secondary ->
                                        Text(
                                            text = secondary,
                                            style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                            if (index < suggestions.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }
            query.trim().isNotEmpty() -> {
                Text(
                    "Sin coincidencias.",
                    style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                )
            }
        }
    }
}
