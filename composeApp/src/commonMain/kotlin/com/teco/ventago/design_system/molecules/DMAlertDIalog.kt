package com.teco.ventago.design_system.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.latoFontFamily

@Composable
fun DMAlertDialog(
    title: String,
    message: String,
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String,
    dismissText: String,
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = title) },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text(text = confirmText) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(text = dismissText) }
            },
        )
    }
}

@Composable
fun DMSimpleAlertDialog(
    title: String,
    message: String,
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    btnText: String,
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = title) },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text(text = btnText) }
            },
        )
    }
}

@Composable
fun DMShippingMethodDialog(
    message: String,
    label: String,
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    confirmText: String,
    dismissText: String,
    initialValue: String,
) {

    var value by remember { mutableStateOf(initialValue) }

    if (show) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(16.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = message,
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                fontFamily = latoFontFamily(),
                                fontWeight = FontWeight(400),
                                color = Color(0xFF2D3139),
                                textAlign = TextAlign.Center,
                                letterSpacing = 0.02.sp,
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        DMOutlinedTextField(
                            text = value,
                            label = label,
                            modifier = Modifier,
                            onChange = {
                                       value = it
                            },
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Number,
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        //Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButtonS(label = dismissText, onClick = onDismiss)
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButtonS(label = confirmText, onClick = {
                                onConfirm(value)
                            })
                        }

                    }
                }

            }
        }
    }
}