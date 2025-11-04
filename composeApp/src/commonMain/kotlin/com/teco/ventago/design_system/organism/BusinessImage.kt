package com.teco.ventago.design_system.organism

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.teco.ventago.design_system.buttons.DottedButton

@Composable
fun BusinessImage(imgUrl: String?, imageBitmap: ImageBitmap?, onClick: () -> Unit) {
    if (imageBitmap != null || (imgUrl != null && imgUrl != "")) {
        Box(modifier = Modifier) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Product Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(0.dp),
                )
            } else {
                AsyncImage(
                    model = imgUrl,
                    contentDescription = "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(0.dp),
                    contentScale = ContentScale.Fit,
                    placeholder = ColorPainter(Color.LightGray),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp)
                    .height(150.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = {
                    onClick()
                }) {
                    Icon(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(50)
                            )
                            .padding(8.dp)
                            .height(24.dp)
                            .width(24.dp),
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit Image",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    } else {
        DottedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp)
                .height(150.dp),
            onClick = {
                onClick()
            }
        )
    }
}