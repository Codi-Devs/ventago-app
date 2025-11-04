package com.teco.ventago.design_system.organism

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.design_system.theme.latoFontFamily

@Composable
fun ShortcutItem(modifier: Modifier, title: String, imageVector: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = modifier.size(94.dp, 75.dp),
        elevation = CardDefaults.cardElevation(
            4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        onClick = onClick
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = imageVector, contentDescription = "", tint = MaterialTheme.colorScheme.primary)
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontFamily = latoFontFamily(),
                    fontWeight = FontWeight(700),
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.03.sp,
                ),
            )
        }
    }
}

@Composable
fun ShortcutOption(modifier: Modifier, title: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: () -> Unit) {
    Column (
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontFamily = latoFontFamily(),
                    fontWeight = FontWeight(400),
                    letterSpacing = 0.25.sp,
                )
            )

            Spacer(modifier = Modifier)

            Switch(checked = checked, onCheckedChange =  {onCheckedChange()}, enabled = enabled)
        }
        Divider(
            color = Color.LightGray,
        )
    }
}