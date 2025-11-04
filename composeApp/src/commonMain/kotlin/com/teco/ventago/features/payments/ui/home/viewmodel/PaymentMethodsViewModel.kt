package com.teco.ventago.features.payments.ui.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.cache.room.models.json
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.financialProfile.domain.model.PaymentSummary
import com.teco.ventago.features.payments.domain.PaymentService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

class PaymentMethodsViewModel(
    private val paymentService: PaymentService,
    private val businessService: BusinessService,
    private val financialProfileService: FinancialProfileService
): BaseViewModel<PaymentUiState, PaymentUiEvent>(PaymentUiState()) {

    private lateinit var summary: PaymentSummary
    private var business: Business? = null

    private var resetSummary = false

    init {
        viewModelScope.launch {
            businessService.business.onEach { business ->
                business?.let{
                    this@PaymentMethodsViewModel.business = it
                    updateState {
                        copy(
                            pendingFeesCurrency = it.currency.currencyCode,
                            pendingFeesCurrencySymbol = it.currency.symbol,
                        )
                    }
                }
            }.launchIn(this)

            financialProfileService.observe().onEach { profile ->
                profile?.let {
                    summary = profile.paymentSummary
                    println("ASDASD: sumary ${json.encodeToString<PaymentSummary>(summary)}")
                    val availablePaymentMethods = mutableMapOf<String, PaymentMethodItem>()
                    if (summary.paymentMethods.paypal.visible) {
                        availablePaymentMethods["paypal"] = PaymentMethodItem(
                            id = "paypal",
                            visible = summary.paymentMethods.paypal.visible,
                            enabled = summary.paymentMethods.paypal.linkedAccount,
                            label = summary.paymentMethods.paypal.email.ifBlank { null }
                        )
                    }
                    if (summary.paymentMethods.yappy.visible) {
                        availablePaymentMethods["yappy"] = PaymentMethodItem(
                            id = "yappy",
                            visible = summary.paymentMethods.yappy.visible,
                            enabled = summary.paymentMethods.yappy.linkedAccount,
                            label = null
                        )
                    }
                    if (summary.paymentMethods.manualTransference.visible) {
                        availablePaymentMethods["transference"] = PaymentMethodItem(
                            id = "transference",
                            visible = summary.paymentMethods.manualTransference.visible,
                            enabled = summary.paymentMethods.manualTransference.enabled,
                            label = null
                        )
                    }

                    updateState {
                        copy(
                            availablePaymentMethods = availablePaymentMethods,
                            showOnboarding = !summary.onboardingCompleted,
                            linkedBillingAgreement = summary.linkedPaypalBillingAgreement,
                            showBottomBar = summary.paymentMethods.paypal.linkedAccount ||
                                    summary.paymentMethods.yappy.linkedAccount,
                            pendingFees = summary.pendingCharges,
                            nextBillingDate = summary.nextBillingDate,
                            loadingSummaryData = false,
                            transferenceInstructions = summary.paymentMethods.manualTransference.paymentInstructions
                        )
                    }
                } ?: run {
                    _events.emit(PaymentUiEvent.ErrorLoadingSummary)
                }

            }.launchIn(this)
        }
    }

    fun isAddressEmpty(): Boolean {
        return business?.let { business ->
             business.address.placeAddress.isBlank() || business.address.placeAddress == "null"
        } ?: run {
             true // If business is null, consider address empty
        }
    }

    fun openPaypal() {
        viewModelScope.launch {
            if (summary.paymentMethods.paypal.linkedAccount) {
                emitEvent(PaymentUiEvent.OpenPaypalScreen)
            } else {
                emitEvent(PaymentUiEvent.OpenPaypalOnboarding)
            }
        }

    }

    fun onboardPayments() {
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                business?.let {
                    val onboarded = paymentService.onboardPayments(it.businessId)
                    if (onboarded) {
                        showSuccess()
                        withContext(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(
                                showOnboarding = false
                            )
                        }
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

    fun updateTransferencePaymentMethod() {
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                business?.let {
                    val success = paymentService.transference(
                        it.businessId,
                        uiState.value.availablePaymentMethods["transference"]?.enabled ?: false,
                        uiState.value.transferenceInstructions
                    )
                    if (success) {
                        showSuccess()
                        // Update summary with the new transference instructions
                        summary = summary.copy(
                            paymentMethods = summary.paymentMethods.copy(
                                manualTransference = summary.paymentMethods.manualTransference.copy(
                                    paymentInstructions = uiState.value.transferenceInstructions,
                                    enabled = uiState.value.availablePaymentMethods["transference"]?.enabled ?: false
                                )
                            )
                        )
                        val availablePaymentMethods = _uiState.value.availablePaymentMethods.toMutableMap()
                        availablePaymentMethods["transference"] = PaymentMethodItem(
                            id = "transference",
                            visible = summary.paymentMethods.manualTransference.visible,
                            enabled = summary.paymentMethods.manualTransference.enabled,
                            label = null
                        )
                        updateState {
                            _uiState.value.copy(
                                availablePaymentMethods = availablePaymentMethods
                            )
                        }

                    } else {
                        val availablePaymentMethods = _uiState.value.availablePaymentMethods.toMutableMap()
                        availablePaymentMethods["transference"] = PaymentMethodItem(
                            id = "transference",
                            visible = summary.paymentMethods.manualTransference.visible,
                            enabled = summary.paymentMethods.manualTransference.enabled,
                            label = null
                        )
                        updateState {
                            _uiState.value.copy(
                                availablePaymentMethods = availablePaymentMethods
                            )
                        }
                        showError()
                    }
                } ?: run {
                    val availablePaymentMethods = _uiState.value.availablePaymentMethods.toMutableMap()
                    availablePaymentMethods["transference"] = PaymentMethodItem(
                        id = "transference",
                        visible = summary.paymentMethods.manualTransference.visible,
                        enabled = summary.paymentMethods.manualTransference.enabled,
                        label = null
                    )
                    updateState {
                        _uiState.value.copy(
                            availablePaymentMethods = availablePaymentMethods
                        )
                    }
                    showError()
                }
            } catch (e: Exception) {
                val availablePaymentMethods = _uiState.value.availablePaymentMethods.toMutableMap()
                availablePaymentMethods["transference"] = PaymentMethodItem(
                    id = "transference",
                    visible = summary.paymentMethods.manualTransference.visible,
                    enabled = summary.paymentMethods.manualTransference.enabled,
                    label = null
                )
                updateState {
                    _uiState.value.copy(
                        availablePaymentMethods = availablePaymentMethods
                    )
                }
                showError()
            }

        }
    }

    fun checkTransferenceUnsavedChanges() {
        val availablePaymentMethods = _uiState.value.availablePaymentMethods.toMutableMap()
        availablePaymentMethods["transference"] = PaymentMethodItem(
            id = "transference",
            visible = summary.paymentMethods.manualTransference.visible,
            enabled = summary.paymentMethods.manualTransference.enabled,
            label = null
        )
        updateState {
            _uiState.value.copy(
                availablePaymentMethods = availablePaymentMethods,
                transferenceInstructions = summary.paymentMethods.manualTransference.paymentInstructions
            )
        }
    }

    fun createBillingAgreement() {
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            business?.let {
                try {
                    val url = paymentService.createBillingAgreement(it.businessId)
                    resetSummary = true
                    showSuccess()
                    _events.emit(PaymentUiEvent.OpenBillingAgreementUrl(url))
                } catch (e: Exception) {
                    showError()
                }
            } ?: run {
                showError()
            }

        }
    }

    fun onTabSelected(index: Int) {
        updateState { copy(tab = index) }
    }

    fun onInstructionsChanged(instructions: String) {
        updateState { copy(transferenceInstructions = instructions) }
    }

    fun canUpdateInstructions(): Boolean {
        var haveUpdates = false
        if (summary.paymentMethods.manualTransference.paymentInstructions != _uiState.value.transferenceInstructions) {
            haveUpdates = true
        }

        if (summary.paymentMethods.manualTransference.enabled != _uiState.value.availablePaymentMethods["transference"]?.enabled) {
            haveUpdates = true
        }

        if (uiState.value.transferenceInstructions.isBlank() && _uiState.value.availablePaymentMethods["transference"]?.enabled == true) {
            return false
        }

        return haveUpdates
    }

    fun activatePaymentMethod(method: String, enabled: Boolean) {
        updateState {
            val availablePaymentMethods = availablePaymentMethods.toMutableMap()
            val currentMethod = availablePaymentMethods[method]
            if (currentMethod != null) {
                availablePaymentMethods[method] = currentMethod.copy(enabled = enabled)
            }
            copy(availablePaymentMethods = availablePaymentMethods)
        }
    }

}