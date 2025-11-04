package com.teco.ventago.features.branches.ui.billing_point.manage.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint
import com.teco.ventago.features.business.domain.BusinessService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class BillingPointsManageViewModel(
    private val branchService: BranchService,
    private val businessService: BusinessService,
    private val logger: ILoggerService,
    private val branchCode: String
) : BaseViewModel<BillingPointsManageState, BillingPointsManageStateUiEvent>(
    BillingPointsManageState()
) {

    init {
        // Observe business changes
        branchService.observe()
            .onStart {
                val branches = branchService.observe().value
                val branch = branches.find { it.branchCode == branchCode }
                updateState {
                    copy(
                        selectedBranchCode = branch?.branchCode,
                        billingPoints = branch?.fiscalBillingPoints?.filter { bp -> bp.status == 1 } ?: emptyList()
                    )
                }
            }
            .onEach { branches ->
                val branch = branches.find { it.branchCode == branchCode }
                updateState {
                    copy(
                        selectedBranchCode = branch?.branchCode,
                        billingPoints = branch?.fiscalBillingPoints ?: emptyList()
                    )
                }
            }
            .catch { e ->

            }
            .launchIn(viewModelScope)
    }


    fun deleteBillingPoint(billingPoint: String) {
        if (billingPoint == "001" || billingPoint.isBlank() || billingPoint.length != 3) {
            return
        }
        showLoading()
        val point = findBillingPointByCode(billingPoint) ?: run {
            showError()
            return
        }
        businessService.business.value?.let { business ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val deleted = branchService.updateBillingPoint(
                        business.businessId,
                        uiState.value.selectedBranchCode ?: "",
                        billingPoint,
                        point.description ?: "",
                        0
                    )
                    if (deleted) {
                        showSuccess()
                    } else {
                        showError()
                    }
                } catch (e: Exception) {
                    logger.sendLog(
                        Log(
                            LogLevel.ERROR,
                            "BillingPointViewModel::disablingBillingPoint",
                            "Error deleting disablingBillingPoint: ${e.message ?: "UNKNOWN"}"
                        )
                    )
                    showError()
                }
            }
        }
    }

    fun findBillingPointByCode(billingPointCode : String): FiscalBillingPoint? {
        val billingPoint = uiState.value.billingPoints.find { it.billingPoint == billingPointCode }
        return billingPoint
    }


}

