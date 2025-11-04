package com.teco.ventago.features.settings.ui.address.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.business.domain.model.BusinessAddress
import com.teco.ventago.features.settings.domain.SettingsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SetAddressViewModel(
    private val settingsService: SettingsService,
    private val businessService: BusinessService,
    private val logger: ILoggerService
) : ViewModel() {


    private val _uiState = MutableStateFlow(SetAddressState())
    val uiState: StateFlow<SetAddressState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SetAddressUiEvent>()
    val events = _events.asSharedFlow()

    var business: Business? = businessService.business.value

    private fun updateState(reducer: SetAddressState.() -> SetAddressState) {
        _uiState.update { it.reducer() }
    }

    init {
        businessService.business.value?.let { business ->
            updateState {
                copy(
                    actualAddress = business.address.placeAddress
                )
            }
        }
    }

    fun setAddress(address: BusinessAddress) {
        updateState {
            copy(actualAddress = address.placeAddress, newAddress = address, updateButtonEnabled = true)
        }
    }

    fun updateBusinessAddress() {
        if (!uiState.value.isAddressFilled || !uiState.value.updateButtonEnabled || uiState.value.newAddress == null) {
            return
        }

        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                businessService.business.value?.let { business ->
                    val changed = settingsService.changeBusinessAddress(
                        business.businessId,
                        uiState.value.newAddress!!
                    )
                    if (changed) {
                        updateState {
                            copy(updateButtonEnabled = false, newAddress = null)
                        }
                        showSuccess()
                    } else {
                        showError()
                    }
                } ?: showError()
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun showLoading() {
        updateState {
            copy(loadingBottomSheet = LoadingBottomSheetState(LoadingState.LOADING, ""))
        }
    }

    fun showSuccess() {
        updateState {
            copy(loadingBottomSheet = LoadingBottomSheetState(LoadingState.SUCCESS, ""))
        }
    }

    fun showError() {
        updateState {
            copy(loadingBottomSheet = LoadingBottomSheetState(LoadingState.ERROR, ""))
        }
    }

    fun hideLoading() {
        updateState {
            copy(loadingBottomSheet = LoadingBottomSheetState(LoadingState.HIDDEN))
        }
    }

}