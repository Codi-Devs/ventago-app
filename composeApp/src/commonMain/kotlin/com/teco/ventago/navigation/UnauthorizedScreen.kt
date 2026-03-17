package com.teco.ventago.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.titleMediumBold
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.authz_access_denied_message
import ventago.composeapp.generated.resources.authz_access_denied_title
import ventago.composeapp.generated.resources.retry
import ventago.composeapp.generated.resources.sign_out

@Composable
fun UnauthorizedScreen(
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.authz_access_denied_title),
            style = titleMediumBold(),
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(Res.string.authz_access_denied_message),
            modifier = Modifier.padding(top = 12.dp),
            style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = TextAlign.Center
        )
        ButtonM(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            onClick = onRetry
        ) {
            Text(stringResource(Res.string.retry))
        }
        OutlinedButtonM(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            onClick = onSignOut
        ) {
            Text(stringResource(Res.string.sign_out))
        }
    }
}
