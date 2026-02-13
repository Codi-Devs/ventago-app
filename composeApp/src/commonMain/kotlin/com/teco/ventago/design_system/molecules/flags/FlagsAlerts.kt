package com.teco.ventago.design_system.molecules.flags

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MaintenanceModeOverlay(
    modifier: Modifier = Modifier,
) {
    val blockerInteraction = remember { MutableInteractionSource() }
    BackHandler(enabled = true) {}

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = blockerInteraction,
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "El sistema esta en mantenimiento",
                        style = titleMediumBold()
                    )
                }

                Text(
                    text = "Estamos realizando mejoras importantes. Volveremos pronto.",
                    style = bodyMedium()
                )

                HorizontalDivider()

                MaintenanceInfoRow(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = "Por que no esta disponible",
                    description = "Estamos actualizando el sistema para ofrecer una mejor experiencia."
                )
                MaintenanceInfoRow(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = "Cuanto tiempo tomara",
                    description = "El mantenimiento suele durar de 5 a 15 minutos."
                )
                MaintenanceInfoRow(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.SupportAgent,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = "Necesitas ayuda",
                    description = "Contactanos por WhatsApp: +507 6387-9477."
                )
            }
        }
    }
}

@Composable
fun DgiDownAlertBanner(
    modifier: Modifier = Modifier,
) {
    val container = Color(0xFFFFF8E1)
    val border = Color(0xFFFFE0B2)
    val titleColor = Color(0xFFFFA000)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = titleColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Intermitencia en el sistema de DGI",
                    style = bodyMediumBold(color = titleColor)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "La DGI esta experimentando problemas tecnicos. Las facturas pueden fallar al emitirse.",
                style = bodyMedium()
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = border
            )
            Text(
                text = "Te recomendamos esperar antes de facturar. Si continuas, podrias necesitar reintentar luego.",
                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun MaintenanceInfoRow(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
) {
    Row(verticalAlignment = Alignment.Top) {
        icon()
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = bodyMediumBold()
            )
            Text(
                text = description,
                style = bodySmall()
            )
        }
    }
}
