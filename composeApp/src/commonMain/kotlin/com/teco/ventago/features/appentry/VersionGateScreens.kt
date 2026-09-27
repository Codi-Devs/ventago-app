package com.teco.ventago.features.appentry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.titleMediumBold
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.update_app_installed_version
import ventago.composeapp.generated.resources.update_forced_body
import ventago.composeapp.generated.resources.update_forced_title
import ventago.composeapp.generated.resources.update_now
import ventago.composeapp.generated.resources.update_offline_body
import ventago.composeapp.generated.resources.update_offline_title
import ventago.composeapp.generated.resources.update_retry

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VersionGateScreen(
    title: String,
    body: String,
    versionName: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    primaryEnabled: Boolean = true,
    icon: @Composable () -> Unit,
) {
    val blockerInteraction = remember { MutableInteractionSource() }
    BackHandler(enabled = true) {}

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = blockerInteraction,
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                icon()
                Text(
                    text = title,
                    style = titleMediumBold(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = body,
                    style = bodyMedium(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (versionName.isNotBlank()) {
                    Text(
                        text = stringResource(Res.string.update_app_installed_version, versionName),
                        style = bodySmall(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                ButtonM(
                    onClick = onPrimary,
                    enabled = primaryEnabled,
                ) {
                    Text(primaryLabel)
                }
                if (secondaryLabel != null && onSecondary != null) {
                    OutlinedButtonM(onClick = onSecondary) {
                        Text(secondaryLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineGateScreen(
    versionName: String,
    retrying: Boolean,
    onRetry: () -> Unit,
) {
    VersionGateScreen(
        title = stringResource(Res.string.update_offline_title),
        body = stringResource(Res.string.update_offline_body),
        versionName = versionName,
        primaryLabel = stringResource(Res.string.update_retry),
        onPrimary = onRetry,
        primaryEnabled = !retrying,
        icon = {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        },
    )
}

@Composable
fun ForcedUpdateScreen(
    versionName: String,
    retrying: Boolean,
    onUpdate: () -> Unit,
    onRetry: () -> Unit,
) {
    VersionGateScreen(
        title = stringResource(Res.string.update_forced_title),
        body = stringResource(Res.string.update_forced_body),
        versionName = versionName,
        primaryLabel = stringResource(Res.string.update_now),
        onPrimary = onUpdate,
        secondaryLabel = stringResource(Res.string.update_retry),
        onSecondary = onRetry,
        primaryEnabled = !retrying,
        icon = {
            Icon(
                imageVector = Icons.Outlined.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        },
    )
}
