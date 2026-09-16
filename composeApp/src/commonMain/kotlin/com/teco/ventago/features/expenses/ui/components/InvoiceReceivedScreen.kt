package com.teco.ventago.features.expenses.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.titleMediumBold
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlin.random.Random
import org.jetbrains.compose.resources.ExperimentalResourceApi
import ventago.composeapp.generated.resources.Res

@OptIn(ExperimentalResourceApi::class)
@Composable
fun InvoiceReceivedScreen(
    onUnderstood: () -> Unit,
    onScanAnother: () -> Unit,
    secondaryLabel: String = "Escanear otra factura",
    message: String = ExpenseInvoiceCopy.OCR_RECEIVED
) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/57767-done.json").decodeToString()
        )
    }
    val progress by animateLottieCompositionAsState(composition, iterations = 1)
    val background = MaterialTheme.colorScheme.background
    val confettiColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFFFFC107),
        Color(0xFF4CAF50),
        Color(0xFFE91E63)
    )
    val pieces = remember {
        List(42) { index ->
            ConfettiPiece(
                x = Random.nextFloat(),
                speed = 0.35f + Random.nextFloat() * 0.55f,
                width = 6f + Random.nextFloat() * 8f,
                height = 10f + Random.nextFloat() * 12f,
                color = confettiColors[index % confettiColors.size],
                drift = -0.15f + Random.nextFloat() * 0.3f
            )
        }
    }
    val infinite = rememberInfiniteTransition(label = "confetti")
    val tick by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiTick"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            pieces.forEachIndexed { index, piece ->
                val phase = (tick * piece.speed + index / pieces.size.toFloat()) % 1f
                val x = (piece.x + piece.drift * phase) * size.width
                val y = phase * (size.height + 40f) - 20f
                drawRect(
                    color = piece.color,
                    topLeft = Offset(x, y),
                    size = Size(piece.width, piece.height)
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = rememberLottiePainter(
                        composition = composition,
                        progress = { progress }
                    ),
                    modifier = Modifier.size(180.dp),
                    contentDescription = "Factura recibida"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Recibimos tu factura",
                    style = titleMediumBold(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    textAlign = TextAlign.Center
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ButtonM(onClick = onUnderstood, modifier = Modifier.fillMaxWidth()) {
                    Text("Entendido")
                }
                OutlinedButtonM(onClick = onScanAnother, modifier = Modifier.fillMaxWidth()) {
                    Text(secondaryLabel)
                }
            }
        }
    }
}

private data class ConfettiPiece(
    val x: Float,
    val speed: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    val drift: Float
)
