package com.teco.ventago.design_system.molecules.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.labelSmall

@Composable
fun <T> ListAutocompleteSearchField(
    query: String,
    hasSelection: Boolean,
    isSearching: Boolean,
    items: List<T>,
    label: String,
    searchContentDescription: String,
    clearContentDescription: String,
    onQueryChanged: (String) -> Unit,
    onItemSelected: (T) -> Unit,
    onClear: () -> Unit,
    itemTitle: (T) -> String,
    itemSubtitle: (T) -> String,
    itemIcon: ImageVector? = null,
) {
    val normalizedQuery = query.trim()
    val showSuggestions = !hasSelection &&
        (isSearching || items.isNotEmpty() || normalizedQuery.length == 1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(2f)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            label = { Text(text = label, style = MaterialTheme.typography.bodyMedium) },
            trailingIcon = {
                AnimatedContent(
                    targetState = query.isNotBlank(),
                    transitionSpec = {
                        (fadeIn(tween(160)) + scaleIn(initialScale = 0.82f, animationSpec = tween(160)))
                            .togetherWith(
                                fadeOut(tween(120)) +
                                    scaleOut(targetScale = 0.82f, animationSpec = tween(120))
                            )
                    },
                    label = "list-autocomplete-search-icon",
                ) { filled ->
                    if (filled) {
                        IconButton(onClick = onClear) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = clearContentDescription,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = searchContentDescription,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            shape = RoundedCornerShape(12.dp),
        )

        if (showSuggestions) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    if (isSearching) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Buscando...",
                                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            )
                        }
                    } else if (normalizedQuery.length == 1) {
                        Text(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            text = "Escribe al menos 2 caracteres",
                            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                    }

                    items.forEachIndexed { index, item ->
                        Surface(
                            onClick = { onItemSelected(item) },
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                itemIcon?.let { icon ->
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = itemTitle(item),
                                        style = bodyMediumBold(color = MaterialTheme.colorScheme.onSurface),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    itemSubtitle(item).takeIf { it.isNotBlank() }?.let { subtitle ->
                                        Text(
                                            text = subtitle,
                                            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                        if (index < items.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
