package com.teco.ventago.features.settings.ui.settings.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.authz.ActionKey
import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.core.authz.RouteKey
import com.teco.ventago.core.camera.SharedImage
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.auth.domain.model.User
import com.teco.ventago.features.auth.domain.model.firebase.FirebaseUserDM
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.BusinessAddress
import com.teco.ventago.features.customers.domain.CustomerService
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.invoicing.domain.InvoiceHtmlSanitizer
import com.teco.ventago.features.invoicing.domain.InvoicingSettingsService
import com.teco.ventago.features.invoicing.domain.models.BottomNoteSettings
import com.teco.ventago.features.invoicing.domain.models.BottomNoteSettingsRequest
import com.teco.ventago.features.orders.domain.OrderService
import com.teco.ventago.features.printers.domain.PrinterService
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentMethodItem
import com.teco.ventago.features.product.domain.ProductService
import com.teco.ventago.features.quotes.domain.QuotesService
import com.teco.ventago.features.quotes.domain.models.QuoteSettings
import com.teco.ventago.features.settings.domain.SettingsService
import com.teco.ventago.utils.randomUUID
import com.teco.ventago.utils.uploadImageToBunnyCdn
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val authService: IAuthService,
    private val businessService: BusinessService,
    private val financialProfileService: FinancialProfileService,
    private val productService: ProductService,
    private val settingsService: SettingsService,
    private val customerService: CustomerService,
    private val branchService: BranchService,
    private val orderService: OrderService,
    private val printerService: PrinterService,
    private val logger: ILoggerService,
    private val betaService: BetaService,
    private val quotesService: QuotesService,
    private val invoicingSettingsService: InvoicingSettingsService,
) : BaseViewModel<SettingsState, SettingsStateUiEvent>(SettingsState()) {
    private companion object {
        const val MAX_BOTTOM_NOTE_TITLE_LENGTH = 255
        const val MAX_BOTTOM_NOTE_BODY_LENGTH = 3000
    }

    private data class SettingsAuthzSnapshot(
        val hasQuotesAccess: Boolean,
        val hasPaymentsAccess: Boolean,
        val canModifySettings: Boolean,
        val canModifyQuoteSettings: Boolean,
    )

    private fun normalizeHtmlForComparison(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return ""
        val normalizedBr = trimmed
            .replace(Regex(">\\s+<"), "><")
            .replace(Regex("<br\\s*/?>"), "<br/>")
        val probe = normalizedBr.replace(Regex("\\s"), "")
        return when (probe) {
            "",
            "<p></p>",
            "<p><br></p>",
            "<p><br/></p>" -> ""
            else -> normalizedBr
        }
    }

    fun isQuoteSettingsDirty(state: SettingsState = uiState.value): Boolean {
        if (!state.hasQuotesAccess || !state.canModifyQuoteSettings) return false
        val currentInfo = normalizeHtmlForComparison(state.defaultQuoteAdditionalInfo)
        val actualInfo = normalizeHtmlForComparison(state.actualDefaultQuoteAdditionalInfo)
        val infoDirty = state.quoteAdditionalInfoWasEdited && currentInfo != actualInfo
        return infoDirty ||
            state.defaultQuoteStyle.trim() != state.actualDefaultQuoteStyle.trim() ||
            state.quotePrefix.trim() != state.actualQuotePrefix.trim() ||
            state.defaultQuoteIncludePaymentButton != state.actualDefaultQuoteIncludePaymentButton
    }

    fun isBottomNoteSettingsDirty(state: SettingsState = uiState.value): Boolean {
        if (!state.invoicingEnabled || !state.canModifySettings) return false
        val currentBody = normalizeHtmlForComparison(state.bottomNoteBody)
        val actualBody = normalizeHtmlForComparison(state.actualBottomNoteBody)
        return state.bottomNoteTitle.trim() != state.actualBottomNoteTitle.trim() ||
            (state.bottomNoteBodyWasEdited && currentBody != actualBody) ||
            state.bottomNoteIncludeOnInvoice != state.actualBottomNoteIncludeOnInvoice
    }

    init {
        observeQuoteSettings()
        observeBottomNoteSettings()
        viewModelScope.launch {
            authService.getUser()
                .combine(betaService.features()) { user, betaResponse ->
                    val betaSnapshot = betaResponse?.features.orEmpty()
                        .mapNotNull(BetaFeature::fromKey)
                        .toSet()
                    SettingsAuthzSnapshot(
                        hasQuotesAccess = AuthzEvaluator.canRoute(RouteKey.QUOTES_LIST, user, betaSnapshot),
                        hasPaymentsAccess = AuthzEvaluator.canRoute(RouteKey.PAYMENTS_PAGE, user, betaSnapshot),
                        canModifySettings = AuthzEvaluator.canAction(ActionKey.SETTINGS_MODIFY, user, betaSnapshot),
                        canModifyQuoteSettings = AuthzEvaluator.canAction(ActionKey.QUOTES_UPDATE, user, betaSnapshot)
                    )
                }
                .collect { authz ->
                    updateState {
                        copy(
                            hasQuotesAccess = authz.hasQuotesAccess,
                            hasPaymentsAccess = authz.hasPaymentsAccess,
                            canModifySettings = authz.canModifySettings,
                            canModifyQuoteSettings = authz.canModifyQuoteSettings
                        )
                    }
                    if (authz.hasQuotesAccess) {
                        refreshQuoteSettingsInBackground()
                    }
            }
        }
        viewModelScope.launch {
            betaService.getFeatures()
        }
        viewModelScope.launch {
            authService.getFirebaseUser()
                .combine(authService.getUser()) { fbUser: FirebaseUserDM?, user: User? ->
                    Pair(fbUser, user)
                }.collect { newState ->
                    val fbUser = newState.first
                    val user = newState.second

                    if (fbUser != null) {
                        for (data in fbUser.providerData) {
                            if (data.equals("google.com", ignoreCase = true)) {
                                updateState { copy(canChangePassword = false) }
                            }
                        }

                        if (fbUser.isEmailVerified) {
                            updateState { copy(isVerified = true) }
                        } else {
                            val auth = Firebase.auth
                            val userFb = auth.currentUser
                            userFb?.let { firebaseUser ->
                                firebaseUser.reload()
                                updateState { copy(isVerified = firebaseUser.isEmailVerified) }
                            } ?: run {
                                signOut()
                            }
                        }
                    }
                }
        }
        viewModelScope.launch {
            financialProfileService.observe().onEach { profile ->
                profile?.let {
                    val summary = profile.paymentSummary
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
                            loadingPaymentMethods = false,
                            invoicingEnabled = profile.invoicingActive,
                        )
                    }
                    if (profile.invoicingActive) {
                        refreshBottomNoteSettingsInBackground()
                    }
                }

            }.launchIn(this)
        }
        initialChanges()
    }

    fun showDeleteAccountDialog(show: Boolean) {
        updateState { copy(showDeleteAccountDialog = show) }
    }

    fun deleteAccount() {
        viewModelScope.launch(Dispatchers.IO) {
            val token = authService.getJwtToken()
            if (token.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    showError()
                    emitEvent(SettingsStateUiEvent.DeleteAccountFailed)
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                updateState { copy(showDeleteAccountDialog = false) }
                showLoading()
            }

            try {
//                AnalyticsHelper.logEvent("account_removed")
                val res = authService.deleteAccount(token)
                if (res) {
                    clearFeatureStateAfterSignOut()
                    withContext(Dispatchers.Main) {
                        showSuccess()
                        emitEvent(SettingsStateUiEvent.AccountDeleted)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showError()
                        emitEvent(SettingsStateUiEvent.DeleteAccountFailed)
                    }
                }
            } catch (e: Exception) {
                logger.sendLog(
                    Log(
                        LogLevel.ERROR,
                        "SettingsViewModel::deleteAccount",
                        "Error deleting account. Error: ${e.message ?: "UNKNOWN"}"
                    )
                )
                withContext(Dispatchers.Main) {
                    showError()
                    emitEvent(SettingsStateUiEvent.DeleteAccountFailed)
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch(Dispatchers.IO) {
//            AnalyticsHelper.logEvent("session_closed")
            // Sign out from auth service (handles listeners, cache, Firebase, tokens)
            authService.signOut()
            clearFeatureStateAfterSignOut()

        }
//        state.signOut.value = true
    }

    private fun clearFeatureStateAfterSignOut() {
        productService.signOut()
        businessService.clear()
        financialProfileService.clear()
        customerService.clear()
        branchService.clear()
        orderService.clear()
        printerService.clear()
        invoicingSettingsService.clearBottomNoteSettings()
        invoicingSettingsService.clearInvoicingSettings()
    }


    fun isValidPhone(phone: String?): Boolean {
        return !phone.isNullOrBlank() && Regex("^\\+?[0-9]{7,15}$").matches(phone)
    }

    fun canModifySettings(): Boolean = uiState.value.canModifySettings

    fun hasMorePaymentMethods(): Boolean {
        if (uiState.value.availablePaymentMethods.isEmpty()) {
            return false
        }
        val linkedPaypal = uiState.value.availablePaymentMethods["paypal"]?.enabled ?: false
        val transference = uiState.value.availablePaymentMethods["transference"]?.enabled ?: false
        if (!uiState.value.availablePaymentMethods.containsKey("yappy")) {
            return !linkedPaypal || !transference
        } else {
            val linkedYappy = uiState.value.availablePaymentMethods["yappy"]?.enabled ?: false
            return !linkedPaypal || !linkedYappy || !transference
        }
    }

    fun resetChanges() {
        resetImage()
        initialChanges()
        updateState {
            copy(
                defaultQuoteAdditionalInfo = actualDefaultQuoteAdditionalInfo,
                quoteAdditionalInfoWasEdited = false,
                defaultQuoteStyle = actualDefaultQuoteStyle,
                quotePrefix = actualQuotePrefix,
                defaultQuoteIncludePaymentButton = actualDefaultQuoteIncludePaymentButton,
                bottomNoteTitle = actualBottomNoteTitle,
                bottomNoteBody = actualBottomNoteBody,
                bottomNoteBodyWasEdited = false,
                bottomNoteIncludeOnInvoice = actualBottomNoteIncludeOnInvoice,
                bottomNoteTitleError = null,
                bottomNoteBodyError = null,
            )
        }
    }

    fun initialChanges() {
        businessService.business.value?.let { business ->
            updateState {
                copy(
                    actualName = business.name,
                    newName = business.name,
                    actualPhone = business.phone,
                    newPhone = business.phone,
                    actualEmail = business.businessEmail ?: "",
                    newEmail = business.businessEmail ?: "",
                    actualRuc = business.ruc ?: "",
                    newRuc = business.ruc ?: "",
                    actualWeb = business.web ?: "",
                    newWeb = business.web ?: "",
                    actualAddress = business.address.placeAddress,
                    imgUrl = business.logo,
                    newAddress = null
                )
            }
        }
    }

    fun setDefaultQuoteAdditionalInfo(value: String) {
        updateState { copy(defaultQuoteAdditionalInfo = value, quoteAdditionalInfoWasEdited = true) }
    }

    fun setDefaultQuoteStyle(value: String) {
        updateState { copy(defaultQuoteStyle = value) }
    }

    fun setQuotePrefix(value: String) {
        val normalized = value.trim().take(5)
        updateState { copy(quotePrefix = normalized) }
    }

    fun setBottomNoteTitle(value: String) {
        updateState {
            copy(
                bottomNoteTitle = value.take(MAX_BOTTOM_NOTE_TITLE_LENGTH),
                bottomNoteTitleError = null,
            )
        }
    }

    fun setBottomNoteBody(value: String) {
        updateState {
            copy(
                bottomNoteBody = value,
                bottomNoteBodyWasEdited = true,
                bottomNoteBodyError = if (value.length > MAX_BOTTOM_NOTE_BODY_LENGTH) {
                    "Máximo $MAX_BOTTOM_NOTE_BODY_LENGTH caracteres"
                } else {
                    null
                },
            )
        }
    }

    fun setBottomNoteIncludeOnInvoice(value: Boolean) {
        updateState { copy(bottomNoteIncludeOnInvoice = value) }
    }

    fun updateBottomNoteIncludeOnInvoice(value: Boolean) {
        val state = uiState.value
        if (!state.canModifySettings ||
            !state.invoicingEnabled ||
            !state.bottomNoteConfigured ||
            state.bottomNoteIncludeSaving ||
            state.bottomNoteSettingsLoading
        ) {
            return
        }
        updateState {
            copy(
                bottomNoteIncludeOnInvoice = value,
                bottomNoteIncludeSaving = true,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val saved = updateBottomNoteSettingsRequest(
                title = state.actualBottomNoteTitle.ifBlank { state.bottomNoteTitle }.trim(),
                body = InvoiceHtmlSanitizer.sanitizeRichTextHtmlForSave(
                    draftHtml = state.actualBottomNoteBody.ifBlank { state.bottomNoteBody },
                    latestEditorHtml = null,
                ),
                includeOnInvoice = value,
            )
            updateState {
                if (saved != null) {
                    copy(bottomNoteIncludeSaving = false)
                } else {
                    copy(
                        bottomNoteIncludeOnInvoice = actualBottomNoteIncludeOnInvoice,
                        bottomNoteIncludeSaving = false,
                    )
                }
            }
        }
    }

    fun updateIncludeCustomerAddressOnInvoice(value: Boolean) {
        val state = uiState.value
        if (!state.canModifySettings ||
            !state.invoicingEnabled ||
            state.includeCustomerAddressSaving ||
            state.invoicingSettingsLoading
        ) {
            return
        }
        updateState {
            copy(
                includeCustomerAddressOnInvoice = value,
                includeCustomerAddressSaving = true,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val updated = runCatching {
                invoicingSettingsService.updateIncludeAddressOnInvoice(value)
            }.getOrDefault(false)
            updateState {
                if (updated) {
                    copy(
                        actualIncludeCustomerAddressOnInvoice = value,
                        includeCustomerAddressSaving = false,
                    )
                } else {
                    copy(
                        includeCustomerAddressOnInvoice = actualIncludeCustomerAddressOnInvoice,
                        includeCustomerAddressSaving = false,
                    )
                }
            }
        }
    }

    fun resetBottomNoteDraft() {
        updateState {
            copy(
                bottomNoteTitle = actualBottomNoteTitle,
                bottomNoteBody = actualBottomNoteBody,
                bottomNoteBodyWasEdited = false,
                bottomNoteIncludeOnInvoice = actualBottomNoteIncludeOnInvoice,
                bottomNoteTitleError = null,
                bottomNoteBodyError = null,
            )
        }
    }

    private fun observeQuoteSettings() {
        viewModelScope.launch {
            quotesService.quoteSettings().collect { settings ->
                if (settings != null) {
                    applyQuoteSettings(settings)
                }
            }
        }
    }

    private fun observeBottomNoteSettings() {
        viewModelScope.launch {
            invoicingSettingsService.bottomNoteSettings().collect { state ->
                applyBottomNoteSettings(state.settings)
            }
        }
        viewModelScope.launch {
            invoicingSettingsService.invoicingSettings().collect { state ->
                updateState {
                    copy(
                        actualIncludeCustomerAddressOnInvoice = state.settings.includeAddressOnInvoice,
                        includeCustomerAddressOnInvoice = state.settings.includeAddressOnInvoice,
                        invoicingSettingsLoading = false,
                        includeCustomerAddressSaving = false,
                    )
                }
            }
        }
    }

    private fun refreshQuoteSettingsInBackground() {
        viewModelScope.launch(Dispatchers.IO) {
            quotesService.refreshQuoteSettings()
        }
    }

    private fun refreshBottomNoteSettingsInBackground() {
        invoicingSettingsService.loadCachedInvoicingSettingsForCurrentBusiness()
        invoicingSettingsService.loadCachedBottomNoteSettingsForCurrentBusiness()
        updateState {
            copy(
                invoicingSettingsLoading = true,
                bottomNoteSettingsLoading = true,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { invoicingSettingsService.refreshInvoicingSettings() }
            runCatching { invoicingSettingsService.refreshBottomNoteSettings() }
            updateState {
                copy(
                    invoicingSettingsLoading = false,
                    bottomNoteSettingsLoading = false,
                )
            }
        }
    }

    private fun applyQuoteSettings(settings: QuoteSettings) {
        updateState {
            val hasUnsavedChanges = isQuoteSettingsDirty(this)
            val nextAdditionalInfo = if (hasUnsavedChanges) defaultQuoteAdditionalInfo else settings.defaultAdditionalInfo
            val nextStyle = if (hasUnsavedChanges) defaultQuoteStyle else settings.defaultQuoteStyle
            val nextPrefix = if (hasUnsavedChanges) quotePrefix else settings.quotePrefix
            val nextIncludePayment = if (hasUnsavedChanges) {
                defaultQuoteIncludePaymentButton
            } else {
                settings.defaultIncludePaymentButton
            }
            copy(
                actualDefaultQuoteAdditionalInfo = settings.defaultAdditionalInfo,
                defaultQuoteAdditionalInfo = nextAdditionalInfo,
                quoteAdditionalInfoWasEdited = if (hasUnsavedChanges) quoteAdditionalInfoWasEdited else false,
                actualDefaultQuoteStyle = settings.defaultQuoteStyle,
                defaultQuoteStyle = nextStyle,
                actualQuotePrefix = settings.quotePrefix,
                quotePrefix = nextPrefix,
                actualDefaultQuoteIncludePaymentButton = settings.defaultIncludePaymentButton,
                defaultQuoteIncludePaymentButton = nextIncludePayment
            )
        }
    }

    private suspend fun updateQuoteSettings(): Boolean? {
        val state = uiState.value
        if (!state.canModifyQuoteSettings) return null
        if (!isQuoteSettingsDirty(state)) return null
        val infoDirty = state.quoteAdditionalInfoWasEdited &&
            normalizeHtmlForComparison(state.defaultQuoteAdditionalInfo) != normalizeHtmlForComparison(state.actualDefaultQuoteAdditionalInfo)
        val settings = QuoteSettings(
            defaultQuoteStyle = state.defaultQuoteStyle,
            defaultAdditionalInfo = if (infoDirty) state.defaultQuoteAdditionalInfo else state.actualDefaultQuoteAdditionalInfo,
            quotePrefix = state.quotePrefix,
            defaultIncludePaymentButton = state.defaultQuoteIncludePaymentButton
        )
        val updated = quotesService.updateQuoteSettings(settings)
        if (updated) {
            updateState {
                copy(
                    actualDefaultQuoteAdditionalInfo = if (infoDirty) defaultQuoteAdditionalInfo else actualDefaultQuoteAdditionalInfo,
                    actualDefaultQuoteStyle = defaultQuoteStyle,
                    actualQuotePrefix = quotePrefix,
                    actualDefaultQuoteIncludePaymentButton = defaultQuoteIncludePaymentButton,
                    quoteAdditionalInfoWasEdited = false
                )
            }
        }
        return updated
    }

    private fun applyBottomNoteSettings(settings: BottomNoteSettings?) {
        updateState {
            val hasUnsavedChanges = isBottomNoteSettingsDirty(this)
            if (settings == null) {
                if (hasUnsavedChanges) {
                    copy(
                        bottomNoteConfigured = false,
                        actualBottomNoteTitle = "",
                        actualBottomNoteBody = "",
                        actualBottomNoteIncludeOnInvoice = false,
                    )
                } else {
                    copy(
                        bottomNoteConfigured = false,
                        actualBottomNoteTitle = "",
                        bottomNoteTitle = "",
                        actualBottomNoteBody = "",
                        bottomNoteBody = "",
                        bottomNoteBodyWasEdited = false,
                        actualBottomNoteIncludeOnInvoice = false,
                        bottomNoteIncludeOnInvoice = false,
                        bottomNoteTitleError = null,
                        bottomNoteBodyError = null,
                        bottomNoteSettingsLoading = false,
                        bottomNoteIncludeSaving = false,
                    )
                }
            } else {
                copy(
                    bottomNoteConfigured = true,
                    actualBottomNoteTitle = settings.title,
                    bottomNoteTitle = if (hasUnsavedChanges) bottomNoteTitle else settings.title,
                    actualBottomNoteBody = settings.body,
                    bottomNoteBody = if (hasUnsavedChanges) bottomNoteBody else settings.body,
                    bottomNoteBodyWasEdited = if (hasUnsavedChanges) bottomNoteBodyWasEdited else false,
                    actualBottomNoteIncludeOnInvoice = settings.includeOnInvoice,
                    bottomNoteIncludeOnInvoice = if (hasUnsavedChanges) {
                        bottomNoteIncludeOnInvoice
                    } else {
                        settings.includeOnInvoice
                    },
                    bottomNoteTitleError = null,
                    bottomNoteBodyError = null,
                    bottomNoteSettingsLoading = false,
                    bottomNoteIncludeSaving = false,
                )
            }
        }
    }

    fun saveBottomNoteSettings(latestBody: String? = null) {
        val state = uiState.value
        if (!state.canModifySettings || !state.invoicingEnabled) return
        val title = state.bottomNoteTitle.trim()
        val body = InvoiceHtmlSanitizer.sanitizeRichTextHtmlForSave(
            draftHtml = state.bottomNoteBody,
            latestEditorHtml = latestBody,
        )
        val normalizedBody = normalizeHtmlForComparison(body)
        val titleError = when {
            title.isBlank() -> "Ingresa un título"
            title.length > MAX_BOTTOM_NOTE_TITLE_LENGTH -> "Máximo $MAX_BOTTOM_NOTE_TITLE_LENGTH caracteres"
            else -> null
        }
        val bodyError = when {
            normalizedBody.isBlank() -> "Ingresa el texto de la factura"
            body.length > MAX_BOTTOM_NOTE_BODY_LENGTH -> "Máximo $MAX_BOTTOM_NOTE_BODY_LENGTH caracteres"
            else -> null
        }
        if (titleError != null || bodyError != null) {
            updateState { copy(bottomNoteTitleError = titleError, bottomNoteBodyError = bodyError) }
            return
        }
        if (latestBody != null && latestBody != state.bottomNoteBody) {
            updateState {
                copy(
                    bottomNoteBody = latestBody,
                    bottomNoteBodyWasEdited = true,
                    bottomNoteBodyError = null,
                )
            }
        }
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            val saved = saveBottomNoteSettingsRequest(title, body, state.bottomNoteIncludeOnInvoice)
            if (saved != null) {
                showSuccess()
            } else {
                showError()
            }
        }
    }

    private suspend fun saveBottomNoteSettingsRequest(
        title: String,
        body: String,
        includeOnInvoice: Boolean,
    ): BottomNoteSettings? = runCatching {
        invoicingSettingsService.saveBottomNoteSettings(
            BottomNoteSettingsRequest(
                title = title,
                body = body,
                includeOnInvoice = includeOnInvoice,
            ),
        )
    }.getOrNull()

    private suspend fun updateBottomNoteSettingsRequest(
        title: String,
        body: String,
        includeOnInvoice: Boolean,
    ): BottomNoteSettings? = runCatching {
        invoicingSettingsService.updateBottomNoteSettings(
            BottomNoteSettingsRequest(
                title = title,
                body = body,
                includeOnInvoice = includeOnInvoice,
            ),
        )
    }.getOrNull()

    fun deleteBottomNoteSettings() {
        if (!uiState.value.canModifySettings || !uiState.value.invoicingEnabled) return
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            val deleted = runCatching {
                invoicingSettingsService.deleteBottomNoteSettings()
            }.getOrDefault(false)
            if (deleted) {
                showSuccess()
            } else {
                showError()
            }
        }
    }

    fun onNameChange(name: String) {
        updateState {
            copy(
                newName = name,
            )
        }
    }

    fun onRucChange(ruc: String) {
        updateState {
            copy(
                newRuc = ruc,
            )
        }
    }

    fun onBusinessEmailChange(email: String) {
        updateState {
            copy(
                newEmail = email,
            )
        }
    }

    fun onWebSiteChange(website: String) {
        updateState {
            copy(
                newWeb = website,
            )
        }
    }

    fun onPhoneChanged(phone: String) {
        updateState {
            copy(
                newPhone = phone,
//                updateButtonEnabled = isValidPhone(phone) && phone != actualPhone
            )
        }
    }

    fun setAddress(address: BusinessAddress) {
        updateState {
            copy(actualAddress = address.placeAddress, newAddress = address)
        }
    }

    fun onImageSelected(sharedImage: SharedImage?) {
        viewModelScope.launch {
            val imageBitmap = withContext(Dispatchers.Default) {
                sharedImage?.toImageBitmap()
            }
            updateState {
                copy(sharedImage = sharedImage, imageBitmap = imageBitmap)
            }
        }

    }

    fun noAddressSelected() {
        viewModelScope.launch(Dispatchers.Main) {
            emitEvent(SettingsStateUiEvent.NoAddressSelected)
        }
    }

    fun launchCamera() {
        viewModelScope.launch(Dispatchers.Main) {
            emitEvent(SettingsStateUiEvent.LaunchCamera)
        }
    }

    fun launchGallery() {
        viewModelScope.launch(Dispatchers.Main) {
            emitEvent(SettingsStateUiEvent.LaunchGallery)
        }
    }

    fun launchSettings() {
        viewModelScope.launch(Dispatchers.Main) {
            emitEvent(SettingsStateUiEvent.LaunchSettings)
        }
    }

    fun showPermissionRationalDialog(show: Boolean) {
        updateState {
            copy(showPermissionRationalDialog = show)
        }
    }

    fun showUploadImageSheet(show: Boolean) {
        updateState {
            copy(showUploadImageSheet = show)
        }
    }

    fun resetImage() {
        updateState {
            copy(sharedImage = null, imageBitmap = null)
        }
    }

    fun saveChanges() {
        val tasks = mutableListOf<suspend () -> Boolean?>()
        var addedUpdateInfoTask = false
        if (uiState.value.newName != uiState.value.actualName) {
            if (uiState.value.newName.isBlank() || uiState.value.newName == "null" || uiState.value.newName.length <= 2) {
                updateState {
                    copy(newName = uiState.value.actualName)
                }
            } else if (!addedUpdateInfoTask) {
                tasks.add { updateBusinessInfo() }
                addedUpdateInfoTask = true
            }
        }
        if (uiState.value.newPhone != uiState.value.actualPhone && !addedUpdateInfoTask) {
            if (uiState.value.newPhone.isBlank() || uiState.value.newPhone == "null" || !isValidPhone(
                    uiState.value.newPhone
                )
            ) {
                updateState {
                    copy(newPhone = uiState.value.actualPhone)
                }
            } else if (!addedUpdateInfoTask) {
                tasks.add { updateBusinessInfo() }
                addedUpdateInfoTask
            }
        }
        if (uiState.value.newRuc != uiState.value.actualRuc) {
            if (uiState.value.newRuc.isBlank() || uiState.value.newRuc == "null") {
                updateState {
                    copy(newRuc = uiState.value.actualRuc)
                }
            } else if (!addedUpdateInfoTask) {
                tasks.add { updateBusinessInfo() }
                addedUpdateInfoTask
            }
        }
        if (uiState.value.newEmail != uiState.value.actualEmail && !addedUpdateInfoTask) {
            tasks.add { updateBusinessInfo() }
            addedUpdateInfoTask = true
        }
        if (uiState.value.newWeb != uiState.value.actualWeb && !addedUpdateInfoTask) {
            tasks.add { updateBusinessInfo() }
        }

        if (uiState.value.newAddress != null) {
            tasks.add { updateBusinessAddress() }
        }
        if (uiState.value.sharedImage != null) {
            tasks.add { updateBusinessLogo() }
        }
        if (isQuoteSettingsDirty(uiState.value)) {
            tasks.add { updateQuoteSettings() }
        }
        if (tasks.isEmpty()) {
            return
        }
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            var success = true
            for (task in tasks) {
                try {
                    val response = task()
                    response?.let {
                        if (success) {
                            success = it
                        }

                        if (!it) {
                            logger.sendLog(
                                Log(
                                    LogLevel.INFO,
                                    "SettingsViewModel::saveChanges",
                                    "Task $task failed"
                                )
                            )
                        }
                    } ?: run {
                        logger.sendLog(
                            Log(
                                LogLevel.WARNING,
                                "SettingsViewModel::saveChanges",
                                "Task $task returned null response"
                            )
                        )
                    }

                } catch (e: Exception) {
                    logger.sendLog(
                        Log(
                            LogLevel.ERROR,
                            "SettingsViewModel::saveChanges",
                            "Task $task exception: ${e.message}"
                        )
                    )
                    success = false
                }
            }
            if (success) {
                showSuccess()
            } else {
                showError()
            }
        }
    }

    suspend fun updateBusinessInfo(): Boolean? {
        if (!uiState.value.isNameFilled || uiState.value.newName.isBlank()) {
            updateState {
                copy(newName = uiState.value.actualName)
            }
            return null
        }

        if (!uiState.value.isPhoneFilled || uiState.value.newPhone.isBlank()) {
            updateState {
                copy(newPhone = uiState.value.actualPhone)
            }
            return null
        }

        if (uiState.value.newRuc.isBlank()) {
            updateState {
                copy(newRuc = uiState.value.actualRuc)
            }
            return null
        }

        businessService.business.value?.let { business ->
            val changed = settingsService.changeBusinessInfo(
                uiState.value.newName,
                uiState.value.newPhone,
                uiState.value.newRuc,
                uiState.value.newWeb,
                uiState.value.newEmail,
                business.businessId,
            )
            if (changed) {
                updateState {
                    copy(
                        actualName = uiState.value.newName,
                        actualPhone = uiState.value.newPhone,
                        actualRuc = uiState.value.newRuc,
                        actualWeb = uiState.value.newWeb,
                        actualEmail = uiState.value.newEmail,
                    )
                }
                return true
            }
        }

        updateState {
            copy(
                newName = uiState.value.actualName,
                newPhone = uiState.value.actualPhone,
                newRuc = uiState.value.actualRuc,
                newWeb = uiState.value.actualWeb,
                newEmail = uiState.value.actualEmail,
            )
        }
        return false
    }

    suspend fun updateBusinessAddress(): Boolean? {
        if (!uiState.value.isAddressFilled || uiState.value.newAddress == null) {
            updateState {
                copy(newAddress = null)
            }
            return null
        }

        businessService.business.value?.let { business ->
            val changed = settingsService.changeBusinessAddress(
                business.businessId,
                uiState.value.newAddress!!
            )
            if (changed) {
                updateState {
                    copy(
                        newAddress = null,
                        actualAddress = uiState.value.newAddress!!.placeAddress,
                    )
                }
                return true
            }
        }

        updateState {
            copy(newAddress = null)
        }
        return false
    }

    suspend fun updateBusinessLogo(): Boolean? {
        businessService.business.value?.let { newBusiness ->
            var imageUrl = ""
            if (uiState.value.sharedImage != null) {
                val imageData = withContext(Dispatchers.Default) {
                    uiState.value.sharedImage!!.toByteArray()
                }
                if (imageData != null) {
                    withContext(Dispatchers.IO) {
                        imageUrl = uploadImageToBunnyCdn(
                            imageData = imageData,
                            "business_${newBusiness.businessId}_${randomUUID()}.jpg",
                            newBusiness.businessId.toString()
                        ) ?: ""
                    }
                }
            }

            val res = settingsService.updateBusinessLogo(newBusiness.businessId, imageUrl)
            if (res) {
                updateState {
                    copy(
                        imgUrl = imageUrl
                    )
                }
            }

            return res
        }

        return false
    }
}
