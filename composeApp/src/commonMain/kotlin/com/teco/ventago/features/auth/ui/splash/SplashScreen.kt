package com.teco.ventago.features.auth.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.app_logo
import ventago.composeapp.generated.resources.app_logo_content_description

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.app_logo),
            contentDescription = stringResource(Res.string.app_logo_content_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(128.dp)
                .clip(RoundedCornerShape(28.dp)),
        )
    }
}
