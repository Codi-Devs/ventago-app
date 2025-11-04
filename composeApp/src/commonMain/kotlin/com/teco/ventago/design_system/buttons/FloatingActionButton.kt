package com.teco.ventago.design_system.buttons

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.theme.Gray10

@Composable
fun floatingActionButton(onClick: () -> Unit, icon: ImageVector, contentDescription: String = "") {

    return FloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(
            tint = Gray10,
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}