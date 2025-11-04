package com.teco.ventago.features.settings.ui.name.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.settings.domain.SettingsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangeNameViewModel(
    private val settingsService: SettingsService,
    private val businessService: BusinessService,
    private val logger: ILoggerService
) : ViewModel() {

    val state = ChangeNameState()
    var business: Business? = businessService.business.value

    fun changeBusinessName(name: String) {
        if (name.isEmpty() || name.isBlank() || name.length < 3) {
            state.isError.value = true
            return
        }

        state.showLoading("")
        val businessId = business?.businessId ?: -1
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val res = settingsService.changeBusinessName(businessId, name)
                    if (!res) {
                        withContext(Dispatchers.Main) {
                            state.showError()
                        }
                    } else {
                        state.name.value = name
                        withContext(Dispatchers.Main) {
                            state.showSuccess()
                        }
                    }
                } catch (e: Exception) {
                    logger.sendLog(
                        Log(
                            LogLevel.ERROR,
                            "ChangeNameViewModel",
                            "changeBusinessName: ${e.message}"
                        )
                    )
                    withContext(Dispatchers.Main) {
                        state.showError()
                    }
                }
            }
        }
    }

    fun loadingDone() {
        state.loadingState.value = state.loadingState.value.copy(state = LoadingState.HIDDEN)
    }
}