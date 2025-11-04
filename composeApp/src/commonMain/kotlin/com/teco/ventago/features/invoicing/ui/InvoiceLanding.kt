package com.teco.ventago.features.invoicing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.headlineSmall
import com.teco.ventago.design_system.theme.labelLarge
import com.teco.ventago.utils.openCustomTab
import com.teco.ventago.utils.openWhatsappMessage


@Composable
fun InvoicingLandingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        val surface = MaterialTheme.colorScheme.surface
        val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(surface)
        ) {
            // Header image only on the upper part (about 42–48% height works well for 9:16)
            AsyncImage(
                model = "https://ventago.b-cdn.net/app/20250927_2248_Secure%20Digital%20Invoice_remix_01k676wn42ev4rcjzwe0k8wpxt.png",
                contentDescription = "Electronic invoicing header",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.56f) // show only upper portion
                    .align(Alignment.TopCenter)
                    // Bottom gradient that fades into the screen background
                    .drawWithCache {
                        val gradient = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, surface.copy(alpha = 0.98f)),
                            startY = size.height * 0.35f,
                            endY = size.height
                        )
                        onDrawWithContent {
                            drawContent()
                            drawRect(gradient)
                        }
                    }
            )

            // Bottom content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Comienza hoy tu Facturación Electrónica",
                    style = headlineSmall().copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 32.sp,
                        textAlign = TextAlign.Start
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Checklist items (short like the sample)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CheckItem("Desde $19.99 anual")
                    CheckItem("Firma electrónica")
                    CheckItem("Soporte especializado")
                    CheckItem("Integrado en Ventago")
                    CheckItem("Cumple con la DGI")
                }

                // CTA row (price note + WhatsApp button style)
                Spacer(modifier = Modifier.height(6.dp))
                ButtonM(
                    onClick = {
                        openWhatsappMessage("50763879477", "Hola, quiero más información sobre la Facturación Electrónica.")
                    }
                ) {
                    Text(
                        text = "Contáctanos por WhatsApp",
                        style = labelLarge().copy(color = MaterialTheme.colorScheme.onPrimary)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center) {
                    TextButtonS(
                        label = "Más información sobre Facturación Electrónica",
                    ) {
                        openCustomTab("https://ventago.app/facturacion-electronica/")

                    }
                }

            }
        }

    }
}

@Composable
private fun CheckItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = Color(0xFF006C44), // brand accent
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = bodyMedium(),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}