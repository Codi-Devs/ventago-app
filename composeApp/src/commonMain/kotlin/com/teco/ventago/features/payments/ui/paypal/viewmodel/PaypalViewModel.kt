package com.teco.ventago.features.payments.ui.paypal.viewmodel

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

class PaypalViewModel(
    private val paymentService: PaymentService,
    private val financialProfileService: FinancialProfileService,
    private val businessService: BusinessService
): ViewModel() {

    private val _uiState = MutableStateFlow(PaypalUiState())
    val uiState: StateFlow<PaypalUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PaypalUIEvents>()
    val events = _events.asSharedFlow()

    private lateinit var summary: PaymentSummary
    private var business: Business? = null

    private fun updateState(reducer: PaypalUiState.() -> PaypalUiState) {
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
                                transactionFeePercent = 2.00,
                                linkedPaypalAccount = summary.paymentMethods.paypal.linkedAccount,
                                linkedBillingAgreement = summary.linkedPaypalBillingAgreement,
                                linkedEmail = summary.paymentMethods.paypal.email,
                                loadingSummaryData = false,
                            )
                        }
                    }
                }
            }.launchIn(this)

            businessService.business.onEach { business ->
                business?.let{
                    this@PaypalViewModel.business = it
                }
            }.launchIn(this)
        }
    }

    fun connectPaypal() {
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                business?.let {
                    val url = paymentService.connectPaypal(it.businessId)
                    _events.emit(PaypalUIEvents.OpenConnectUrl(url))
                    showSuccess()
                } ?: run {
                    showError()
                }
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun unlinkPaypal() {
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                business?.let {
                    val unlinked = paymentService.unlinkPaypal(it.businessId)
                    if (unlinked) {
                        _events.emit(PaypalUIEvents.UnlinkedPaypalAccount)
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

    fun createBillingAgreement() {
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            business?.let {
                try {
                    val url = paymentService.createBillingAgreement(it.businessId)
                    showSuccess()
                    _events.emit(PaypalUIEvents.OpenBillingAgreementUrl(url))
                } catch (e: Exception) {
                    showError()
                }
            } ?: run {
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