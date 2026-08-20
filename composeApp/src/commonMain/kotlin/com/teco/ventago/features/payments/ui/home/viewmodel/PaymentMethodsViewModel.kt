package com.teco.ventago.features.payments.ui.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.authz.ActionKey
import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.financialProfile.domain.model.AchAccountSummary
import com.teco.ventago.features.financialProfile.domain.model.CardMethod
import com.teco.ventago.features.financialProfile.domain.model.CardProviderStatus
import com.teco.ventago.features.financialProfile.domain.model.FeeBillingSummary
import com.teco.ventago.features.financialProfile.domain.model.PaymentSummary
import com.teco.ventago.features.payments.domain.PaymentErrorMapper
import com.teco.ventago.features.payments.domain.PaymentService
import com.teco.ventago.features.payments.domain.models.AchAccountConfigRequest
import com.teco.ventago.features.payments.domain.models.AchStatus
import com.teco.ventago.features.payments.domain.models.FeeSummary
import com.teco.ventago.features.payments.domain.models.TiloPayStatus
import com.teco.ventago.features.payments.domain.models.YappyOnsiteDevice
import com.teco.ventago.features.payments.domain.models.YappyOnsiteDeviceConfigRequest
import com.teco.ventago.features.payments.domain.models.YappyOnsiteGroup
import com.teco.ventago.features.payments.domain.models.YappyOnsiteGroupConfigRequest
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
    private val branchService: BranchService,
    private val financialProfileService: FinancialProfileService,
    private val analyticsService: AnalyticsService,
    private val authService: IAuthService,
) : BaseViewModel<PaymentUiState, PaymentUiEvent>(PaymentUiState()) {

    private var business: Business? = null
    private var summary: PaymentSummary? = null
    private var paymentSettingsViewedLogged = false
    private var paymentOnboardingViewedLogged = false
    private var paymentOnboardingCompleted = false

    init {
        observeAuthz()
        observeBusiness()
        observeBranches()
        observeFinancialProfile()
    }

    private fun observeAuthz() {
        viewModelScope.launch {
            authService.getUser().onEach { user ->
                val canViewPayments = AuthzEvaluator.canAction(ActionKey.PAYMENTS_VIEW, user, emptySet())
                val canConfigurePayments = AuthzEvaluator.canAction(ActionKey.PAYMENTS_CONFIGURE, user, emptySet())
                val canPayFees = AuthzEvaluator.canAction(ActionKey.PAYMENTS_PAY, user, emptySet())
                updateState {
                    copy(
                        canViewPayments = canViewPayments,
                        canConfigurePayments = canConfigurePayments,
                        canPayFees = canPayFees,
                    )
                }
                recomputeStateMode()
            }.launchIn(this)
        }
    }

    private fun observeBranches() {
        viewModelScope.launch {
            branchService.observe().onEach { branches ->
                updateState {
                    val currentGroupBranch = yappyOnsiteGroupForm.branchCode
                    val firstBranchCode = branches.firstOrNull()?.branchCode.orEmpty()
                    val nextGroupBranch = currentGroupBranch.ifBlank { firstBranchCode }
                    val nextDeviceBranch = yappyOnsiteDeviceForm.branchCode.ifBlank { nextGroupBranch }
                    val nextBillingPoint = yappyOnsiteDeviceForm.billingPoint.ifBlank {
                        branches.firstOrNull { it.branchCode == nextDeviceBranch }
                            ?.fiscalBillingPoints
                            ?.firstOrNull()
                            ?.billingPoint
                            .orEmpty()
                    }
                    copy(
                        branches = branches,
                        yappyOnsiteGroupForm = yappyOnsiteGroupForm.copy(branchCode = nextGroupBranch),
                        yappyOnsiteDeviceForm = yappyOnsiteDeviceForm.copy(
                            branchCode = nextDeviceBranch,
                            billingPoint = nextBillingPoint,
                        ),
                        yappyOnsiteGroupDrafts = yappyOnsiteGroupDrafts.map { draft ->
                            if (draft.branchCode.isBlank()) draft.copy(branchCode = nextGroupBranch) else draft
                        },
                        yappyOnsiteDeviceDrafts = yappyOnsiteDeviceDrafts.map { draft ->
                            if (draft.branchCode.isBlank()) {
                                draft.copy(
                                    branchCode = nextDeviceBranch,
                                    billingPoint = nextBillingPoint,
                                )
                            } else {
                                draft
                            }
                        },
                    )
                }
            }.launchIn(this)
        }
    }

    private fun observeBusiness() {
        viewModelScope.launch {
            businessService.business.onEach { newBusiness ->
                business = newBusiness
                recomputeStateMode()
                refreshAchStatusIfReady()
            }.launchIn(this)
        }
    }

    private fun observeFinancialProfile() {
        viewModelScope.launch {
            financialProfileService.observe().onEach { profile ->
                val effectiveSummary = profile?.paymentSummary?.let { paymentSummary ->
                    uiState.value.tiloPayStatus
                        ?.takeIf { it.configuredForSettings() }
                        ?.let { paymentSummary.withTiloPayStatus(it) }
                        ?: paymentSummary
                }
                summary = effectiveSummary

                profile?.let { financialProfile ->
                    val localSummary = effectiveSummary ?: financialProfile.paymentSummary
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
                                localSummary.paymentMethods.paypal.readyForPayments()
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
                if (profile != null && businessId > 0) refreshAchStatusIfReady()
            }.launchIn(this)
        }
    }

    private fun recomputeStateMode() {
        if (!uiState.value.canViewPayments) {
            updateState {
                copy(
                    loadingSummaryData = false,
                    screenMode = PaymentScreenMode.BlockedNoPaymentsAccess,
                )
            }
            return
        }

        val currentSummary = summary

        if (currentSummary == null) {
            updateState {
                copy(
                    loadingSummaryData = true,
                    screenMode = PaymentScreenMode.Loading,
                )
            }
            return
        }

        val localAchStatus = uiState.value.achStatus
        val paymentMethods = buildAvailableMethods(currentSummary, localAchStatus)
        val currentMode = uiState.value.screenMode

        if (!currentSummary.moduleAccess.hasAccess()) {
            updateState {
                copy(
                    screenMode = PaymentScreenMode.BlockedPaymentsModuleInactive,
                    loadingSummaryData = false,
                    activeMethod = null,
                    activeStep = 1,
                    paymentSummary = currentSummary,
                    availablePaymentMethods = paymentMethods,
                    autoInvoiceEnabled = currentSummary.autoInvoiceOnPaymentSuccess,
                )
            }
            return
        }

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
    }

    private fun refreshAchStatusIfReady() {
        val businessId = business?.businessId ?: -1
        if (!uiState.value.canViewPayments || businessId <= 0 || summary == null) return
        if (summary?.moduleAccess?.hasAccess() != true) return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching { refreshAchStatus() }
        }
    }

    private fun canConfigurePaymentsOrWarn(): Boolean {
        if (summary != null && summary?.moduleAccess?.hasAccess() != true) {
            emitWarning("El módulo de pagos y cobros no está activo para este negocio.")
            return false
        }
        if (uiState.value.canConfigurePayments) return true
        emitWarning("No tienes permisos para configurar métodos de pago.")
        return false
    }

    private fun canUsePaymentsModuleOrWarn(): Boolean {
        if (summary?.moduleAccess?.hasAccess() == true) return true
        emitWarning("El módulo de pagos y cobros no está activo para este negocio.")
        return false
    }

    fun onStartOnboarding() {
        if (!canUsePaymentsModuleOrWarn()) return
        if (!canConfigurePaymentsOrWarn()) return
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
        if (!canUsePaymentsModuleOrWarn()) return
        val configured = methodConfigured(method)
        if (!configured && !canConfigurePaymentsOrWarn()) return
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
        if (method == PaymentMethodType.YappyOnsite) {
            prepareYappyOnsiteOnboardingDrafts()
            refreshYappyOnsiteConfig()
        }
        if (method == PaymentMethodType.CardTilopay) {
            refreshTiloPayStatus()
        }
    }

    fun onBackToMethods() {
        val currentSummary = summary
        updateState {
            copy(
                activeMethod = null,
                activeStep = 1,
                screenMode = when {
                    currentSummary == null -> PaymentScreenMode.Loading
                    !currentSummary.moduleAccess.hasAccess() -> PaymentScreenMode.BlockedPaymentsModuleInactive
                    currentSummary.onboardingCompleted -> PaymentScreenMode.ConfiguredList
                    else -> PaymentScreenMode.GlobalOnboarding
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
        if (!canUsePaymentsModuleOrWarn()) return
        if (!canConfigurePaymentsOrWarn()) return

        if (method == PaymentMethodType.Yappy) {
            when (state.activeStep) {
                1 -> updateState { copy(activeStep = 2) }
                2 -> updateState { copy(activeStep = 3) }
                3 -> updateState { copy(activeStep = 4) }
                else -> onBackToMethods()
            }
            return
        }

        if (method == PaymentMethodType.YappyOnsite) {
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
                3 -> updateState { copy(activeStep = 4) }
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

        if (method == PaymentMethodType.CardTilopay) {
            when (state.activeStep) {
                1 -> updateState { copy(activeStep = 2) }
                2 -> updateState { copy(activeStep = 3) }
                3 -> updateState { copy(activeStep = 4) }
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
        if (!canUsePaymentsModuleOrWarn()) return
        if (!canConfigurePaymentsOrWarn()) return
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
                it.paymentMethods.paypal.readyForPayments()
            } ?: methodConfigured(PaymentMethodType.Paypal)

            withContext(Dispatchers.Main) {
                if (isConfigured) {
                    showSuccess()
                    updateState { copy(activeStep = 5) }
                } else {
                    hideLoading()
                    emitWarning("Completa la conexión con PayPal para finalizar la configuración.")
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

    fun prepareYappyOnsiteOnboardingDrafts() {
        if (!uiState.value.canConfigurePayments) return
        updateState {
            val shouldPrepare = activeMethod == PaymentMethodType.YappyOnsite &&
                screenMode == PaymentScreenMode.MethodDetailOnboarding
            if (!shouldPrepare || yappyOnsiteGroupDrafts.isNotEmpty() || yappyOnsiteGroups.isNotEmpty() || branches.isEmpty()) {
                this
            } else {
                copy(
                    yappyOnsiteGroupDrafts = listOf(
                        YappyOnsiteGroupDraftState(
                            localId = nextYappyOnsiteDraftId(),
                            branchCode = nextAvailableYappyGroupBranch(),
                        )
                    )
                )
            }
        }
    }

    fun onAddYappyOnsiteGroupDraft() {
        if (!canConfigurePaymentsOrWarn()) return
        val state = uiState.value
        if (state.branches.isEmpty()) {
            emitWarning("Primero crea una sucursal para registrar grupos de Yappy.")
            return
        }
        if (state.yappyOnsiteGroups.size + state.yappyOnsiteGroupDrafts.size >= state.branches.size) {
            emitWarning("No puedes crear más grupos que sucursales existentes.")
            return
        }
        val branchCode = state.nextAvailableYappyGroupBranch()
        if (branchCode.isBlank()) {
            emitWarning("Todas las sucursales ya tienen un grupo asignado.")
            return
        }

        updateState {
            copy(
                yappyOnsiteGroupDrafts = yappyOnsiteGroupDrafts.map { it.copy(collapsed = true) } +
                    YappyOnsiteGroupDraftState(
                        localId = nextYappyOnsiteDraftId(),
                        branchCode = branchCode,
                    )
            )
        }
    }

    fun onRemoveYappyOnsiteGroupDraft(localId: Int) {
        if (!canConfigurePaymentsOrWarn()) return
        updateState { copy(yappyOnsiteGroupDrafts = yappyOnsiteGroupDrafts.filterNot { it.localId == localId }) }
    }

    fun onToggleYappyOnsiteGroupDraft(localId: Int) {
        if (!canConfigurePaymentsOrWarn()) return
        updateState {
            copy(
                yappyOnsiteGroupDrafts = yappyOnsiteGroupDrafts.map {
                    if (it.localId == localId) it.copy(collapsed = !it.collapsed) else it
                }
            )
        }
    }

    fun onYappyOnsiteGroupDraftIdChange(localId: Int, value: String) {
        updateState {
            copy(
                yappyOnsiteGroupDrafts = yappyOnsiteGroupDrafts.map {
                    if (it.localId == localId) it.copy(groupId = value) else it
                }
            )
        }
    }

    fun onYappyOnsiteGroupDraftBranchChange(localId: Int, value: String) {
        val state = uiState.value
        val branchAlreadyUsed = state.yappyOnsiteGroups.any { it.branchCode == value } ||
            state.yappyOnsiteGroupDrafts.any { it.localId != localId && it.branchCode == value }
        if (branchAlreadyUsed) {
            emitWarning("Ya existe un grupo para esa sucursal.")
            return
        }
        updateState {
            copy(
                yappyOnsiteGroupDrafts = yappyOnsiteGroupDrafts.map {
                    if (it.localId == localId) it.copy(branchCode = value) else it
                }
            )
        }
    }

    fun onYappyOnsiteGroupDraftApiKeyChange(localId: Int, value: String) {
        updateState {
            copy(
                yappyOnsiteGroupDrafts = yappyOnsiteGroupDrafts.map {
                    if (it.localId == localId) it.copy(apiKey = value) else it
                }
            )
        }
    }

    fun onYappyOnsiteGroupDraftSecretKeyChange(localId: Int, value: String) {
        updateState {
            copy(
                yappyOnsiteGroupDrafts = yappyOnsiteGroupDrafts.map {
                    if (it.localId == localId) it.copy(secretKey = value) else it
                }
            )
        }
    }

    fun onSaveYappyOnsiteGroups() {
        if (!canConfigurePaymentsOrWarn()) return
        val businessId = business?.businessId ?: -1
        val state = uiState.value
        val drafts = state.yappyOnsiteGroupDrafts

        if (businessId <= 0) {
            emitWarning("No se pudo identificar el negocio para configurar Yappy en caja.")
            return
        }
        if (drafts.isEmpty()) {
            if (state.yappyOnsiteGroups.isNotEmpty()) {
                updateState {
                    val target = firstAvailableYappyDeviceTarget()
                    copy(
                        activeStep = 3,
                        yappyOnsiteDeviceDrafts = if (yappyOnsiteDeviceDrafts.isEmpty() && target != null) {
                            listOf(
                                YappyOnsiteDeviceDraftState(
                                    localId = nextYappyOnsiteDraftId(),
                                    groupId = target.first.groupId,
                                    branchCode = target.first.branchCode,
                                    billingPoint = target.second,
                                )
                            )
                        } else {
                            yappyOnsiteDeviceDrafts
                        },
                    )
                }
            } else {
                emitWarning("Agrega al menos un grupo para continuar.")
            }
            return
        }
        if (YappyOnsiteOnboardingPolicy.exceedsGroupLimit(state.yappyOnsiteGroups, drafts, state.branches)) {
            emitWarning("No puedes crear más grupos que sucursales existentes.")
            return
        }

        if (YappyOnsiteOnboardingPolicy.hasDuplicateGroupBranches(state.yappyOnsiteGroups, drafts)) {
            emitWarning("Cada grupo debe usar una sucursal diferente.")
            return
        }

        val draftGroupIds = drafts.map { it.groupId.trim().lowercase() }
        if (draftGroupIds.any { it.isBlank() }) {
            emitWarning("Completa el ID de cada grupo.")
            return
        }
        if (YappyOnsiteOnboardingPolicy.hasDuplicateGroupIds(state.yappyOnsiteGroups, drafts)) {
            emitWarning("Cada ID de grupo debe ser único.")
            return
        }
        if (drafts.any { it.branchCode.isBlank() || it.apiKey.isBlank() || it.secretKey.isBlank() }) {
            emitWarning("Completa la sucursal, API key y secret key de cada grupo.")
            return
        }

        val requests = drafts.map { draft ->
            draft to YappyOnsiteGroupConfigRequest(
                name = YappyOnsiteOnboardingPolicy.groupNameForBranch(draft.branchCode, state.branches),
                apiKey = draft.apiKey.trim(),
                secretKey = draft.secretKey.trim(),
                branchCode = draft.branchCode.trim(),
            )
        }

        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                requests.map { (draft, request) ->
                    paymentService.configureYappyOnsiteGroup(
                        businessId = businessId,
                        groupId = draft.groupId.trim(),
                        request = request,
                    )
                }.all { it }
            }.onSuccess { success ->
                withContext(Dispatchers.Main) {
                    if (success) {
                        showSuccess()
                        updateState {
                            val createdGroups = requests.map { (draft, request) ->
                                YappyOnsiteGroup(
                                    businessId = businessId,
                                    groupId = draft.groupId.trim(),
                                    name = request.name,
                                    branchCode = request.branchCode,
                                    enabled = true,
                                )
                            }
                            val mergedGroups = (yappyOnsiteGroups + createdGroups)
                                .distinctBy { it.groupId.lowercase() }
                            val firstTarget = copy(yappyOnsiteGroups = mergedGroups)
                                .firstAvailableYappyDeviceTarget()
                            copy(
                                activeStep = 3,
                                yappyOnsiteGroups = mergedGroups,
                                yappyOnsiteGroupDrafts = emptyList(),
                                yappyOnsiteDeviceDrafts = if (yappyOnsiteDeviceDrafts.isEmpty() && firstTarget != null) {
                                    listOf(
                                        YappyOnsiteDeviceDraftState(
                                            localId = nextYappyOnsiteDraftId(),
                                            groupId = firstTarget.first.groupId,
                                            branchCode = firstTarget.first.branchCode,
                                            billingPoint = firstTarget.second,
                                        )
                                    )
                                } else {
                                    yappyOnsiteDeviceDrafts
                                },
                            )
                        }
                        refreshYappyOnsiteConfig()
                    } else {
                        showError()
                        emitWarning("No fue posible guardar los grupos de Yappy en caja.")
                    }
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    showError()
                    emitWarning(warningFromError(error, "No fue posible guardar los grupos de Yappy en caja."))
                }
            }
        }
    }

    fun onAddYappyOnsiteDeviceDraft() {
        if (!canConfigurePaymentsOrWarn()) return
        val state = uiState.value
        if (state.yappyOnsiteGroups.isEmpty()) {
            emitWarning("Guarda al menos un grupo antes de registrar unidades de cobro.")
            return
        }
        if (state.yappyOnsiteDevices.size + state.yappyOnsiteDeviceDrafts.size >= YappyOnsiteOnboardingPolicy.totalBillingPoints(state.branches)) {
            emitWarning("No puedes crear más unidades de cobro que puntos de facturación existentes.")
            return
        }
        val target = state.firstAvailableYappyDeviceTarget()
        if (target == null) {
            emitWarning("No hay puntos de facturación disponibles para otra unidad de cobro.")
            return
        }

        updateState {
            copy(
                yappyOnsiteDeviceDrafts = yappyOnsiteDeviceDrafts.map { it.copy(collapsed = true) } +
                    YappyOnsiteDeviceDraftState(
                        localId = nextYappyOnsiteDraftId(),
                        groupId = target.first.groupId,
                        branchCode = target.first.branchCode,
                        billingPoint = target.second,
                    )
            )
        }
    }

    fun onRemoveYappyOnsiteDeviceDraft(localId: Int) {
        if (!canConfigurePaymentsOrWarn()) return
        updateState { copy(yappyOnsiteDeviceDrafts = yappyOnsiteDeviceDrafts.filterNot { it.localId == localId }) }
    }

    fun onToggleYappyOnsiteDeviceDraft(localId: Int) {
        if (!canConfigurePaymentsOrWarn()) return
        updateState {
            copy(
                yappyOnsiteDeviceDrafts = yappyOnsiteDeviceDrafts.map {
                    if (it.localId == localId) it.copy(collapsed = !it.collapsed) else it
                }
            )
        }
    }

    fun onYappyOnsiteDeviceDraftGroupChange(localId: Int, value: String) {
        val state = uiState.value
        val group = state.yappyOnsiteGroups.firstOrNull { it.groupId == value } ?: return
        val billingPoint = state.firstAvailableYappyDeviceBillingPoint(group, excludedLocalId = localId)
        if (billingPoint.isBlank()) {
            emitWarning("Ese grupo no tiene puntos de facturación disponibles.")
            return
        }
        updateState {
            copy(
                yappyOnsiteDeviceDrafts = yappyOnsiteDeviceDrafts.map {
                    if (it.localId == localId) {
                        it.copy(
                            groupId = group.groupId,
                            branchCode = group.branchCode,
                            billingPoint = billingPoint,
                        )
                    } else {
                        it
                    }
                }
            )
        }
    }

    fun onYappyOnsiteDeviceDraftBillingPointChange(localId: Int, value: String) {
        val state = uiState.value
        val draft = state.yappyOnsiteDeviceDrafts.firstOrNull { it.localId == localId } ?: return
        val duplicate = state.yappyOnsiteDevices.any {
            it.groupId.equals(draft.groupId, ignoreCase = true) && it.billingPoint == value
        } || state.yappyOnsiteDeviceDrafts.any {
            it.localId != localId && it.groupId.equals(draft.groupId, ignoreCase = true) && it.billingPoint == value
        }
        if (duplicate) {
            emitWarning("Ya existe una unidad de cobro para ese punto de facturación.")
            return
        }
        updateState {
            copy(
                yappyOnsiteDeviceDrafts = yappyOnsiteDeviceDrafts.map {
                    if (it.localId == localId) it.copy(billingPoint = value) else it
                }
            )
        }
    }

    fun onYappyOnsiteDeviceDraftIdChange(localId: Int, value: String) {
        updateState {
            copy(
                yappyOnsiteDeviceDrafts = yappyOnsiteDeviceDrafts.map {
                    if (it.localId == localId) it.copy(deviceId = value) else it
                }
            )
        }
    }

    fun onSaveYappyOnsiteDevices() {
        if (!canConfigurePaymentsOrWarn()) return
        val businessId = business?.businessId ?: -1
        val state = uiState.value
        val drafts = state.yappyOnsiteDeviceDrafts

        if (businessId <= 0) {
            emitWarning("No se pudo identificar el negocio para configurar las unidades de cobro.")
            return
        }
        if (state.yappyOnsiteGroups.isEmpty()) {
            emitWarning("Guarda al menos un grupo antes de registrar unidades de cobro.")
            return
        }
        if (drafts.isEmpty()) {
            emitWarning("Agrega al menos una unidad de cobro para continuar.")
            return
        }
        if (YappyOnsiteOnboardingPolicy.exceedsDeviceLimit(state.yappyOnsiteDevices, drafts, state.branches)) {
            emitWarning("No puedes crear más unidades de cobro que puntos de facturación existentes.")
            return
        }
        if (drafts.any { it.groupId.isBlank() || it.billingPoint.isBlank() || it.deviceId.isBlank() }) {
            emitWarning("Completa el grupo, punto de facturación y Device ID de cada unidad de cobro.")
            return
        }

        if (YappyOnsiteOnboardingPolicy.hasDuplicateDeviceIds(state.yappyOnsiteDevices, drafts)) {
            emitWarning("Cada Device ID debe ser único por grupo.")
            return
        }

        if (YappyOnsiteOnboardingPolicy.hasDuplicateDeviceBillingPoints(state.yappyOnsiteDevices, drafts)) {
            emitWarning("Cada punto de facturación solo puede tener una unidad de cobro.")
            return
        }

        val requests = drafts.map { draft ->
            val group = state.yappyOnsiteGroups.firstOrNull { it.groupId == draft.groupId }
            draft to YappyOnsiteDeviceConfigRequest(
                deviceId = draft.deviceId.trim(),
                name = YappyOnsiteOnboardingPolicy.deviceNameForBillingPoint(
                    branchCode = group?.branchCode.orEmpty(),
                    billingPoint = draft.billingPoint,
                    branches = state.branches,
                ),
                userCode = null,
                billingPoint = draft.billingPoint.trim(),
            )
        }

        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                requests.map { (draft, request) ->
                    paymentService.registerYappyOnsiteDevice(
                        businessId = businessId,
                        groupId = draft.groupId.trim(),
                        request = request,
                    )
                }.all { it }
            }.onSuccess { success ->
                withContext(Dispatchers.Main) {
                    if (success) {
                        showSuccess()
                        updateState {
                            val createdDevices = requests.map { (draft, request) ->
                                YappyOnsiteDevice(
                                    businessId = businessId,
                                    groupId = draft.groupId.trim(),
                                    deviceId = draft.deviceId.trim(),
                                    name = request.name,
                                    branchCode = draft.branchCode.trim(),
                                    billingPoint = request.billingPoint,
                                    enabled = true,
                                )
                            }
                            copy(
                                activeStep = 4,
                                yappyOnsiteDevices = (yappyOnsiteDevices + createdDevices).distinctBy {
                                    "${it.groupId.lowercase()}|${it.deviceId.lowercase()}"
                                },
                                yappyOnsiteDeviceDrafts = emptyList(),
                            )
                        }
                        refreshYappyOnsiteConfig()
                    } else {
                        showError()
                        emitWarning("No fue posible registrar las unidades de cobro.")
                    }
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    showError()
                    emitWarning(warningFromError(error, "No fue posible registrar las unidades de cobro."))
                }
            }
        }
    }

    fun onYappyOnsiteGroupIdChange(value: String) {
        updateState { copy(yappyOnsiteGroupForm = yappyOnsiteGroupForm.copy(groupId = value)) }
    }

    fun onYappyOnsiteGroupNameChange(value: String) {
        updateState { copy(yappyOnsiteGroupForm = yappyOnsiteGroupForm.copy(name = value)) }
    }

    fun onYappyOnsiteGroupApiKeyChange(value: String) {
        updateState { copy(yappyOnsiteGroupForm = yappyOnsiteGroupForm.copy(apiKey = value)) }
    }

    fun onYappyOnsiteGroupSecretKeyChange(value: String) {
        updateState { copy(yappyOnsiteGroupForm = yappyOnsiteGroupForm.copy(secretKey = value)) }
    }

    fun onYappyOnsiteGroupBranchChange(value: String) {
        updateState {
            copy(
                yappyOnsiteGroupForm = yappyOnsiteGroupForm.copy(branchCode = value),
                yappyOnsiteDeviceForm = yappyOnsiteDeviceForm.copy(
                    branchCode = value,
                    billingPoint = branches.firstOrNull { it.branchCode == value }
                        ?.fiscalBillingPoints
                        ?.firstOrNull()
                        ?.billingPoint
                        .orEmpty()
                )
            )
        }
    }

    fun onYappyOnsiteDeviceGroupChange(value: String) {
        val group = uiState.value.yappyOnsiteGroups.firstOrNull { it.groupId == value }
        val branchCode = group?.branchCode ?: uiState.value.yappyOnsiteDeviceForm.branchCode
        val billingPoint = uiState.value.branches.firstOrNull { it.branchCode == branchCode }
            ?.fiscalBillingPoints
            ?.firstOrNull()
            ?.billingPoint
            .orEmpty()
        updateState {
            copy(
                yappyOnsiteDeviceForm = yappyOnsiteDeviceForm.copy(
                    groupId = value,
                    branchCode = branchCode,
                    billingPoint = billingPoint.ifBlank { yappyOnsiteDeviceForm.billingPoint },
                )
            )
        }
    }

    fun onYappyOnsiteDeviceIdChange(value: String) {
        updateState { copy(yappyOnsiteDeviceForm = yappyOnsiteDeviceForm.copy(deviceId = value)) }
    }

    fun onYappyOnsiteDeviceNameChange(value: String) {
        updateState { copy(yappyOnsiteDeviceForm = yappyOnsiteDeviceForm.copy(name = value)) }
    }

    fun onYappyOnsiteDeviceUserCodeChange(value: String) {
        updateState { copy(yappyOnsiteDeviceForm = yappyOnsiteDeviceForm.copy(userCode = value)) }
    }

    fun onYappyOnsiteDeviceBillingPointChange(value: String) {
        updateState { copy(yappyOnsiteDeviceForm = yappyOnsiteDeviceForm.copy(billingPoint = value)) }
    }

    fun refreshYappyOnsiteConfig() {
        val businessId = business?.businessId ?: -1
        if (businessId <= 0) return

        updateState { copy(yappyOnsiteLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val groups = paymentService.listYappyOnsiteGroups(businessId)
                val devices = paymentService.listAllYappyOnsiteDevices(businessId, groups)
                groups to devices
            }.onSuccess { (groups, devices) ->
                withContext(Dispatchers.Main) {
                    updateState {
                        val firstGroup = groups.firstOrNull()
                        val shouldResumeDeviceStep = activeMethod == PaymentMethodType.YappyOnsite &&
                            screenMode == PaymentScreenMode.MethodDetailOnboarding &&
                            activeStep <= 2 &&
                            groups.isNotEmpty() &&
                            devices.isEmpty()
                        val stateWithConfig = copy(
                            yappyOnsiteGroups = groups,
                            yappyOnsiteDevices = devices,
                        )
                        val firstTarget = if (shouldResumeDeviceStep && yappyOnsiteDeviceDrafts.isEmpty()) {
                            stateWithConfig.firstAvailableYappyDeviceTarget()
                        } else {
                            null
                        }
                        copy(
                            activeStep = if (shouldResumeDeviceStep) 3 else activeStep,
                            yappyOnsiteGroups = groups,
                            yappyOnsiteDevices = devices,
                            yappyOnsiteGroupDrafts = if (shouldResumeDeviceStep) emptyList() else yappyOnsiteGroupDrafts,
                            yappyOnsiteDeviceDrafts = if (firstTarget != null) {
                                listOf(
                                    YappyOnsiteDeviceDraftState(
                                        localId = stateWithConfig.nextYappyOnsiteDraftId(),
                                        groupId = firstTarget.first.groupId,
                                        branchCode = firstTarget.first.branchCode,
                                        billingPoint = firstTarget.second,
                                    )
                                )
                            } else {
                                yappyOnsiteDeviceDrafts
                            },
                            yappyOnsiteLoading = false,
                            yappyOnsiteDeviceForm = yappyOnsiteDeviceForm.copy(
                                groupId = yappyOnsiteDeviceForm.groupId.ifBlank { firstGroup?.groupId.orEmpty() },
                                branchCode = yappyOnsiteDeviceForm.branchCode.ifBlank { firstGroup?.branchCode.orEmpty() },
                            )
                        )
                    }
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    updateState { copy(yappyOnsiteLoading = false) }
                    emitWarning(warningFromError(error, "No fue posible cargar la configuración de Yappy en caja."))
                }
            }
        }
    }

    fun onEditYappyOnsiteGroup(groupId: String) {
        if (!canConfigurePaymentsOrWarn()) return
        val group = uiState.value.yappyOnsiteGroups.firstOrNull {
            it.groupId.equals(groupId, ignoreCase = true)
        } ?: return

        updateState {
            copy(
                showYappyOnsiteGroupSheet = true,
                editingYappyOnsiteGroupId = group.groupId,
                yappyOnsiteGroupForm = YappyOnsiteGroupFormState(
                    groupId = group.groupId,
                    name = group.name,
                    branchCode = group.branchCode,
                    apiKey = "",
                    secretKey = "",
                )
            )
        }
    }

    fun onAddYappyOnsiteGroup() {
        if (!canConfigurePaymentsOrWarn()) return
        val state = uiState.value
        if (state.yappyOnsiteGroups.size >= state.branches.size) {
            emitWarning("No hay sucursales disponibles para agregar otro grupo.")
            return
        }
        updateState {
            copy(
                showYappyOnsiteGroupSheet = true,
                editingYappyOnsiteGroupId = null,
                yappyOnsiteGroupForm = YappyOnsiteGroupFormState(
                    branchCode = nextAvailableYappyGroupBranch()
                )
            )
        }
    }

    fun onCancelYappyOnsiteGroupEdit() {
        updateState {
            copy(
                showYappyOnsiteGroupSheet = false,
                editingYappyOnsiteGroupId = null,
                yappyOnsiteGroupForm = YappyOnsiteGroupFormState(
                    branchCode = branches.firstOrNull()?.branchCode.orEmpty()
                )
            )
        }
    }

    fun requestDeleteYappyOnsiteGroup(groupId: String) {
        if (!canConfigurePaymentsOrWarn()) return
        updateState { copy(confirmDeleteYappyOnsiteGroupId = groupId) }
    }

    fun dismissDeleteYappyOnsiteGroupDialog() {
        updateState { copy(confirmDeleteYappyOnsiteGroupId = null) }
    }

    fun confirmDeleteYappyOnsiteGroup() {
        if (!canConfigurePaymentsOrWarn()) return
        val businessId = business?.businessId ?: -1
        val groupId = uiState.value.confirmDeleteYappyOnsiteGroupId?.trim().orEmpty()
        if (businessId <= 0 || groupId.isBlank()) {
            updateState { copy(confirmDeleteYappyOnsiteGroupId = null) }
            emitWarning("No se pudo identificar el grupo para eliminar.")
            return
        }

        updateState { copy(confirmDeleteYappyOnsiteGroupId = null) }
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                paymentService.deleteYappyOnsiteGroup(businessId, groupId)
            }.onSuccess { success ->
                withContext(Dispatchers.Main) {
                    if (success) {
                        showSuccess()
                        val remainingGroups = uiState.value.yappyOnsiteGroups.filterNot {
                            it.groupId.equals(groupId, ignoreCase = true)
                        }
                        if (remainingGroups.isEmpty()) {
                            onBackToMethods()
                            emitEvent(PaymentUiEvent.NavigateToPaymentMethodsHome)
                        } else {
                            onCancelYappyOnsiteGroupEdit()
                            refreshYappyOnsiteConfig()
                        }
                    } else {
                        showError()
                        emitWarning("No fue posible eliminar el grupo de Yappy en caja.")
                    }
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    showError()
                    emitWarning(warningFromError(error, "No fue posible eliminar el grupo de Yappy en caja."))
                }
            }
        }
    }

    fun onSaveYappyOnsiteGroup() {
        if (!canConfigurePaymentsOrWarn()) return
        val businessId = business?.businessId ?: -1
        val state = uiState.value
        val form = state.yappyOnsiteGroupForm
        val groupId = form.groupId.trim()
        val editingGroupId = state.editingYappyOnsiteGroupId
        val isEditing = editingGroupId != null
        val shouldStayConfigured = state.showYappyOnsiteGroupSheet &&
            state.screenMode == PaymentScreenMode.MethodDetailConfigured

        if (businessId <= 0) {
            emitWarning("No se pudo identificar el negocio para configurar Yappy en caja.")
            return
        }
        if (groupId.isBlank() || form.branchCode.isBlank()) {
            emitWarning("Completa el grupo y la sucursal para continuar.")
            return
        }
        if (!isEditing && (form.apiKey.isBlank() || form.secretKey.isBlank())) {
            emitWarning("Completa el API key y la clave secreta para crear un grupo nuevo.")
            return
        }
        if (state.yappyOnsiteGroups.any {
                it.groupId.equals(groupId, ignoreCase = true) &&
                    !it.groupId.equals(editingGroupId.orEmpty(), ignoreCase = true)
            }) {
            emitWarning("Ya existe un grupo de Yappy en caja con ese identificador.")
            return
        }
        if (state.yappyOnsiteGroups.any {
                it.branchCode == form.branchCode.trim() &&
                    !it.groupId.equals(editingGroupId.orEmpty(), ignoreCase = true)
            }) {
            emitWarning("Ya existe un grupo para esa sucursal.")
            return
        }

        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val request = YappyOnsiteGroupConfigRequest(
                    name = YappyOnsiteOnboardingPolicy.groupNameForBranch(form.branchCode, state.branches),
                    apiKey = form.apiKey.trim().takeIf { it.isNotBlank() },
                    secretKey = form.secretKey.trim().takeIf { it.isNotBlank() },
                    branchCode = form.branchCode.trim(),
                )
                val saved = paymentService.configureYappyOnsiteGroup(
                    businessId = businessId,
                    groupId = groupId,
                    request = request,
                )
                if (saved &&
                    isEditing &&
                    !groupId.equals(editingGroupId.orEmpty(), ignoreCase = true)
                ) {
                    runCatching {
                        paymentService.deleteYappyOnsiteGroup(businessId, editingGroupId.orEmpty())
                    }.onFailure { deleteError ->
                        withContext(Dispatchers.Main) {
                            emitWarning(warningFromError(deleteError, "El grupo nuevo se guardó, pero no fue posible eliminar el anterior."))
                        }
                    }
                }
                saved
            }.onSuccess { success ->
                withContext(Dispatchers.Main) {
                    if (success) {
                        showSuccess()
                        updateState {
                            copy(
                                screenMode = if (shouldStayConfigured) PaymentScreenMode.MethodDetailConfigured else screenMode,
                                activeStep = if (shouldStayConfigured) 1 else 3,
                                showYappyOnsiteGroupSheet = false,
                                editingYappyOnsiteGroupId = null,
                                yappyOnsiteGroupForm = yappyOnsiteGroupForm.copy(
                                    groupId = "",
                                    name = "",
                                    apiKey = "",
                                    secretKey = "",
                                )
                            )
                        }
                        refreshYappyOnsiteConfig()
                    } else {
                        showError()
                        emitWarning("No fue posible guardar el grupo de Yappy en caja.")
                    }
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    showError()
                    emitWarning(warningFromError(error, "No fue posible guardar el grupo de Yappy en caja."))
                }
            }
        }
    }

    fun onEditYappyOnsiteDevice(device: YappyOnsiteDevice) {
        if (!canConfigurePaymentsOrWarn()) return
        val group = uiState.value.yappyOnsiteGroups.firstOrNull {
            it.groupId.equals(device.groupId, ignoreCase = true)
        }
        updateState {
            copy(
                showYappyOnsiteDeviceSheet = true,
                editingYappyOnsiteDeviceOriginalGroupId = device.groupId,
                editingYappyOnsiteDeviceId = device.deviceId,
                yappyOnsiteDeviceForm = YappyOnsiteDeviceFormState(
                    groupId = device.groupId,
                    deviceId = device.deviceId,
                    name = device.name,
                    userCode = device.userCode,
                    branchCode = device.branchCode.ifBlank { group?.branchCode.orEmpty() },
                    billingPoint = device.billingPoint,
                )
            )
        }
    }

    fun onAddYappyOnsiteDevice() {
        if (!canConfigurePaymentsOrWarn()) return
        val state = uiState.value
        if (state.yappyOnsiteDevices.size >= YappyOnsiteOnboardingPolicy.totalBillingPoints(state.branches)) {
            emitWarning("No hay puntos de facturación disponibles para agregar otra unidad de cobro.")
            return
        }
        updateState {
            val firstTarget = firstAvailableYappyDeviceTarget()
            val firstGroup = firstTarget?.first ?: yappyOnsiteGroups.firstOrNull()
            copy(
                showYappyOnsiteDeviceSheet = true,
                editingYappyOnsiteDeviceOriginalGroupId = null,
                editingYappyOnsiteDeviceId = null,
                yappyOnsiteDeviceForm = YappyOnsiteDeviceFormState(
                    groupId = firstGroup?.groupId.orEmpty(),
                    branchCode = firstGroup?.branchCode.orEmpty(),
                    billingPoint = firstTarget?.second.orEmpty()
                )
            )
        }
    }

    fun onCancelYappyOnsiteDeviceEdit() {
        updateState {
            val firstGroup = yappyOnsiteGroups.firstOrNull()
            copy(
                showYappyOnsiteDeviceSheet = false,
                editingYappyOnsiteDeviceOriginalGroupId = null,
                editingYappyOnsiteDeviceId = null,
                yappyOnsiteDeviceForm = YappyOnsiteDeviceFormState(
                    groupId = firstGroup?.groupId.orEmpty(),
                    branchCode = firstGroup?.branchCode.orEmpty(),
                    billingPoint = branches.firstOrNull { it.branchCode == firstGroup?.branchCode }
                        ?.fiscalBillingPoints
                        ?.firstOrNull()
                        ?.billingPoint
                        .orEmpty()
                )
            )
        }
    }

    fun requestDeleteYappyOnsiteDevice(device: YappyOnsiteDevice) {
        if (!canConfigurePaymentsOrWarn()) return
        updateState { copy(confirmDeleteYappyOnsiteDevice = device) }
    }

    fun dismissDeleteYappyOnsiteDeviceDialog() {
        updateState { copy(confirmDeleteYappyOnsiteDevice = null) }
    }

    fun confirmDeleteYappyOnsiteDevice() {
        if (!canConfigurePaymentsOrWarn()) return
        val businessId = business?.businessId ?: -1
        val state = uiState.value
        val device = state.confirmDeleteYappyOnsiteDevice
        if (businessId <= 0 || device == null || device.groupId.isBlank() || device.deviceId.isBlank()) {
            updateState { copy(confirmDeleteYappyOnsiteDevice = null) }
            emitWarning("No se pudo identificar la unidad de cobro para eliminar.")
            return
        }

        val activeDevicesInGroup = state.yappyOnsiteDevices.count {
            it.groupId.equals(device.groupId, ignoreCase = true) && it.enabled
        }
        if (device.enabled && activeDevicesInGroup <= 1) {
            updateState { copy(confirmDeleteYappyOnsiteDevice = null) }
            emitWarning("No puedes eliminar la última unidad de cobro activa de este grupo.")
            return
        }

        updateState { copy(confirmDeleteYappyOnsiteDevice = null) }
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                paymentService.deleteYappyOnsiteDevice(
                    businessId = businessId,
                    groupId = device.groupId,
                    deviceId = device.deviceId,
                )
            }.onSuccess { success ->
                withContext(Dispatchers.Main) {
                    if (success) {
                        showSuccess()
                        onCancelYappyOnsiteDeviceEdit()
                        refreshYappyOnsiteConfig()
                    } else {
                        showError()
                        emitWarning("No fue posible eliminar la unidad de cobro.")
                    }
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    showError()
                    emitWarning(warningFromError(error, "No fue posible eliminar la unidad de cobro."))
                }
            }
        }
    }

    fun onSaveYappyOnsiteDevice() {
        if (!canConfigurePaymentsOrWarn()) return
        val businessId = business?.businessId ?: -1
        val state = uiState.value
        val form = state.yappyOnsiteDeviceForm
        val groupId = form.groupId.trim()
        val deviceId = form.deviceId.trim()
        val originalGroupId = state.editingYappyOnsiteDeviceOriginalGroupId
        val originalDeviceId = state.editingYappyOnsiteDeviceId
        val isEditing = originalGroupId != null && originalDeviceId != null
        val shouldStayConfigured = state.showYappyOnsiteDeviceSheet &&
            state.screenMode == PaymentScreenMode.MethodDetailConfigured

        if (businessId <= 0) {
            emitWarning("No se pudo identificar el negocio para configurar la unidad de cobro.")
            return
        }
        if (groupId.isBlank() || deviceId.isBlank() || form.billingPoint.isBlank()) {
            emitWarning("Completa el grupo, Device ID y punto de facturación.")
            return
        }
        val groupBranch = state.yappyOnsiteGroups.firstOrNull { it.groupId == groupId }?.branchCode.orEmpty()
        val branchCode = form.branchCode.ifBlank { groupBranch }
        if (state.yappyOnsiteDevices.any {
                it.groupId.equals(groupId, ignoreCase = true) &&
                    it.deviceId.equals(deviceId, ignoreCase = true) &&
                    !(isEditing &&
                        it.groupId.equals(originalGroupId.orEmpty(), ignoreCase = true) &&
                        it.deviceId.equals(originalDeviceId.orEmpty(), ignoreCase = true))
            }) {
            emitWarning("Ya existe una unidad de cobro Yappy en caja con ese identificador.")
            return
        }
        if (state.yappyOnsiteDevices.any { device ->
                device.groupId.equals(groupId, ignoreCase = true) &&
                    device.branchCode == branchCode &&
                    device.billingPoint == form.billingPoint.trim() &&
                    !(isEditing &&
                        device.groupId.equals(originalGroupId.orEmpty(), ignoreCase = true) &&
                        device.deviceId.equals(originalDeviceId.orEmpty(), ignoreCase = true))
            }) {
            emitWarning("Ya existe una unidad de cobro Yappy en caja para ese punto de facturación.")
            return
        }

        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val request = YappyOnsiteDeviceConfigRequest(
                    deviceId = if (isEditing) null else deviceId,
                    name = YappyOnsiteOnboardingPolicy.deviceNameForBillingPoint(
                        branchCode = branchCode,
                        billingPoint = form.billingPoint,
                        branches = state.branches,
                    ),
                    userCode = null,
                    billingPoint = form.billingPoint.trim(),
                )

                if (!isEditing) {
                    paymentService.registerYappyOnsiteDevice(
                        businessId = businessId,
                        groupId = groupId,
                        request = request.copy(deviceId = deviceId),
                    )
                } else if (
                    originalGroupId.orEmpty().equals(groupId, ignoreCase = true) &&
                    originalDeviceId.orEmpty().equals(deviceId, ignoreCase = true)
                ) {
                    paymentService.updateYappyOnsiteDevice(
                        businessId = businessId,
                        groupId = originalGroupId.orEmpty(),
                        deviceId = originalDeviceId.orEmpty(),
                        request = request,
                    )
                } else {
                    val created = paymentService.registerYappyOnsiteDevice(
                        businessId = businessId,
                        groupId = groupId,
                        request = request.copy(deviceId = deviceId),
                    )
                    if (created) {
                        runCatching {
                            paymentService.deleteYappyOnsiteDevice(
                                businessId = businessId,
                                groupId = originalGroupId.orEmpty(),
                                deviceId = originalDeviceId.orEmpty(),
                            )
                        }.onFailure { deleteError ->
                            withContext(Dispatchers.Main) {
                                emitWarning(warningFromError(deleteError, "La unidad de cobro nueva se guardó, pero no fue posible eliminar la anterior."))
                            }
                        }
                    }
                    created
                }
            }.onSuccess { success ->
                withContext(Dispatchers.Main) {
                    if (success) {
                        showSuccess()
                        updateState {
                            copy(
                                screenMode = if (shouldStayConfigured) PaymentScreenMode.MethodDetailConfigured else screenMode,
                                activeStep = if (shouldStayConfigured) 1 else 4,
                                showYappyOnsiteDeviceSheet = false,
                                editingYappyOnsiteDeviceOriginalGroupId = null,
                                editingYappyOnsiteDeviceId = null,
                                yappyOnsiteDeviceForm = yappyOnsiteDeviceForm.copy(
                                    deviceId = "",
                                    name = "",
                                    userCode = "",
                                )
                            )
                        }
                        refreshYappyOnsiteConfig()
                    } else {
                        showError()
                        emitWarning("No fue posible registrar la unidad de cobro.")
                    }
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    showError()
                    emitWarning(warningFromError(error, "No fue posible registrar la unidad de cobro."))
                }
            }
        }
    }

    fun onSaveYappy() {
        if (!canConfigurePaymentsOrWarn()) return
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
        if (!canConfigurePaymentsOrWarn()) return
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
        if (!canConfigurePaymentsOrWarn()) return
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

    fun onTiloPayApiUserChange(value: String) {
        updateState { copy(tiloPayForm = tiloPayForm.copy(apiUser = value)) }
    }

    fun onTiloPayPasswordChange(value: String) {
        updateState { copy(tiloPayForm = tiloPayForm.copy(password = value)) }
    }

    fun onTiloPayApiKeyChange(value: String) {
        updateState { copy(tiloPayForm = tiloPayForm.copy(apiKey = value)) }
    }

    fun refreshTiloPayStatus() {
        val businessId = business?.businessId ?: -1
        if (businessId <= 0) return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                paymentService.getTiloPayStatus(businessId)
            }.onSuccess { status ->
                withContext(Dispatchers.Main) {
                    updateState { copy(tiloPayStatus = status) }
                }
            }
        }
    }

    fun onSaveTiloPay() {
        if (!canConfigurePaymentsOrWarn()) return
        val businessId = business?.businessId ?: -1
        val form = uiState.value.tiloPayForm
        if (businessId <= 0) {
            emitWarning("No se pudo identificar el negocio para configurar TiloPay.")
            return
        }
        if (form.apiUser.isBlank() || form.password.isBlank() || form.apiKey.isBlank()) {
            emitWarning("Completa el usuario, contraseña y API key de TiloPay.")
            return
        }

        analyticsService.logPaymentMethodConfigAttempted(
            paymentMethod = "card_tilopay",
            mode = "credentials"
        )
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                paymentService.configureTiloPayCredentials(
                    businessId = businessId,
                    apiUser = form.apiUser.trim(),
                    password = form.password.trim(),
                    apiKey = form.apiKey.trim(),
                )
            }.onSuccess { status ->
                withContext(Dispatchers.Main) {
                    val updatedSummary = summary?.withTiloPayStatus(status)
                    if (updatedSummary != null) {
                        summary = updatedSummary
                        paymentOnboardingCompleted = paymentOnboardingCompleted || updatedSummary.onboardingCompleted
                    }
                    analyticsService.logPaymentMethodConfigSucceeded(
                        paymentMethod = "card_tilopay",
                        mode = "credentials"
                    )
                    showSuccess()
                    updateState {
                        copy(
                            tiloPayStatus = status,
                            tiloPayForm = TiloPayFormState(),
                            paymentSummary = updatedSummary ?: paymentSummary,
                            availablePaymentMethods = updatedSummary?.let { buildAvailableMethods(it, achStatus) }
                                ?: availablePaymentMethods,
                            activeStep = if (
                                screenMode == PaymentScreenMode.MethodDetailOnboarding &&
                                activeMethod == PaymentMethodType.CardTilopay
                            ) {
                                5
                            } else {
                                activeStep
                            },
                        )
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        financialProfileService.refresh(businessId)
                    }
                }
            }.onFailure { error ->
                analyticsService.logPaymentMethodConfigFailed(
                    paymentMethod = "card_tilopay",
                    mode = "credentials",
                    errorCode = analyticsService.extractErrorCode(error)
                )
                withContext(Dispatchers.Main) {
                    showError()
                    emitWarning(warningFromError(error, "No fue posible vincular la cuenta de TiloPay."))
                }
            }
        }
    }

    fun requestUnlinkMethod(method: PaymentMethodType) {
        if (!canConfigurePaymentsOrWarn()) return
        updateState { copy(confirmUnlinkMethod = method) }
    }

    fun dismissUnlinkDialog() {
        updateState { copy(confirmUnlinkMethod = null) }
    }

    fun confirmUnlinkMethod() {
        if (!canConfigurePaymentsOrWarn()) {
            updateState { copy(confirmUnlinkMethod = null) }
            return
        }
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
            PaymentMethodType.YappyOnsite -> "yappy_onsite"
            PaymentMethodType.Ach -> "ach"
            PaymentMethodType.CardTilopay -> "card_tilopay"
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
                    PaymentMethodType.YappyOnsite -> false
                    PaymentMethodType.Ach -> false
                    PaymentMethodType.CardTilopay -> !paymentService.disconnectTiloPay(businessId).readyForPayments()
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
                        if (method == PaymentMethodType.Paypal || method == PaymentMethodType.Yappy || method == PaymentMethodType.CardTilopay) {
                            viewModelScope.launch(Dispatchers.IO) {
                                financialProfileService.refresh(businessId)
                                if (method == PaymentMethodType.CardTilopay) {
                                    refreshTiloPayStatus()
                                }
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
        if (!canConfigurePaymentsOrWarn()) return
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
        if (!canConfigurePaymentsOrWarn()) return
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

        if (!isConfiguredAch && form.accountNumber.isBlank()) {
            emitWarning("Completa todos los campos requeridos para guardar ACH.")
            return
        }

        val achMode = if (isConfiguredAch) "update" else "create"
        val accountNumber = form.accountNumber.trim()
            .takeIf { it.isNotBlank() && it != uiState.value.achAccountNumberMasked }
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
                        accountNumber = accountNumber,
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
                                    5
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
        if (!canConfigurePaymentsOrWarn()) {
            updateState { copy(confirmDisableAch = false) }
            return
        }
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

    fun onFeeBatchSelected(batchId: Long?) {
        updateState {
            copy(
                viewMode = PaymentViewMode.Transactions,
                filters = filters.copy(
                    transactionsStatus = "",
                    transactionsMethod = null,
                    selectedBatchId = batchId,
                ),
                feeTransactions = feeTransactions.copy(page = 1, items = emptyList(), total = 0),
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            loadFeeTransactionsInternal(reset = true)
        }
    }

    fun onClearFeeBatchFilter() {
        updateState {
            copy(
                filters = filters.copy(
                    transactionsStatus = "",
                    transactionsMethod = null,
                    selectedBatchId = null,
                ),
                feeTransactions = feeTransactions.copy(page = 1, items = emptyList(), total = 0),
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            loadFeeTransactionsInternal(reset = true)
        }
    }

    fun onPayCommissions() {
        emitWarning("Las comisiones VentaGo por transacción ya no se cobran al cliente.")
    }

    fun refreshCommissions() {
        emitWarning("Las comisiones VentaGo por transacción ya no se cobran al cliente.")
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
        val shouldKeepMaskedAccount = status.configured && status.enabled
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
                            accountNumber = if (shouldKeepMaskedAccount) "" else status.account?.accountNumber.orEmpty(),
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
                batchId = state.filters.selectedBatchId,
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
            PaymentMethodType.YappyOnsite -> {
                localSummary.paymentMethods.yappy.onsite.configured &&
                    localSummary.paymentMethods.yappy.onsite.enabled
            }
            PaymentMethodType.Paypal -> localSummary.paymentMethods.paypal.readyForPayments()
            PaymentMethodType.Ach -> {
                val summaryConfigured = localSummary.paymentMethods.ach.configured &&
                    localSummary.paymentMethods.ach.enabled &&
                    localSummary.paymentMethods.ach.isActive
                val statusConfigured = achFromStatus?.configured == true && achFromStatus.enabled && achFromStatus.isActive
                statusConfigured || summaryConfigured
            }
            PaymentMethodType.CardTilopay -> localSummary.paymentMethods.card.configuredForSettings() ||
                uiState.value.tiloPayStatus?.configuredForSettings() == true
        }
    }

    fun methodVisible(method: PaymentMethodType): Boolean {
        val localSummary = summary ?: return false
        return when (method) {
            PaymentMethodType.Yappy -> localSummary.paymentMethods.yappy.visible
            PaymentMethodType.YappyOnsite -> localSummary.paymentMethods.yappy.visible ||
                localSummary.paymentMethods.yappy.onsite.configured ||
                localSummary.paymentMethods.yappy.onsite.enabled
            PaymentMethodType.Ach -> localSummary.paymentMethods.ach.visible
            PaymentMethodType.Paypal -> localSummary.paymentMethods.paypal.visible
            PaymentMethodType.CardTilopay -> localSummary.paymentMethods.card.visible ||
                localSummary.paymentMethods.card.configured ||
                localSummary.paymentMethods.card.providers.any { it.provider.equals("tilopay", ignoreCase = true) }
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

        val onsite = localSummary.paymentMethods.yappy.onsite
        if (localSummary.paymentMethods.yappy.visible || onsite.configured || onsite.enabled) {
            methods["yappy_onsite"] = PaymentMethodItem(
                id = "yappy_onsite",
                visible = true,
                enabled = onsite.configured && onsite.enabled,
                label = "${onsite.groupsCount} grupos · ${onsite.devicesCount} unidades de cobro · ${onsite.openSessionsCount} sesiones abiertas",
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
                enabled = localSummary.paymentMethods.paypal.readyForPayments(),
                label = localSummary.paymentMethods.paypal.email.ifBlank { null },
            )
        }

        val card = localSummary.paymentMethods.card
        val tiloPayProvider = card.providers.firstOrNull {
            it.provider.equals("tilopay", ignoreCase = true) ||
                it.paymentMethod.equals("card_tilopay", ignoreCase = true)
        }
        if (card.visible || card.configured || tiloPayProvider != null) {
            val tiloConfigured = tiloPayProvider?.configuredForSettings() == true ||
                card.configuredForSettings()
            methods["card_tilopay"] = PaymentMethodItem(
                id = "card_tilopay",
                visible = true,
                enabled = tiloConfigured,
                label = if (tiloConfigured) "Cuenta lista para tarjetas" else "Requiere credenciales",
            )
        }

        return methods
    }

    private fun PaymentSummary.withTiloPayStatus(status: TiloPayStatus): PaymentSummary {
        val card = paymentMethods.card
        val provider = CardProviderStatus(
            provider = status.provider.ifBlank { "tilopay" },
            paymentMethod = "card_tilopay",
            configured = status.configured,
            enabled = status.enabled,
            platformAllowed = status.platformAllowed,
            businessEnabled = status.businessEnabled,
        )
        val providers = if (card.providers.any { it.isTiloPayProvider() }) {
            card.providers.map { existing ->
                if (existing.isTiloPayProvider()) provider else existing
            }
        } else {
            card.providers + provider
        }

        return copy(
            onboardingCompleted = onboardingCompleted || status.configuredForSettings(),
            paymentMethods = paymentMethods.copy(
                card = card.copy(
                    visible = card.visible || status.configured,
                    configured = card.configured || status.configured,
                    enabled = card.enabled || status.enabled,
                    platformAllowed = card.platformAllowed || status.platformAllowed,
                    businessEnabled = card.businessEnabled || status.businessEnabled,
                    providers = providers,
                )
            )
        )
    }

    private fun TiloPayStatus.configuredForSettings(): Boolean = configured && enabled

    private fun CardProviderStatus.configuredForSettings(): Boolean = configured && enabled

    private fun CardMethod.configuredForSettings(): Boolean {
        return (configured && enabled) || providers.any { it.configuredForSettings() }
    }

    private fun CardProviderStatus.isTiloPayProvider(): Boolean {
        return provider.equals("tilopay", ignoreCase = true) ||
            paymentMethod.equals("card_tilopay", ignoreCase = true)
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
            .map { "${it.id}-${it.periodStart}-${it.periodEnd}-${it.status}-${it.issuedAt}-${it.dueAt}" }
            .toMutableSet()
        val merged = existing.toMutableList()
        incoming.forEach { item ->
            val key = "${item.id}-${item.periodStart}-${item.periodEnd}-${item.status}-${item.issuedAt}-${item.dueAt}"
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

    private fun PaymentUiState.nextYappyOnsiteDraftId(): Int {
        val groupIds = yappyOnsiteGroupDrafts.map { it.localId }
        val deviceIds = yappyOnsiteDeviceDrafts.map { it.localId }
        return ((groupIds + deviceIds).maxOrNull() ?: 0) + 1
    }

    private fun PaymentUiState.nextAvailableYappyGroupBranch(excludedLocalId: Int? = null): String {
        val usedBranchCodes = yappyOnsiteGroups.map { it.branchCode }.toSet() +
            yappyOnsiteGroupDrafts
                .filterNot { it.localId == excludedLocalId }
                .map { it.branchCode }
                .toSet()
        return branches.firstOrNull { it.branchCode !in usedBranchCodes }?.branchCode
            ?: branches.firstOrNull()?.branchCode.orEmpty()
    }

    private fun PaymentUiState.firstAvailableYappyDeviceTarget(
        excludedLocalId: Int? = null,
    ): Pair<YappyOnsiteGroup, String>? {
        return yappyOnsiteGroups.firstNotNullOfOrNull { group ->
            firstAvailableYappyDeviceBillingPoint(group, excludedLocalId)
                .takeIf { it.isNotBlank() }
                ?.let { billingPoint -> group to billingPoint }
        }
    }

    private fun PaymentUiState.firstAvailableYappyDeviceBillingPoint(
        group: YappyOnsiteGroup,
        excludedLocalId: Int? = null,
    ): String {
        val usedBillingPoints = yappyOnsiteDevices
            .filter { it.groupId.equals(group.groupId, ignoreCase = true) }
            .map { it.billingPoint }
            .toSet() +
            yappyOnsiteDeviceDrafts
                .filterNot { it.localId == excludedLocalId }
                .filter { it.groupId.equals(group.groupId, ignoreCase = true) }
                .map { it.billingPoint }
                .toSet()

        return branches
            .firstOrNull { it.branchCode == group.branchCode }
            ?.fiscalBillingPoints
            ?.firstOrNull { it.billingPoint !in usedBillingPoints }
            ?.billingPoint
            .orEmpty()
    }

    override fun onCleared() {
        if (paymentOnboardingViewedLogged && !paymentOnboardingCompleted) {
            analyticsService.logPaymentOnboardingSkipped(source = "settings")
        }
        super.onCleared()
    }
}
