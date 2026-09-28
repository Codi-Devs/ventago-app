package com.teco.ventago.features.appentry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.teco.ventago.App
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.theme.DigitalMenuTheme
import com.teco.ventago.features.auth.ui.splash.SplashScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.update_later
import ventago.composeapp.generated.resources.update_now
import ventago.composeapp.generated.resources.update_recommended_body
import ventago.composeapp.generated.resources.update_recommended_title

@Composable
fun AppEntry(
    viewModel: AppEntryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onForeground()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DigitalMenuTheme {
        when (state.phase) {
            AppEntryPhase.Checking -> SplashScreen(Modifier.fillMaxSize())
            AppEntryPhase.Offline -> OfflineGateScreen(
                versionName = state.versionName,
                retrying = state.retrying,
                onRetry = viewModel::retry,
            )
            AppEntryPhase.Forced -> ForcedUpdateScreen(
                versionName = state.versionName,
                retrying = state.retrying,
                onUpdate = { viewModel.updateNow(forced = true) },
                onRetry = viewModel::retry,
            )
            AppEntryPhase.Ready -> {
                Box(Modifier.fillMaxSize()) {
                    App()
                    DMAlertDialog(
                        title = stringResource(Res.string.update_recommended_title),
                        message = stringResource(Res.string.update_recommended_body),
                        show = state.showRecommendedPrompt,
                        onDismiss = viewModel::dismissRecommended,
                        onConfirm = {
                            viewModel.updateNow(forced = false)
                            viewModel.dismissRecommended()
                        },
                        confirmText = stringResource(Res.string.update_now),
                        dismissText = stringResource(Res.string.update_later),
                    )
                }
            }
        }
    }
}
