package com.teco.ventago.features.appentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.version.AppBuildInfo
import com.teco.ventago.core.version.AppVersion
import com.teco.ventago.core.version.IAppUpdateLauncher
import com.teco.ventago.core.version.VersionGateDecision
import com.teco.ventago.core.version.VersionGateHold
import com.teco.ventago.core.version.VersionGateService
import com.teco.ventago.core.version.VersionPolicy
import com.teco.ventago.core.version.parseMarketingVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppEntryPhase {
    Checking,
    Offline,
    Forced,
    Ready,
}

data class AppEntryUiState(
    val phase: AppEntryPhase = AppEntryPhase.Checking,
    val versionName: String = "",
    val installedVersion: AppVersion? = null,
    val policy: VersionPolicy = VersionPolicy.DISABLED,
    val showRecommendedPrompt: Boolean = false,
    val showSettingsUpdate: Boolean = false,
    val retrying: Boolean = false,
)

class AppEntryViewModel(
    private val versionGateService: VersionGateService,
    private val updateLauncher: IAppUpdateLauncher,
    private val versionGateHold: VersionGateHold,
    buildInfo: AppBuildInfo,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AppEntryUiState(
            versionName = buildInfo.versionName,
            installedVersion = parseMarketingVersion(buildInfo.versionName),
        )
    )
    val uiState: StateFlow<AppEntryUiState> = _uiState.asStateFlow()
    private var pendingForced = false
    private var hasCompletedInitialCheck = false

    init {
        viewModelScope.launch { refresh(isRetry = false) }
    }

    fun onForeground() {
        if (!hasCompletedInitialCheck) return
        viewModelScope.launch { refresh(isRetry = false) }
    }

    fun retry() {
        viewModelScope.launch { refresh(isRetry = true) }
    }

    fun updateNow(forced: Boolean) {
        viewModelScope.launch {
            if (forced) {
                updateLauncher.startForcedUpdate()
            } else {
                updateLauncher.startRecommendedUpdate()
            }
        }
    }

    fun dismissRecommended() {
        versionGateService.dismissRecommendedPrompt()
        _uiState.update { it.copy(showRecommendedPrompt = false) }
    }

    private suspend fun refresh(isRetry: Boolean) {
        if (isRetry) {
            _uiState.update { it.copy(retrying = true) }
        }
        val decision = runCatching { versionGateService.evaluate() }
            .getOrDefault(VersionGateDecision.Allowed)
        val snapshot = versionGateService.snapshot.value
        val hold = versionGateHold.hasHold()
        val currentlyReady = _uiState.value.phase == AppEntryPhase.Ready

        val nextPhase = when (decision) {
            VersionGateDecision.Offline -> {
                if (currentlyReady && hold) AppEntryPhase.Ready else AppEntryPhase.Offline
            }
            VersionGateDecision.Forced -> {
                if (currentlyReady && hold) {
                    pendingForced = true
                    AppEntryPhase.Ready
                } else {
                    pendingForced = false
                    AppEntryPhase.Forced
                }
            }
            VersionGateDecision.Recommended, VersionGateDecision.Allowed -> {
                if (pendingForced && !hold) {
                    AppEntryPhase.Forced
                } else {
                    if (!hold) pendingForced = false
                    AppEntryPhase.Ready
                }
            }
        }

        _uiState.update {
            it.copy(
                phase = nextPhase,
                versionName = snapshot.versionName,
                installedVersion = snapshot.installedVersion,
                policy = snapshot.policy,
                showRecommendedPrompt = nextPhase == AppEntryPhase.Ready &&
                    versionGateService.shouldShowRecommendedPrompt(),
                showSettingsUpdate = snapshot.showSettingsUpdate,
                retrying = false,
            )
        }
        hasCompletedInitialCheck = true
    }
}
