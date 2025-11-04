package com.teco.ventago.features.payments.ui.yappy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.financialProfile.domain.model.PaymentSummary
import com.teco.ventago.features.payments.domain.PaymentService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class YappyViewModel(
    private val paymentService: PaymentService,
    private val businessService: BusinessService,
    private val financialProfileService: FinancialProfileService,
): ViewModel()  {

    private val _uiState = MutableStateFlow(YappyUiState())
    val uiState: StateFlow<YappyUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<YappyUIEvents>()
    val events = _events.asSharedFlow()

    private lateinit var summary: PaymentSummary
    private var business: Business? = null

    private fun updateState(reducer: YappyUiState.() -> YappyUiState) {
        _uiState.update { it.reducer() }
    }

    init {
        viewModelScope.launch {
            financialProfileService.observe().onEach { profile ->
                profile?.let {
                    summary = profile.paymentSummary
                    withContext(Dispatchers.Main) {
                        updateState {
                            copy(
                                transactionFeePercent = 1.00,
                                linkedYappyAccount = summary.paymentMethods.yappy.linkedAccount,
                                loadingSummaryData = false,
                                showIntroDialog = !summary.paymentMethods.yappy.linkedAccount,
                            )
                        }
                    }
                }
            }.launchIn(this)

            businessService.business.onEach { business ->
                business?.let{
                    this@YappyViewModel.business = it
                }
            }.launchIn(this)
        }
    }

    fun connectYappy() {
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                business?.let {
                    val connected = paymentService.connectYappy(it.businessId,
                        uiState.value.merchantID, uiState.value.domain, uiState.value.secretKey)
                    updateState {
                        copy(
                            loadingBottomSheet = LoadingBottomSheetState(LoadingState.SUCCESS, ""),
                            linkedYappyAccount = connected,
                            merchantID = "",
                            domain = "",
                            secretKey = ""
                        )
                    }
                    showSuccess()
                    _events.emit(YappyUIEvents.LinkedYappyAccount)
                } ?: run {
                    showError()
                }
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun unlinkYappy() {
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                business?.let {
                    val unlinked = paymentService.unlinkYappy(it.businessId)
                    if (unlinked) {
                        _events.emit(YappyUIEvents.UnlinkedYappyAccount)
                        showSuccess()
                    } else {
                        showError()
                    }
                } ?: run {
                    showError()
                }
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun canConfigureYappy(): Boolean {
        return uiState.value.merchantID.length > 3 &&
                uiState.value.domain.length > 3 &&
                uiState.value.secretKey.length > 3
    }

    fun onMerchantIDChange(value: String) {
        updateState {
            copy(merchantID = value)
        }
    }

    fun onDomainChange(value: String) {
        updateState {
            copy(domain = value)
        }
    }

    fun onSecretKeyChange(value: String) {
        updateState {
            copy(secretKey = value)
        }
    }

    fun showIntroDialog(show: Boolean) {
        updateState {
            copy(showIntroDialog = show)
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