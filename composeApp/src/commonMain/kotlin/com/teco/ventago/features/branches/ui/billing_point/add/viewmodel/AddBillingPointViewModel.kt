package com.teco.ventago.features.branches.ui.billing_point.add.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.location.PanamaLocations
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.business.domain.BusinessService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AddBillingPointViewModel(
    private val branchService: BranchService,
    private val businessService: BusinessService,
    private val loggerService: ILoggerService,
    private val branchCode: String
) : BaseViewModel<AddBillingPointState, AddBillingPointStateUiEvent>(AddBillingPointState()) {


    private fun enableButton() {
        val state = uiState.value
        if (state.name.isBlank() || state.name.length < 3) {
            updateState { copy(buttonEnabled = false) }
            return
        }

        updateState { copy(buttonEnabled = true) }
    }
    fun onNameChange(name: String) {
        updateState {
            copy(name = name)
        }
        enableButton()
    }

    fun addBillingPoint() {
        val state = uiState.value
        if (state.name.isBlank()) {
            return
        }

        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val billingPoint = branchService.addBillingPoint(
                    businessService.business.value!!.businessId,
                    branchCode,
                    state.name,
                )
                if (billingPoint == null) {
                    loggerService.sendLog(Log(
                        LogLevel.ERROR,
                        "AddBillingPointViewModel::addBillingPoint",
                        "Billing point returned from businessService is null"
                    ))
                    showError()
                    return@launch
                }
                showSuccess()
                delay(600)
                emitEvent(AddBillingPointStateUiEvent.GoBack)
            } catch (e: Exception) {
                loggerService.sendLog(
                    Log(
                        LogLevel.ERROR,
                        "AddBillingPointViewModel::addBillingPoint",
                        "Error adding billing point. Error: ${e.message ?: "UNKNOWN" }"
                    )
                )
                showError()
            }

        }


    }

}