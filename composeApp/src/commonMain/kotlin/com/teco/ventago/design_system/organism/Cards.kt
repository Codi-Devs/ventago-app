package com.teco.ventago.design_system.organism

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.teco.ventago.design_system.buttons.DottedButton
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.latoFontFamily

@Composable
fun HomeTopCard(
    modifier : Modifier = Modifier,
    title: String,
    image: String? = null,
    onAddImageClick: () -> Unit
) {

    Card (
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(0.dp, 0.dp, 20.dp, 20.dp),
        elevation = CardDefaults.cardElevation(
            4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor())
    ) {

        Row(
            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (image.isNullOrBlank()) {
                DottedButton(
                    modifier = Modifier.size(70.dp, 70.dp),
                    onClick = onAddImageClick
                )
            } else {
                HomeLogo(url = image)
            }

            Text(
                modifier = Modifier.padding(16.dp, 8.dp, 8.dp, 8.dp),
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    fontFamily = latoFontFamily(),
                    fontWeight = FontWeight.W400,
                    textAlign = TextAlign.Start,
                )
            )
        }
    }
}

@Composable
fun HomeLogo(url: String) {
    val shape = RoundedCornerShape(8.dp)

    val height = 70.dp
    val width = 70.dp

    Box (
        modifier = Modifier
            .height(70.dp)
            .width(70.dp)
            .background(color = MaterialTheme.colorScheme.background, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = url,
            contentDescription = "Image Styles preview",
            placeholder = ColorPainter(Color.LightGray),
            contentScale = ContentScale.FillWidth,
            error = ColorPainter(Color.LightGray),
            modifier = Modifier
                .height(height)
                .width(width)
                .clip(shape),
        )
    }
}