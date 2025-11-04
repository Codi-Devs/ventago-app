package com.teco.ventago.features.branches.ui.billing_point.edit.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.business.domain.BusinessService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EditBillingPointViewModel(
    private val branchService: BranchService,
    private val businessService: BusinessService,
    private val loggerService: ILoggerService,
    private val branchCode: String,
    private val billingPoint: String,
) : BaseViewModel<EditBillingPointState, EditBillingPointStateUiEvent>(EditBillingPointState()) {

    init {
        val branches = branchService.observe().value
        val branch = branches.firstOrNull { branch -> branch.branchCode == branchCode }
        if (branch != null) {
            val fiscalBillingPoint = branch.fiscalBillingPoints.firstOrNull { point -> point.billingPoint == billingPoint }
            updateState {
                copy(
                    billingPoint = fiscalBillingPoint,
                    name = fiscalBillingPoint?.description ?: "",
                )
            }
        } else {
            loggerService.sendLog(Log(
                LogLevel.ERROR,
                "EditBillingPointViewModel::init",
                "Branch is null on init. branchCode: $branchCode, businessID: ${businessService.business.value?.businessId}"
            ))
            viewModelScope.launch {
                emitEvent(EditBillingPointStateUiEvent.GoBack)
            }

        }
        // initialize with branchId
    }

    private fun enableButton() {
        val s = uiState.value
        val orig = s.billingPoint

        // 2) Name changed and valid
        val nameChangedValid = s.name.isNotBlank() && s.name != orig?.description

        updateState { copy(buttonEnabled = nameChangedValid) }
    }

    fun onNameChange(name: String) {
        updateState {
            copy(name = name)
        }
        enableButton()
    }

    fun editBillingPoint() {
        val state = uiState.value
        if (state.name.isBlank()) {
            return
        }

        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updated = branchService.updateBillingPoint(
                    businessService.business.value!!.businessId,
                    branchCode,
                    billingPoint,
                    state.name,
                    state.billingPoint?.status ?: 1,
                )
                if (!updated) {
                    loggerService.sendLog(
                        Log(
                            LogLevel.ERROR,
                            "EditBillingPointViewModel::editBillingPoint",
                            "updated returned from businessService is false"
                        )
                    )
                    showError()
                    return@launch
                }
                showSuccess()
                delay(600)
                emitEvent(EditBillingPointStateUiEvent.GoBack)
            } catch (e: Exception) {
                loggerService.sendLog(
                    Log(
                        LogLevel.ERROR,
                        "EditBillingPointViewModel::editBillingPoint",
                        "Error editing billing point. Error: ${e.message ?: "UNKNOWN"}"
                    )
                )
                showError()
            }

        }
    }

}