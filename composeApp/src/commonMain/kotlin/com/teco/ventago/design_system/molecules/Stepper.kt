package com.teco.ventago.design_system.molecules

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Composable
fun StepperIndicator(
    totalSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.3f),
    dotSize: Dp = 8.dp,
    lineWidth: Dp = 32.dp,
    spacing: Dp = 8.dp
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        for (i in 0 until totalSteps) {
            val isActive = i == currentStep

            val animatedWidth by animateDpAsState(
                targetValue = if (isActive) lineWidth else dotSize,
                label = "widthAnim"
            )
            val animatedColor by animateColorAsState(
                targetValue = if (isActive) activeColor else inactiveColor,
                label = "colorAnim"
            )

            Box(
                modifier = Modifier
                    .height(dotSize)
                    .width(animatedWidth)
                    .clip(CircleShape)
                    .background(animatedColor)
            )

            if (i != totalSteps - 1) {
                Spacer(modifier = Modifier.width(spacing))
            }
        }
    }
}