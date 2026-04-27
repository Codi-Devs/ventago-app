package com.teco.ventago.features.quotes.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import coil3.compose.AsyncImage
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.features.quotes.ui.quoteStyleOptions
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.quote_additional_info_hint
import ventago.composeapp.generated.resources.quote_settings
import ventago.composeapp.generated.resources.default_additional_info
import ventago.composeapp.generated.resources.default_quote_style
import ventago.composeapp.generated.resources.quote_prefix
import ventago.composeapp.generated.resources.preview_quote_styles
import ventago.composeapp.generated.resources.quote_style_preview_title
import ventago.composeapp.generated.resources.use_quote_style
import ventago.composeapp.generated.resources.ic_arrow_forward_ios

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteSettingsSection(
    additionalInfo: String,
    style: String,
    quotePrefix: String,
    enabled: Boolean = true,
    onAdditionalInfoChange: (String) -> Unit,
    onStyleChange: (String) -> Unit,
    onQuotePrefixChange: (String) -> Unit,
) {
    val richTextState = rememberRichTextState()
    val styleOptions = quoteStyleOptions()
    val currentSpanStyle = richTextState.currentSpanStyle
    val isBold = currentSpanStyle.fontWeight == FontWeight.Bold
    val isItalic = currentSpanStyle.fontStyle == FontStyle.Italic
    val isUnderline = currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true
    val isOrderedList = richTextState.isOrderedList
    val isUnorderedList = richTextState.isUnorderedList
    val additionalInfoHint = stringResource(Res.string.quote_additional_info_hint)
    var showPreview by remember { mutableStateOf(false) }
    var editBaselineHtml by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(additionalInfo) {
        if (additionalInfo != richTextState.toHtml()) {
            richTextState.setHtml(additionalInfo)
        }
        editBaselineHtml = null
    }

    LaunchedEffect(richTextState) {
        snapshotFlow { richTextState.toHtml() }
            .distinctUntilChanged()
            .collectLatest { html ->
                val baseline = editBaselineHtml ?: return@collectLatest
                if (html != baseline) {
                    editBaselineHtml = html
                    onAdditionalInfoChange(html)
                }
            }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor()
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.quote_settings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_forward_ios),
                    contentDescription = if (expanded) "Colapsar" else "Expandir",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(if (expanded) 90f else 0f)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(160)) + expandVertically(animationSpec = tween(160)),
                exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = tween(120))
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.default_additional_info),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FormatToggleButton(
                            selected = isBold,
                            enabled = enabled,
                            onClick = { richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                            icon = { Icon(Icons.Filled.FormatBold, contentDescription = "Bold") }
                        )
                        FormatToggleButton(
                            selected = isItalic,
                            enabled = enabled,
                            onClick = { richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                            icon = { Icon(Icons.Filled.FormatItalic, contentDescription = "Italic") }
                        )
                        FormatToggleButton(
                            selected = isUnderline,
                            enabled = enabled,
                            onClick = { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
                            icon = { Icon(Icons.Filled.FormatUnderlined, contentDescription = "Underline") }
                        )
                        FormatToggleButton(
                            selected = isUnorderedList,
                            enabled = enabled,
                            onClick = { richTextState.toggleUnorderedList() },
                            icon = { Icon(Icons.Filled.FormatListBulleted, contentDescription = "Bullet list") }
                        )
                        FormatToggleButton(
                            selected = isOrderedList,
                            enabled = enabled,
                            onClick = { richTextState.toggleOrderedList() },
                            icon = { Icon(Icons.Filled.FormatListNumbered, contentDescription = "Numbered list") }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedRichTextEditor(
                        state = richTextState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focus ->
                                if (focus.isFocused && editBaselineHtml == null) {
                                    editBaselineHtml = richTextState.toHtml()
                                }
                                if (!focus.isFocused) {
                                    editBaselineHtml = null
                                }
                            },
                        supportingText = { Text(additionalInfoHint) },
                        minLines = 4,
                        maxLines = 8,
                        enabled = enabled
                    )

                    Spacer(Modifier.height(12.dp))
                    DMOutlinedTextField(
                        text = quotePrefix,
                        label = stringResource(Res.string.quote_prefix),
                        modifier = Modifier.fillMaxWidth(),
                        onChange = { value ->
                            onQuotePrefixChange(value.take(5))
                        },
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                        isError = false,
                        enabled = enabled
                    )

                    Spacer(Modifier.height(12.dp))
                    val selectedStyleIndex = styleOptions.indexOfFirst { it.key == style }.takeIf { it >= 0 } ?: 0
                    DMDropDownField(
                        label = stringResource(Res.string.default_quote_style),
                        items = styleOptions.map { it.label },
                        selectedIndex = selectedStyleIndex,
                        modifier = Modifier.fillMaxWidth(),
                        onItemSelected = { index, _ -> onStyleChange(styleOptions[index].key) },
                        enabled = enabled
                    )

                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        onClick = { showPreview = true }
                    ) {
                        Text(text = stringResource(Res.string.preview_quote_styles))
                    }
                }
            }
        }
    }

    if (showPreview) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPreview = false },
            sheetState = sheetState
        ) {
            Text(
                text = stringResource(Res.string.quote_style_preview_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val columns = if (maxWidth < 600.dp) 1 else 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(styleOptions) { option ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardContainerColor())
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                AsyncImage(
                                    model = option.previewUrl,
                                    contentDescription = option.label,
                                    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                                    error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(8.5f / 11f)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                TextButton(
                                    onClick = {
                                        onStyleChange(option.key)
                                        showPreview = false
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    if (option.key == style) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(text = stringResource(Res.string.use_quote_style))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormatToggleButton(
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(onClick = onClick, enabled = enabled) {
        CompositionLocalProvider(LocalContentColor provides tint) {
            icon()
        }
    }
}
