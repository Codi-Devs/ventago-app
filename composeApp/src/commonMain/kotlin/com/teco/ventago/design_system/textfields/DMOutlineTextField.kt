package com.teco.ventago.design_system.textfields


import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.textfields.helpers.PrefixTransformation
import com.teco.ventago.design_system.textfields.helpers.getKeyboardType
import com.teco.ventago.design_system.theme.bodyLarge
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.utils.CurrencyAmountInputVisualTransformation

@Composable
fun DMOutlinedTextField(
    text: String,
    label: String,
    modifier: Modifier,
    onChange: (String) -> Unit,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    enabled: Boolean = true,
    supportingText: String = "",
    capitalization: KeyboardCapitalization? = null,
    prefix: String? = null,
    isError: Boolean = false,
    readOnly: Boolean = false,
    trailingIcon: ImageVector? = null,
    leadingIcon: ImageVector? = null,
    trailingIconClick: () -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    leadingIcon?.let {
        OutlinedTextField(
            value = text,
            onValueChange = onChange,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            label = {
                Text(label, style = bodyLarge())
                    },
            leadingIcon = {
                Icon(imageVector = leadingIcon, contentDescription = "")
            },
            trailingIcon = {
                if (trailingIcon != null) {
                    IconButton(onClick = trailingIconClick){
                        Icon(imageVector = trailingIcon, contentDescription = "")
                    }
                }
            },
            supportingText = {Text(text = supportingText)},
            isError = isError,
            maxLines = maxLines,
            singleLine = maxLines == 1,
            keyboardOptions = getKeyboardType(keyboardType, imeAction, capitalization),
            visualTransformation = prefix?.let { PrefixTransformation(it) }?:  visualTransformation,
        )
    } ?: run {
        OutlinedTextField(
            modifier = modifier.fillMaxWidth(),
            value = text,
            onValueChange = onChange,
            label = { Text(label, style = bodyLarge()) },
            isError = isError,
            maxLines = maxLines,
            singleLine = maxLines == 1,
            keyboardOptions = getKeyboardType(keyboardType, imeAction, capitalization),
            enabled = enabled,
            supportingText = { Text(text = supportingText) },
            readOnly = readOnly,
            visualTransformation = prefix?.let { PrefixTransformation(it) }?:  visualTransformation,
            trailingIcon = {
                if (trailingIcon != null) {
                    IconButton(onClick = trailingIconClick){
                        Icon(imageVector = trailingIcon, contentDescription = "")
                    }
                }
            },
        )
    }

}

@Composable
fun DMMoneyOutlinedTextField(
    text: String,
    label: String,
    modifier: Modifier,
    onChange: (String) -> Unit,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    enabled: Boolean = true,
    supportingText: String = "",
    capitalization: KeyboardCapitalization? = null,
    prefix: String? = null,
    isError: Boolean = false,
    readOnly: Boolean = false,
    trailingIcon: ImageVector? = null,
    leadingIcon: ImageVector? = null,
    trailingIconClick: () -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    leadingIcon?.let {
        OutlinedTextField(
            modifier = modifier.fillMaxWidth(),
            value = text,
            onValueChange = onChange,
            label = { Text(label, style = bodyLarge()) },
            isError = isError,
            maxLines = maxLines,
            singleLine = maxLines == 1,
            keyboardOptions = KeyboardOptions(keyboardType=KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            enabled = enabled,
            supportingText = { Text(text = supportingText) },
            readOnly = readOnly,
            visualTransformation = CurrencyAmountInputVisualTransformation(
                fixedCursorAtTheEnd = true,
                numberOfDecimals = 2
            ),
            trailingIcon = {
                if (trailingIcon != null) {
                    IconButton(onClick = trailingIconClick){
                        Icon(imageVector = trailingIcon, contentDescription = "")
                    }
                }
            },
            leadingIcon = {
                Icon(imageVector = leadingIcon, contentDescription = "")
            }
        )
    } ?: run {
        OutlinedTextField(
            modifier = modifier.fillMaxWidth(),
            value = text,
            onValueChange = onChange,
            label = { Text(label, style = bodyLarge()) },
            isError = isError,
            maxLines = maxLines,
            singleLine = maxLines == 1,
            keyboardOptions = KeyboardOptions(keyboardType=KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            enabled = enabled,
            supportingText = { Text(text = supportingText) },
            readOnly = readOnly,
            visualTransformation = CurrencyAmountInputVisualTransformation(
                fixedCursorAtTheEnd = true,
                numberOfDecimals = 2
            ),
            trailingIcon = {
                if (trailingIcon != null) {
                    IconButton(onClick = trailingIconClick){
                        Icon(imageVector = trailingIcon, contentDescription = "")
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DMChipTextField(
    modifier: Modifier = Modifier,
    label: String = "Label here",
    chips: List<String>,
    onAddChip: (String) -> Unit,
    onRemoveChip: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }

    // Needed to detect click area outside the text field
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(8.dp)
            .clickable {
                // Request focus on click anywhere inside
                focusRequester.requestFocus()
            }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        FlowRow{
            chips.forEach { chip ->
                AssistChip(
                    onClick = { /* Optional click on chip */ },
                    label = { Text(chip) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.clickable { onRemoveChip(chip) }
                        )
                    }
                )
            }

            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    if (it.endsWith(" ") || it.endsWith(",")) {
                        val newChip = it.trim().removeSuffix(",")
                        if (newChip.isNotBlank()) {
                            onAddChip(newChip)
                            text = ""
                        }
                    }
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .widthIn(min = 80.dp, max = 200.dp)
                    .padding(4.dp)

            )
        }
    }
}
