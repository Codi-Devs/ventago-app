package com.teco.ventago.features.payments.ui.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.financialProfile.domain.model.AchAccountSummary
import com.teco.ventago.features.financialProfile.domain.model.FeeBillingSummary
import com.teco.ventago.features.financialProfile.domain.model.PaymentSummary
import com.teco.ventago.features.payments.domain.PaymentErrorMapper
import com.teco.ventago.features.payments.domain.PaymentService
import com.teco.ventago.features.payments.domain.models.AchAccountConfigRequest
import com.teco.ventago.features.payments.domain.models.AchStatus
import com.teco.ventago.features.payments.domain.models.FeeSummary
import com.teco.ventago.utils.toDecimalString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class PaymentMethodsViewModel(
    private val paymentService: PaymentService,
    private val businessService: BusinessService,
    private val financialProfileService: FinancialProfileService,
    private val betaService: BetaService,
    private val analyticsService: AnalyticsService,
) : BaseViewModel<PaymentUiState, PaymentUiEvent>(PaymentUiState()) {

    private var business: Business? = null
    private var summary: PaymentSummary? = null
    private var hasPaymentsAccess: Boolean? = null
    private var accessEventSent = false
    private var commissionsBootstrappedForBusinessId: Int? = null
    private var paymentSettingsViewedLogged = false
    private var paymentOnboardingViewedLogged = false
    private var paymentOnboardingCompleted = false

    init {
        observeBusiness()
        observeFinancialProfile()
        observeBetaAccess()
        viewModelScope.launch {
            betaService.getFeatures()
        }
    }

    private fun observeBusiness() {
        viewModelScope.launch {
            businessService.business.onEach { newBusiness ->
                business = newBusiness
                val businessId = newBusiness?.businessId ?: -1
                if (businessId > 0 && commissionsBootstrappedForBusinessId != businessId) {
                    commissionsBootstrappedForBusinessId = null
                }
                recomputeStateMode()
            }.launchIn(this)
        }
    }

    private fun observeFinancialProfile() {
        viewModelScope.launch {
            financialProfileService.observe().onEach { profile ->
                summary = profile?.paymentSummary

                profile?.let { financialProfile ->
                    val localSummary = financialProfile.paymentSummary
                    paymentOnboardingCompleted = paymentOnboardingCompleted || localSummary.onboardingCompleted
                    val achStatus = uiState.value.achStatus
                    val availableMethods = buildAvailableMethods(localSummary, achStatus)
                    val shouldPrefillAch = uiState.value.activeMethod != PaymentMethodType.Ach

                    updateState {
                        copy(
                            paymentSummary = localSummary,
                            availablePaymentMethods = availableMethods,
                            autoInvoiceEnabled = localSummary.autoInvoiceOnPaymentSuccess,
                            feeSummary = localSummary.feeBilling.toFeeSummary(),
                            achForm = if (shouldPrefillAch) {
                                achForm.fromAccount(
                                    achStatus?.account?.bankCode ?: localSummary.paymentMethods.ach.account?.bankCode.orEmpty(),
                                    achStatus?.account?.bankName ?: localSummary.paymentMethods.ach.account?.bankName.orEmpty(),
                                    achStatus?.account?.accountType ?: localSummary.paymentMethods.ach.account?.accountType.orEmpty(),
                                    achStatus?.account?.accountNumber ?: localSummary.paymentMethods.ach.account?.accountNumber.orEmpty(),
                                    achStatus?.account?.accountHolderName ?: localSummary.paymentMethods.ach.account?.accountHolderName.orEmpty(),
                                )
                            } else achForm,
                            achAccountNumberMasked = achStatus?.account?.accountNumberMasked
                                ?.ifBlank { null }
                                ?: localSummary.paymentMethods.ach.account?.accountNumberMasked.orEmpty(),
                            activeStep = if (
                                screenMode == PaymentScreenMode.MethodDetailOnboarding &&
                                activeMethod == PaymentMethodType.Paypal &&
                                activeStep >= 4 &&
                                localSummary.paymentMethods.paypal.linkedAccount &&
                                localSummary.linkedPaypalBillingAgreement
                            ) {
                                5
                            } else {
                                activeStep
                            },
                            errorLoadingSummaryData = false,
                        )
                    }
                }

                recomputeStateMode()

                val businessId = business?.businessId ?: -1
                if (profile != null && businessId > 0 && hasPaymentsAccess == true) {
                    viewModelScope.launch(Dispatchers.IO) {
                        runCatching {
                            refreshAchStatus()
                        }
                    }
                }
            }.launchIn(this)
        }
    }

    private fun observeBetaAccess() {
        viewModelScope.launch {
            betaService.features().onEach { features ->
                hasPaymentsAccess = features?.features?.contains(BetaFeature.PAYMENTS.key)
                recomputeStateMode()
                val businessId = business?.businessId ?: -1
                if (hasPaymentsAccess == true && businessId > 0) {
                    viewModelScope.launch(Dispatchers.IO) {
                        runCatching { refreshAchStatus() }
                    }
                }
            }.launchIn(this)
        }
    }

    private fun recomputeStateMode() {
        val access = hasPaymentsAccess
        val currentSummary = summary

        if (access == false) {
            updateState {
                copy(
                    screenMode = PaymentScreenMode.BlockedNoPaymentsAccess,
                    loadingSummaryData = false,
                )
            }
            if (!accessEventSent) {
                accessEventSent = true
                viewModelScope.launch {
                    emitEvent(PaymentUiEvent.ShowWarning("No tienes acceso a Pagos y cobros."))
                    emitEvent(PaymentUiEvent.NavigateToSettingsRoot)
                }
            }
            return
        }

        if (access == null || currentSummary == null) {
            updateState {
                copy(
                    loadingSummaryData = true,
                    screenMode = PaymentScreenMode.Loading,
                )
            }
            return
        }

        accessEventSent = false

        val localAchStatus = uiState.value.achStatus
        val paymentMethods = buildAvailableMethods(currentSummary, localAchStatus)
        val currentMode = uiState.value.screenMode

        val nextMode = when (currentMode) {
            PaymentScreenMode.MethodDetailConfigured,
            PaymentScreenMode.MethodDetailOnboarding -> currentMode
            else -> {
                if (currentSummary.onboardingCompleted) PaymentScreenMode.ConfiguredList
                else PaymentScreenMode.GlobalOnboarding
            }
        }

        updateState {
            copy(
                screenMode = nextMode,
                loadingSummaryData = false,
                paymentSummary = currentSummary,
                availablePaymentMethods = paymentMethods,
                autoInvoiceEnabled = currentSummary.autoInvoiceOnPaymentSuccess,
                feeSummary = if (feeSummary.isAllZero()) currentSummary.feeBilling.toFeeSummary() else feeSummary,
            )
        }

        if (nextMode == PaymentScreenMode.GlobalOnboarding && !paymentOnboardingViewedLogged) {
            paymentOnboardingViewedLogged = true
            analyticsService.logPaymentOnboardingViewed(source = "settings")
        }

        val businessId = business?.businessId ?: -1
        if (nextMode == PaymentScreenMode.ConfiguredList && businessId > 0) {
            maybeBootstrapCommissions(businessId)
        }
    }

    private fun maybeBootstrapCommissions(businessId: Int) {
        if (commissionsBootstrappedForBusinessId == businessId) return
        commissionsBootstrappedForBusinessId = businessId
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                loadFeeSummaryInternal()
                loadFeeTransactionsInternal(reset = true)
                loadFeeBatchesInternal(reset = true)
            }.onFailure {
                commissionsBootstrappedForBusinessId = null
            }
        }
    }

    fun onStartOnboarding() {
        analyticsService.logPaymentOnboardingStarted(source = "settings")
        if (isAddressMissing()) {
            analyticsService.logPaymentOnboardingBlocked(
                source = "settings",
                errorCode = "MISSING_ADDRESS"
            )
            updateState { copy(showAddressRequiredDialog = true) }
            return
        }

        val businessId = business?.businessId ?: -1
        if (businessId <= 0) {
            analyticsService.logPaymentOnboardingBlocked(
                source = "settings",
                errorCode = "MISSING_BUSINESS"
            )
            emitWarning("No se pudo identificar el negocio para activar pagos.")
            return
        }

        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                paymentService.onboardPayments(businessId)
            }.onSuccess { success ->
                withContext(Dispatchers.Main) {
                    if (success) {
                        paymentOnboardingCompleted = true
                        analyticsService.logPaymentOnboardingCompleted(source = "settings")
                        showSuccess()
                        viewModelScope.launch(Dispatchers.IO) {
                            financialProfileService.refresh(businessId)
                        }
                    } else {
                        analyticsService.logPaymentOnboardingFailed(
                            source = "settings",
                            errorCode = "UNKNOWN"
                        )
                        showError()
                        emitWarning("No fue posible completar la configuración inicial de pagos.")
                    }
                }
            }.onFailure { error ->
                analyticsService.logPaymentOnboardingFailed(
                    source = "settings",
                    errorCode = analyticsService.extractErrorCode(error)
                )
                withContext(Dispatchers.Main) {
                    showError()
                    emitWarning(warningFromError(error, "No fue posible completar la configuración inicial de pagos."))
                }
            }
        }
    }

    fun onEnterHomeRoute() {
        if (!paymentSettingsViewedLogged) {
            paymentSettingsViewedLogged = true
            analyticsService.logPaymentSettingsViewed()
        }
        updateState {
            copy(
                activeMethod = null,
                activeStep = 1,
                confirmUnlinkMethod = null,
                confirmDisableAch = false,
            )
        }
        recomputeStateMode()
    }

    fun dismissAddressRequiredDialog() {
        updateState { copy(showAddressRequiredDialog = false) }
    }

    fun onOpenMethod(method: PaymentMethodType) {
        val currentSummary = summary ?: return
        val configured = methodConfigured(method)
        val achStatusAccount = uiState.value.achStatus?.account
        val achSummaryAccount = currentSummary.paymentMethods.ach.account
        val openingConfiguredAch = method == PaymentMethodType.Ach && configured

        updateState {
            copy(
                activeMethod = method,
                activeStep = if (configured) 2 else 1,
                screenMode = if (configured) PaymentScreenMode.MethodDetailConfigured else PaymentScreenMode.MethodDetailOnboarding,
                achForm = achForm.fromAccount(
                    bankCode = achStatusAccount?.bankCode ?: achSummaryAccount?.bankCode.orEmpty(),
                    bankName = achStatusAccount?.bankName ?: achSummaryAccount?.bankName.orEmpty(),
                    accountType = achStatusAccount?.accountType ?: achSummaryAccount?.accountType.orEmpty(),
                    accountNumber = if (openingConfiguredAch) {
                        ""
                    } else {
                        achStatusAccount?.accountNumber ?: achSummaryAccount?.accountNumber.orEmpty()
                    },
                    accountHolderName = achStatusAccount?.accountHolderName ?: achSummaryAccount?.accountHolderName.orEmpty(),
                ),
                achAccountNumberMasked = achStatusAccount?.accountNumberMasked
                    ?.ifBlank { null }
                    ?: achSummaryAccount?.accountNumberMasked.orEmpty(),
                yappyMerchantId = "",
                yappySecretKey = "",
            )
        }

        if (method == PaymentMethodType.Ach) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { refreshAchStatus() }
            }
        }
    }

    fun onBackToMethods() {
        val currentSummary = summary
        updateState {
            copy(
                activeMethod = null,
                activeStep = 1,
                screenMode = if (currentSummary?.onboardingCompleted == true) {
                    PaymentScreenMode.ConfiguredList
                } else {
                    PaymentScreenMode.GlobalOnboarding
                }
            )
        }
    }

    fun onPrevStep() {
        val state = uiState.value
        val step = state.activeStep
        if (state.activeMethod == PaymentMethodType.Yappy && state.screenMode == PaymentScreenMode.MethodDetailOnboarding) {
            if (step <= 1) {
                onBackToMethods()
                return
            }
            updateState { copy(activeStep = step - 1) }
            return
        }

        if (step <= 1) {
            onBackToMethods()
            return
        }
        updateState { copy(activeStep = step - 1) }
    }

    fun onNextStep() {
        val state = uiState.value
        val method = state.activeMethod ?: return
        if (state.screenMode != PaymentScreenMode.MethodDetailOnboarding) return

        if (method == PaymentMethodType.Yappy) {
            when (state.activeStep) {
                1 -> updateState { copy(activeStep = 2) }
                2 -> updateState { copy(activeStep = 3) }
                3 -> updateState { copy(activeStep = 4) }
                else -> onBackToMethods()
            }
            return
        }

        if (method == PaymentMethodType.Ach) {
            when (state.activeStep) {
                1 -> updateState { copy(activeStep = 2) }
                2 -> updateState { copy(activeStep = 3) }
                else -> onBackToMethods()
            }
            return
        }

        if (method == PaymentMethodType.Paypal) {
            when (state.activeStep) {
                1 -> updateState { copy(activeStep = 2) }
                2 -> updateState { copy(activeStep = 3) }
                3 -> updateState { copy(activeStep = 4) }
                4 -> verifyPaypalOnboardingCompletion()
                else -> onBackToMethods()
            }
            return
        }

        when (state.activeStep) {
            1 -> updateState { copy(activeStep = 2) }
            2 -> {
                if (methodConfigured(method)) {
                    updateState { copy(activeStep = 3) }
                } else {
                    emitWarning("Debes completar la configuración para continuar.")
                }
            }
            else -> onBackToMethods()
        }
    }

    fun onViewModeSelected(mode: PaymentViewMode) {
        updateState { copy(viewMode = mode) }
    }

    fun onToggleAutoInvoice(enabled: Boolean) {
        val businessId = business?.businessId ?: -1
        if (businessId <= 0) {
            emitWarning("No se pudo guardar la configuración de facturación automática.")
            return
        }

        val previous = uiState.value.autoInvoiceEnabled
        updateState { copy(autoInvoiceEnabled = enabled) }
        showLoading()

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                paymentService.setAutoInvoiceOnPaymentSuccess(businessId, enabled)
            }.onSuccess { success ->
                withContext(Dispatchers.Main) {
                    if (success) {
                        showSuccess()
                    } else {
                        updateState { copy(autoInvoiceEnabled = previous) }
                        showError()
                        emitWarning("No fue posible actualizar la facturación automática.")
                    }
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    updateState { copy(autoInvoiceEnabled = previous) }
                    showError()
                    emitWarning(warningFromError(error, "No fue posible actualizar la facturación automática."))
                }
            }
        }
    }

    private fun verifyPaypalOnboardingCompletion() {
        val businessId = business?.businessId ?: -1
        if (businessId <= 0) {
            emitWarning("No se pudo identificar el negocio para validar la configuración de PayPal.")
            return
        }

        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            financialProfileService.refresh(businessId)

            val refreshedSummary = financialProfileService.observe().value?.paymentSummary
            val isConfigured = refreshedSummary?.let {
                it.paymentMethods.paypal.linkedAccount && it.linkedPaypalBillingAgreement
            } ?: methodConfigured(PaymentMethodType.Paypal)

            withContext(Dispatchers.Main) {
                if (isConfigured) {
                    showSuccess()
                    updateState { copy(activeStep = 5) }
                } else {
                    hideLoading()
                    emitWarning("Debes completar ambos pasos para finalizar la configuración.")
                }
            }
        }
    }

    fun onYappyMerchantIdChange(value: String) {
        updateState { copy(yappyMerchantId = value) }
    }

    fun onYappySecretKeyChange(value: String) {
        updateState { copy(yappySecretKey = value) }
    }

    fun onSaveYappy() {
        val businessId = business?.businessId ?: -1
        val merchantId = uiState.value.yappyMerchantId.trim()
        val secretKey = uiState.value.yappySecretKey.trim()
        val isOnboarding = uiState.value.screenMode == PaymentScreenMode.MethodDetailOnboarding

        if (businessId <= 0) {
            emitWarning("No se pudo identificar el negocio para vincular Yappy.")
            return
        }
        if (merchantId.length < 4 || secretKey.length < 4) {
            emitWarning("Completa el Merchant ID y la Clave secreta para continuar.")
            return
        }

        analyticsService.logPaymentMethodConfigAttempted(
            paymentMethod = "yappy",
            mode = "link"
        )
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                paymentService.connectYappy(
                    businessId = businessId,
                    merchantID = merchantId,
                    domain = "https://tecodigi.com",
                    secretKey = secretKey,
                )
            }.onSuccess { success ->
                withContext(Dispatchers.Main) {
                    if (success) {
                        analyticsService.logPaymentMethodConfigSucceeded(
                            paymentMethod = "yappy",
                            mode = "link"
                        )
                        showSuccess()
                        updateState {
                            copy(
                                activeStep = if (isOnboarding) 5 else activeStep,
                                yappyMerchantId = "",
                                yappySecretKey = "",
                            )
                        }
                        if (isOnboarding) {
                            viewModelScope.launch {
                                delay(1400)
                                val current = uiState.value
                                if (
                                    current.activeMethod == PaymentMethodType.Yappy &&
                                    current.screenMode == PaymentScreenMode.MethodDetailOnboarding
                                ) {
                                    onBackToMethods()
                                }
                            }
                        }
                        viewModelScope.launch(Dispatchers.IO) {
                            financialProfileService.refresh(businessId)
                        }
                    } else {
                        analyticsService.logPaymentMethodConfigFailed(
                            paymentMethod = "yappy",
                            mode = "link",
                            errorCode = "UNKNOWN"
                        )
                        showError()
                        emitWarning("No fue posible vincular la cuenta de Yappy.")
                    }
                }
            }.onFailure { error ->
                analyticsService.logPaymentMethodConfigFailed(
                    paymentMethod = "yappy",
                    mode = "link",
                    errorCode = analyticsService.extractErrorCode(error)
                )
                withContext(Dispatchers.Main) {
                    showError()
                    emitWarning(warningFromError(error, "No fue posible vincular la cuenta de Yappy."))
                }
            }
        }
    }

    fun onOpenExternalUrl(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            emitEvent(PaymentUiEvent.OpenExternalUrl(url))
        }
    }

    fun onConnectPaypal() {
        val businessId = business?.businessId ?: -1
        if (businessId <= 0) {
            emitWarning("No se pudo identificar el negocio para conectar PayPal.")
            return
        }

        analyticsService.logPaymentMethodConfigAttempted(
            paymentMethod = "paypal",
            mode = "open_app_connect"
        )
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                paymentService.connectPaypal(businessId)
            }.onSuccess { url ->
                withContext(Dispatchers.Main) {
                    if (url.isBlank()) {
                        analyticsService.logPaymentMethodConfigFailed(
                            paymentMethod = "paypal",
                            mode = "open_app_connect",
                            errorCode = "UNKNOWN"
                        )
                        showError()
                        emitWarning("No fue posible abrir la conexión de PayPal.")
                    } else {
                        analyticsService.logPaymentMethodConfigSucceeded(
                            paymentMethod = "paypal",
                            mode = "open_app_connect"
                        )
                        showSuccess()
                        emitEvent(PaymentUiEvent.OpenExternalUrl(url))
                    }
                }
            }.onFailure { error ->
                analyticsService.logPaymentMethodConfigFailed(
                    paymentMethod = "paypal",
                    mode = "open_app_connect",
                    errorCode = analyticsService.extractErrorCode(error)
                )
                withContext(Dispatchers.Main) {
                    showError()
                    emitWarning(warningFromError(error, "No fue posible abrir la conexión de PayPal."))
                }
            }
        }
    }

    fun onAuthorizePaypalBilling() {
        val businessId = business?.businessId ?: -1
        if (businessId <= 0) {
            emitWarning("No se pudo identificar el negocio para autorizar facturación con PayPal.")
            return
        }

        analyticsService.logPaymentMethodConfigAttempted(
            paymentMethod = "paypal",
            mode = "open_app_billing_agreement"
        )
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                paymentService.createBillingAgreement(businessId)
            }.onSuccess { url ->
                withContext(Dispatchers.Main) {
                    if (url.isBlank()) {
                        analyticsService.logPaymentMethodConfigFailed(
                            paymentMethod = "paypal",
                            mode = "open_app_billing_agreement",
                            errorCode = "UNKNOWN"
                        )
                        showError()
                        emitWarning("No fue posible abrir la autorización de cobros con PayPal.")
                    } else {
                        analyticsService.logPaymentMethodConfigSucceeded(
                            paymentMethod = "paypal",
                            mode = "open_app_billing_agreement"
                        )
                        showSuccess()
                        emitEvent(PaymentUiEvent.OpenExternalUrl(url))
                    }
                }
            }.onFailure { error ->
                analyticsService.logPaymentMethodConfigFailed(
                    paymentMethod = "paypal",
                    mode = "open_app_billing_agreement",
                    errorCode = analyticsService.extractErrorCode(error)
                )
                withContext(Dispatchers.Main) {
                    showError()
                    emitWarning(warningFromError(error, "No fue posible abrir la autorización de cobros con PayPal."))
                }
            }
        }
    }

    fun requestUnlinkMethod(method: PaymentMethodType) {
        updateState { copy(confirmUnlinkMethod = method) }
    }

    fun dismissUnlinkDialog() {
        updateState { copy(confirmUnlinkMethod = null) }
    }

    fun confirmUnlinkMethod() {
        val method = uiState.value.confirmUnlinkMethod ?: return
        val businessId = business?.businessId ?: -1
        if (businessId <= 0) {
            emitWarning("No se pudo identificar el negocio para desvincular este método.")
            updateState { copy(confirmUnlinkMethod = null) }
            return
        }

        updateState { copy(confirmUnlinkMethod = null) }
        val paymentMethodKey = when (method) {
            PaymentMethodType.Paypal -> "paypal"
            PaymentMethodType.Yappy -> "yappy"
            PaymentMethodType.Ach -> "ach"
        }
        analyticsService.logPaymentMethodConfigAttempted(
            paymentMethod = paymentMethodKey,
            mode = "unlink"
        )
        showLoading()

        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                when (method) {
                    PaymentMethodType.Paypal -> paymentService.unlinkPaypal(businessId)
                    PaymentMethodType.Yappy -> paymentService.unlinkYappy(businessId)
                    PaymentMethodType.Ach -> false
                }
            }

            withContext(Dispatchers.Main) {
                result.onSuccess { success ->
                    if (success) {
                        analyticsService.logPaymentMethodConfigSucceeded(
                            paymentMethod = paymentMethodKey,
                            mode = "unlink"
                        )
                        showSuccess()
                        if (method == PaymentMethodType.Paypal || method == PaymentMethodType.Yappy) {
                            viewModelScope.launch(Dispatchers.IO) {
                                financialProfileService.refresh(businessId)
                            }
                        }
                    } else {
                        analyticsService.logPaymentMethodConfigFailed(
                            paymentMethod = paymentMethodKey,
                            mode = "unlink",
                            errorCode = "UNKNOWN"
                        )
                        showError()
                        emitWarning("No fue posible desvincular el método de pago seleccionado.")
                    }
                }.onFailure { error ->
                    analyticsService.logPaymentMethodConfigFailed(
                        paymentMethod = paymentMethodKey,
                        mode = "unlink",
                        errorCode = analyticsService.extractErrorCode(error)
                    )
                    showError()
                    emitWarning(warningFromError(error, "No fue posible desvincular el método de pago seleccionado."))
                }
            }
        }
    }

    fun requestDisableAch() {
        updateState { copy(confirmDisableAch = true) }
    }

    fun dismissDisableAchDialog() {
        updateState { copy(confirmDisableAch = false) }
    }

    fun onAchBankCodeChange(value: String) {
        updateState { copy(achForm = achForm.copy(bankCode = value)) }
    }

    fun onAchBankNameChange(value: String) {
        updateState { copy(achForm = achForm.copy(bankName = value)) }
    }

    fun onAchAccountTypeChange(value: String) {
        updateState { copy(achForm = achForm.copy(accountType = value)) }
    }

    fun onAchAccountNumberChange(value: String) {
        updateState { copy(achForm = achForm.copy(accountNumber = value)) }
    }

    fun onAchAccountHolderChange(value: String) {
        updateState { copy(achForm = achForm.copy(accountHolderName = value)) }
    }

    fun onAchInstructionsChange(value: String) {
        updateState { copy(achForm = achForm.copy(instructionsText = value)) }
    }

    fun onSaveAch() {
        val businessId = business?.businessId ?: -1
        val form = uiState.value.achForm
        val isConfiguredAch = methodConfigured(PaymentMethodType.Ach)

        if (businessId <= 0) {
            emitWarning("No se pudo identificar el negocio para configurar ACH.")
            return
        }

        if (
            form.bankCode.isBlank() ||
            form.bankName.isBlank() ||
            form.accountType.isBlank() ||
            form.accountHolderName.isBlank()
        ) {
            emitWarning("Completa todos los campos requeridos para guardar ACH.")
            return
        }

        if (form.accountNumber.isBlank()) {
            if (isConfiguredAch) {
                emitWarning("Para actualizar ACH debes ingresar nuevamente el número de cuenta.")
            } else {
                emitWarning("Completa todos los campos requeridos para guardar ACH.")
            }
            return
        }

        if (isConfiguredAch && form.accountNumber == uiState.value.achAccountNumberMasked) {
            emitWarning("Para actualizar ACH debes ingresar nuevamente el número de cuenta.")
            return
        }

        val achMode = if (isConfiguredAch) "update" else "create"
        analyticsService.logPaymentMethodConfigAttempted(
            paymentMethod = "ach",
            mode = achMode
        )
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                paymentService.configureAchAccount(
                    businessId = businessId,
                    request = AchAccountConfigRequest(
                        bankCode = form.bankCode,
                        bankName = form.bankName,
                        accountType = form.accountType,
                        accountNumber = form.accountNumber,
                        accountHolderName = form.accountHolderName,
                        instructionsText = form.instructionsText,
                    )
                )
            }.onSuccess { success ->
                withContext(Dispatchers.Main) {
                    if (success) {
                        analyticsService.logPaymentMethodConfigSucceeded(
                            paymentMethod = "ach",
                            mode = achMode
                        )
                        showSuccess()
                        updateState {
                            copy(
                                activeStep = if (
                                    screenMode == PaymentScreenMode.MethodDetailOnboarding &&
                                    activeMethod == PaymentMethodType.Ach
                                ) {
                                    4
                                } else {
                                    activeStep
                                }
                            )
                        }
                        viewModelScope.launch(Dispatchers.IO) {
                            financialProfileService.refresh(businessId)
                            refreshAchStatus()
                        }
                    } else {
                        analyticsService.logPaymentMethodConfigFailed(
                            paymentMethod = "ach",
                            mode = achMode,
                            errorCode = "UNKNOWN"
                        )
                        showError()
                        emitWarning("No fue posible guardar la configuración ACH.")
                    }
                }
            }.onFailure { error ->
                analyticsService.logPaymentMethodConfigFailed(
                    paymentMethod = "ach",
                    mode = achMode,
                    errorCode = analyticsService.extractErrorCode(error)
                )
                withContext(Dispatchers.Main) {
                    showError()
                    emitWarning(warningFromError(error, "No fue posible guardar la configuración ACH."))
                }
            }
        }
    }

    fun onConfirmDisableAch() {
        updateState { copy(confirmDisableAch = false) }

        val businessId = business?.businessId ?: -1
        if (businessId <= 0) {
            emitWarning("No se pudo identificar el negocio para desactivar ACH.")
            return
        }

        analyticsService.logPaymentMethodConfigAttempted(
            paymentMethod = "ach",
            mode = "disable"
        )
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                paymentService.disableAch(businessId)
            }.onSuccess { success ->
                withContext(Dispatchers.Main) {
                    if (success) {
                        analyticsService.logPaymentMethodConfigSucceeded(
                            paymentMethod = "ach",
                            mode = "disable"
                        )
                        showSuccess()
                        updateState { copy(activeStep = 2) }
                        viewModelScope.launch(Dispatchers.IO) {
                            financialProfileService.refresh(businessId)
                            refreshAchStatus()
                        }
                    } else {
                        analyticsService.logPaymentMethodConfigFailed(
                            paymentMethod = "ach",
                            mode = "disable",
                            errorCode = "UNKNOWN"
                        )
                        showError()
                        emitWarning("No fue posible desactivar ACH.")
                    }
                }
            }.onFailure { error ->
                analyticsService.logPaymentMethodConfigFailed(
                    paymentMethod = "ach",
                    mode = "disable",
                    errorCode = analyticsService.extractErrorCode(error)
                )
                withContext(Dispatchers.Main) {
                    showError()
                    emitWarning(warningFromError(error, "No fue posible desactivar ACH."))
                }
            }
        }
    }

    fun onTransactionsStatusFilterChange(status: String) {
        updateState {
            copy(
                filters = filters.copy(transactionsStatus = status),
                feeTransactions = feeTransactions.copy(page = 1, items = emptyList(), total = 0)
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            loadFeeTransactionsInternal(reset = true)
        }
    }

    fun onTransactionsMethodFilterChange(method: String?) {
        updateState {
            copy(
                filters = filters.copy(transactionsMethod = method),
                feeTransactions = feeTransactions.copy(page = 1, items = emptyList(), total = 0)
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            loadFeeTransactionsInternal(reset = true)
        }
    }

    fun onBatchesStatusFilterChange(status: String) {
        updateState {
            copy(
                filters = filters.copy(batchesStatus = status),
                feeBatches = feeBatches.copy(page = 1, items = emptyList(), total = 0)
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            loadFeeBatchesInternal(reset = true)
        }
    }

    fun refreshCommissions() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                loadFeeSummaryInternal()
                loadFeeTransactionsInternal(reset = true)
                loadFeeBatchesInternal(reset = true)
            }
        }
    }

    fun loadMoreTransactions() {
        val current = uiState.value.feeTransactions
        if (current.loading || current.loadingMore) return
        if (current.items.size >= current.total && current.total > 0) return

        viewModelScope.launch(Dispatchers.IO) {
            loadFeeTransactionsInternal(reset = false)
        }
    }

    fun loadMoreBatches() {
        val current = uiState.value.feeBatches
        if (current.loading || current.loadingMore) return
        if (current.items.size >= current.total && current.total > 0) return

        viewModelScope.launch(Dispatchers.IO) {
            loadFeeBatchesInternal(reset = false)
        }
    }

    private suspend fun refreshAchStatus() {
        val businessId = business?.businessId ?: -1
        if (businessId <= 0) return

        val status = paymentService.getAchStatus(businessId)
        val shouldRequireAccountReentry = status.configured && status.enabled
        withContext(Dispatchers.Main) {
            updateState {
                val localSummary = paymentSummary
                copy(
                    achStatus = status,
                    availablePaymentMethods = if (localSummary != null) {
                        buildAvailableMethods(localSummary, status)
                    } else {
                        availablePaymentMethods
                    },
                    achForm = if (activeMethod == PaymentMethodType.Ach) {
                        achForm.fromAccount(
                            bankCode = status.account?.bankCode.orEmpty(),
                            bankName = status.account?.bankName.orEmpty(),
                            accountType = status.account?.accountType.orEmpty(),
                            accountNumber = if (shouldRequireAccountReentry) "" else status.account?.accountNumber.orEmpty(),
                            accountHolderName = status.account?.accountHolderName.orEmpty(),
                        )
                    } else {
                        achForm
                    },
                    achAccountNumberMasked = status.account?.accountNumberMasked.orEmpty(),
                )
            }
        }
    }

    private suspend fun loadFeeSummaryInternal() {
        val businessId = business?.businessId ?: -1
        if (businessId <= 0) return

        val currencyCode = resolveCurrencyCode()
        val result = runCatching {
            paymentService.getFeesSummary(businessId, currencyCode)
        }
        result.onSuccess { summaryData ->
            withContext(Dispatchers.Main) {
                updateState { copy(feeSummary = summaryData) }
            }
        }
    }

    private suspend fun loadFeeTransactionsInternal(reset: Boolean) {
        val businessId = business?.businessId ?: -1
        if (businessId <= 0) return

        val state = uiState.value
        val current = state.feeTransactions
        val nextPage = if (reset) 1 else current.page + 1

        withContext(Dispatchers.Main) {
            updateState {
                copy(
                    feeTransactions = feeTransactions.copy(
                        loading = reset,
                        loadingMore = !reset,
                    )
                )
            }
        }

        val result = runCatching {
            paymentService.getFeeTransactions(
                businessId = businessId,
                page = nextPage,
                size = current.size,
                status = state.filters.transactionsStatus,
                paymentMethod = state.filters.transactionsMethod,
                currencyCode = resolveCurrencyCode(),
            )
        }

        result.onSuccess { (items, total) ->
            withContext(Dispatchers.Main) {
                updateState {
                    val merged = if (reset) {
                        items
                    } else {
                        mergeTransactions(feeTransactions.items, items)
                    }
                    copy(
                        feeTransactions = feeTransactions.copy(
                            items = merged,
                            total = total,
                            page = nextPage,
                            loading = false,
                            loadingMore = false,
                        )
                    )
                }
            }
        }.onFailure {
            withContext(Dispatchers.Main) {
                updateState {
                    copy(
                        feeTransactions = feeTransactions.copy(
                            loading = false,
                            loadingMore = false,
                        )
                    )
                }
            }
        }
    }

    private suspend fun loadFeeBatchesInternal(reset: Boolean) {
        val businessId = business?.businessId ?: -1
        if (businessId <= 0) return

        val state = uiState.value
        val current = state.feeBatches
        val nextPage = if (reset) 1 else current.page + 1

        withContext(Dispatchers.Main) {
            updateState {
                copy(
                    feeBatches = feeBatches.copy(
                        loading = reset,
                        loadingMore = !reset,
                    )
                )
            }
        }

        val result = runCatching {
            paymentService.getFeeBatches(
                businessId = businessId,
                page = nextPage,
                size = current.size,
                status = state.filters.batchesStatus,
                currencyCode = resolveCurrencyCode(),
            )
        }

        result.onSuccess { (items, total) ->
            withContext(Dispatchers.Main) {
                updateState {
                    val merged = if (reset) {
                        items
                    } else {
                        mergeBatches(feeBatches.items, items)
                    }
                    copy(
                        feeBatches = feeBatches.copy(
                            items = merged,
                            total = total,
                            page = nextPage,
                            loading = false,
                            loadingMore = false,
                        )
                    )
                }
            }
        }.onFailure {
            withContext(Dispatchers.Main) {
                updateState {
                    copy(
                        feeBatches = feeBatches.copy(
                            loading = false,
                            loadingMore = false,
                        )
                    )
                }
            }
        }
    }

    fun methodConfigured(method: PaymentMethodType): Boolean {
        val localSummary = summary ?: return false
        val achFromStatus = uiState.value.achStatus
        return when (method) {
            PaymentMethodType.Yappy -> localSummary.paymentMethods.yappy.linkedAccount
            PaymentMethodType.Paypal -> localSummary.paymentMethods.paypal.linkedAccount && localSummary.linkedPaypalBillingAgreement
            PaymentMethodType.Ach -> {
                val summaryConfigured = localSummary.paymentMethods.ach.configured && localSummary.paymentMethods.ach.enabled
                val statusConfigured = achFromStatus?.configured == true && achFromStatus.enabled
                statusConfigured || summaryConfigured
            }
        }
    }

    fun methodVisible(method: PaymentMethodType): Boolean {
        val localSummary = summary ?: return false
        return when (method) {
            PaymentMethodType.Yappy -> localSummary.paymentMethods.yappy.visible
            PaymentMethodType.Ach -> localSummary.paymentMethods.ach.visible
            PaymentMethodType.Paypal -> localSummary.paymentMethods.paypal.visible
        }
    }

    fun hasConfiguredFees(): Boolean {
        val s = uiState.value.feeSummary
        return !(s.pendingDueAmount == 0L && s.overdueAmount == 0L && s.accruedCurrentPeriodAmount == 0L && s.paidAmount == 0L)
    }

    fun feesHeadlineCents(): Long {
        val s = uiState.value.feeSummary
        return s.pendingDueAmount + s.overdueAmount
    }

    fun feeDateLabel(): String {
        return if (feesHeadlineCents() > 0L) {
            "Próximo vencimiento"
        } else {
            "Próximo corte de comisiones"
        }
    }

    fun resolveFeeDateValue(): String {
        val summaryValue = uiState.value.feeSummary
        return if (feesHeadlineCents() > 0L) {
            summaryValue.nextDueAt
        } else {
            summaryValue.nextBatchGenerationAt
        }
    }

    fun resolveCurrencyCode(): String {
        val localSummary = summary
        return localSummary?.feeBilling?.currencyCode
            ?.takeIf { it.isNotBlank() }
            ?: business?.currency?.currencyCode
            ?: "USD"
    }

    fun formatCents(cents: Long): String {
        return cents.toDecimalString()
    }

    private fun buildAvailableMethods(
        localSummary: PaymentSummary,
        achStatus: AchStatus?
    ): Map<String, PaymentMethodItem> {
        val methods = linkedMapOf<String, PaymentMethodItem>()

        if (localSummary.paymentMethods.yappy.visible) {
            methods["yappy"] = PaymentMethodItem(
                id = "yappy",
                visible = true,
                enabled = localSummary.paymentMethods.yappy.linkedAccount,
                label = null,
            )
        }

        if (localSummary.paymentMethods.ach.visible) {
            val enabledFromSummary = localSummary.paymentMethods.ach.configured && localSummary.paymentMethods.ach.enabled
            val enabledFromStatus = achStatus?.configured == true && achStatus.enabled
            methods["ach"] = PaymentMethodItem(
                id = "ach",
                visible = true,
                enabled = enabledFromStatus || enabledFromSummary,
                label = achStatus?.account?.bankName
                    ?.ifBlank { null }
                    ?: localSummary.paymentMethods.ach.account?.bankName?.ifBlank { null },
            )
        }

        if (localSummary.paymentMethods.paypal.visible) {
            methods["paypal"] = PaymentMethodItem(
                id = "paypal",
                visible = true,
                enabled = localSummary.paymentMethods.paypal.linkedAccount && localSummary.linkedPaypalBillingAgreement,
                label = localSummary.paymentMethods.paypal.email.ifBlank { null },
            )
        }

        return methods
    }

    private fun warningFromError(error: Throwable, fallback: String): String {
        val raw = error.message.orEmpty()
        val code = Regex("\\\"error\\\":\\\"([^\\\"]+)\\\"").find(raw)?.groupValues?.getOrNull(1)
            ?: Regex("\\\"errorCode\\\":\\\"([^\\\"]+)\\\"").find(raw)?.groupValues?.getOrNull(1)
        return PaymentErrorMapper.messageForCode(code, fallback)
    }

    private fun isAddressMissing(): Boolean {
        val current = business ?: return true
        val placeAddress = current.address.placeAddress
        val placeId = current.address.placeId
        return placeAddress.isBlank() ||
            placeAddress.equals("null", ignoreCase = true) ||
            placeId.isBlank() ||
            placeId.equals("null", ignoreCase = true)
    }

    private fun emitWarning(message: String) {
        viewModelScope.launch {
            emitEvent(PaymentUiEvent.ShowWarning(message))
        }
    }

    private fun mergeTransactions(
        existing: List<com.teco.ventago.features.payments.domain.models.FeeTransactionItem>,
        incoming: List<com.teco.ventago.features.payments.domain.models.FeeTransactionItem>,
    ): List<com.teco.ventago.features.payments.domain.models.FeeTransactionItem> {
        val seen = existing
            .map { "${it.orderId}-${it.date}-${it.feeGenerated}-${it.paymentMethod}" }
            .toMutableSet()
        val merged = existing.toMutableList()
        incoming.forEach { item ->
            val key = "${item.orderId}-${item.date}-${item.feeGenerated}-${item.paymentMethod}"
            if (seen.add(key)) {
                merged.add(item)
            }
        }
        return merged
    }

    private fun mergeBatches(
        existing: List<com.teco.ventago.features.payments.domain.models.FeeBatchItem>,
        incoming: List<com.teco.ventago.features.payments.domain.models.FeeBatchItem>,
    ): List<com.teco.ventago.features.payments.domain.models.FeeBatchItem> {
        val seen = existing
            .map { "${it.periodStart}-${it.periodEnd}-${it.status}-${it.issuedAt}-${it.dueAt}" }
            .toMutableSet()
        val merged = existing.toMutableList()
        incoming.forEach { item ->
            val key = "${item.periodStart}-${item.periodEnd}-${item.status}-${item.issuedAt}-${item.dueAt}"
            if (seen.add(key)) {
                merged.add(item)
            }
        }
        return merged
    }

    private fun FeeBillingSummary.toFeeSummary(): FeeSummary {
        return FeeSummary(
            currencyCode = currencyCode,
            pendingDueAmount = pendingDueAmount,
            overdueAmount = overdueAmount,
            accruedCurrentPeriodAmount = accruedCurrentPeriodAmount,
            paidAmount = paidAmount,
            nextBatchGenerationAt = nextBatchGenerationAt,
            nextDueAt = nextDueAt,
        )
    }

    private fun FeeSummary.isAllZero(): Boolean {
        return pendingDueAmount == 0L &&
            overdueAmount == 0L &&
            accruedCurrentPeriodAmount == 0L &&
            paidAmount == 0L
    }

    private fun PaymentAchFormState.fromAccount(
        bankCode: String,
        bankName: String,
        accountType: String,
        accountNumber: String,
        accountHolderName: String,
    ): PaymentAchFormState {
        return copy(
            bankCode = bankCode,
            bankName = bankName,
            accountType = accountType.ifBlank { "checking" },
            accountNumber = accountNumber,
            accountHolderName = accountHolderName,
        )
    }

    private fun PaymentAchFormState.fromSummaryAccount(summaryAccount: AchAccountSummary?): PaymentAchFormState {
        return copy(
            bankCode = summaryAccount?.bankCode.orEmpty(),
            bankName = summaryAccount?.bankName.orEmpty(),
            accountType = summaryAccount?.accountType?.ifBlank { "checking" } ?: "checking",
            accountNumber = summaryAccount?.accountNumber.orEmpty(),
            accountHolderName = summaryAccount?.accountHolderName.orEmpty(),
        )
    }

    override fun onCleared() {
        if (paymentOnboardingViewedLogged && !paymentOnboardingCompleted) {
            analyticsService.logPaymentOnboardingSkipped(source = "settings")
        }
        super.onCleared()
    }
}
