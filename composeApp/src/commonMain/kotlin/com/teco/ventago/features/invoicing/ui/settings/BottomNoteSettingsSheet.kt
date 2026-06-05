package com.teco.ventago.features.invoicing.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.features.quotes.ui.settings.FormatToggleButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNoteSettingsSheet(
    title: String,
    body: String,
    includeOnInvoice: Boolean,
    configured: Boolean,
    titleError: String?,
    bodyError: String?,
    enabled: Boolean,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onIncludeChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val richTextState = rememberRichTextState()
    val currentSpanStyle = richTextState.currentSpanStyle
    val isBold = currentSpanStyle.fontWeight == FontWeight.Bold
    val isItalic = currentSpanStyle.fontStyle == FontStyle.Italic
    val isUnderline = currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true
    val isOrderedList = richTextState.isOrderedList
    val isUnorderedList = richTextState.isUnorderedList
    var editBaselineHtml by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(body) {
        if (body != richTextState.toHtml()) {
            richTextState.setHtml(body)
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
                    onBodyChange(html)
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Texto predeterminado para facturas",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Define el título y texto que podrá agregarse al pie de tus facturas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(16.dp))

        DMOutlinedTextField(
            text = title,
            label = "Título",
            modifier = Modifier.fillMaxWidth(),
            onChange = onTitleChange,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            supportingText = titleError ?: "${title.length}/255",
            isError = titleError != null,
            enabled = enabled,
        )

        Spacer(Modifier.height(12.dp))
        Text(
            text = "Texto de la factura",
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormatToggleButton(
                selected = isBold,
                enabled = enabled,
                onClick = { richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                icon = { Icon(Icons.Filled.FormatBold, contentDescription = "Negrita") },
            )
            FormatToggleButton(
                selected = isItalic,
                enabled = enabled,
                onClick = { richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                icon = { Icon(Icons.Filled.FormatItalic, contentDescription = "Itálica") },
            )
            FormatToggleButton(
                selected = isUnderline,
                enabled = enabled,
                onClick = { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
                icon = { Icon(Icons.Filled.FormatUnderlined, contentDescription = "Subrayado") },
            )
            FormatToggleButton(
                selected = isUnorderedList,
                enabled = enabled,
                onClick = { richTextState.toggleUnorderedList() },
                icon = { Icon(Icons.Filled.FormatListBulleted, contentDescription = "Lista") },
            )
            FormatToggleButton(
                selected = isOrderedList,
                enabled = enabled,
                onClick = { richTextState.toggleOrderedList() },
                icon = { Icon(Icons.Filled.FormatListNumbered, contentDescription = "Lista numerada") },
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
            enabled = enabled,
            minLines = 5,
            maxLines = 10,
            supportingText = {
                Text(
                    text = bodyError ?: "${body.length}/3000",
                    color = if (bodyError != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = includeOnInvoice,
                onCheckedChange = onIncludeChange,
                enabled = enabled,
            )
            Text(
                text = "Incluir este texto en las facturas por defecto",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(16.dp))
        ButtonM(
            onClick = onSave,
            enabled = enabled,
        ) {
            Text(if (configured) "Guardar cambios" else "Crear texto predeterminado")
        }
        if (configured) {
            Spacer(Modifier.height(8.dp))
            OutlinedButtonM(
                onClick = onDelete,
                enabled = enabled,
                contentColor = MaterialTheme.colorScheme.error,
            ) {
                Text("Eliminar configuración")
            }
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cancelar")
        }
    }
}
