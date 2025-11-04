package com.teco.ventago.features.auth.ui.register.business.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Country
import com.teco.ventago.features.business.domain.model.Currency
import com.teco.ventago.features.business.domain.model.requests.RegisterBusinessRequest
import com.teco.ventago.features.customers.domain.CustomerService
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.product.domain.ProductService
import com.teco.ventago.utils.emailRegex
import com.teco.ventago.utils.sanitizePhone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

class BusinessRegisterViewModel(
    private val authService: IAuthService,
    private val businessService: BusinessService,
    private val productService: ProductService,
    private val customerService: CustomerService,
    private val financialProfileService: FinancialProfileService,
    private val logger: ILoggerService,
    private val analytics: AnalyticsService,
) : BaseViewModel<BusinessRegisterState, BusinessRegisterUiEvent>(BusinessRegisterState()) {

    val countries = Country.loadData()
    val currencies = Currency.fromMemory()

    init {
        viewModelScope.launch {
            authService.getUser().collect {
                updateState { copy(userName = it?.name ?: "") }
            }
        }
    }


    fun signOut() {
        viewModelScope.launch {
            authService.signOut()
        }
    }

    fun nameChanged(name: String) {
        updateState { copy(name = name) }
        if (name.isNotBlank() && name.length >= 3) {
            updateState { copy(invalidName = false) }
        }
    }

    fun rucChanged(ruc: String) {
        updateState { copy(ruc = ruc) }
        if (ruc.isNotBlank() && ruc.length >= 3) {
            updateState { copy(invalidRuc = false) }
        }
    }

    fun businessPhoneChanged(phone: String) {
        updateState { copy(businessPhone = phone) }
    }

    fun businessEmailChanged(email: String) {
        updateState { copy(businessEmail = email) }
    }

    fun businessWebChanged(web: String) {
        updateState { copy(businessWeb = web) }
    }


//    fun countrySelected(index: Int) {
//        updateState { copy(selectedCountry = index) }
//        if (index != -1) {
//            updateState { copy(invalidCountry = false) }
//        }
//
//        val country = countries[index]
//        val currency = currencies.find { it.currencyCode == country.currencyCode }
//        if (currency != null) {
//            updateState { copy(selectedCurrency = currencies.indexOf(currency)) }
//        }
//    }
//
//    fun currencySelected(index: Int) {
//        updateState { copy(selectedCurrency = index) }
//        if (index != -1) {
//            updateState { copy(invalidCurrency = false) }
//        }
//    }


    fun register() {
        // Run input validations
        validateInput()
        if (uiState.value.invalidName ||
            uiState.value.invalidRuc ||
            uiState.value.invalidBusinessPhone ||
            uiState.value.invalidBusinessEmail
        ) {
            return
        }

        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = RegisterBusinessRequest(
                    name = uiState.value.name,
                    desc = "",
                    img = "",
                    ruc = uiState.value.ruc,
                    businessPhone = if (uiState.value.businessPhone.isNotBlank()) sanitizePhone(
                        uiState.value.businessPhone
                    ) else "",
                    businessEmail = uiState.value.businessEmail.ifBlank { "" },
                    web = uiState.value.businessWeb.ifBlank { "" },
                )
                val res = businessService.registerBusiness(request)

                if (res.businessId == -1 || res.menuId == 0) {
                    withContext(Dispatchers.Main) {
                        showError()
                    }
                    return@launch
                }

//                CrashlyticsService.sendLog(CrashlyticsLog.BUSINESS_CREATED)
                analytics.logEvent("business_created", analytics.businessCreatedBundle(res))

                supervisorScope {
                    val businessId = res.businessId

                    val businessJob = async {
                        runCatching { businessService.getBusinessById(businessId) }
                    }

                    val productsJob = async {
                        runCatching { productService.getProductsByBusinessId(businessId) }
                    }

                    val financialJob = async {
                        runCatching { financialProfileService.setBusiness(businessId, true) }
                    }

                    val businessRes = businessJob.await()
                    val financialRes = financialJob.await()
                    val productRes = productsJob.await()

                    businessRes.exceptionOrNull()?.let {
                        logger.sendLog(
                            Log(
                                LogLevel.ERROR,
                                "businessRegister()",
                                "getBusinessById failed: ${it.message ?: "UNKNOWN"}"
                            )
                        )
                    }
                    financialRes.exceptionOrNull()?.let {
                        logger.sendLog(
                            Log(
                                LogLevel.ERROR,
                                "register()",
                                "setBusiness (financialProfile) failed: ${it.message ?: "UNKNOWN"}"
                            )
                        )
                    }
                    productRes.exceptionOrNull()?.let {
                        logger.sendLog(
                            Log(
                                LogLevel.ERROR,
                                "register()",
                                "getProductsByBusinessId failed: ${it.message ?: "UNKNOWN"}"
                            )
                        )
                    }

                    val ok = businessRes.isSuccess && productRes.isSuccess
                    withContext(Dispatchers.Main) {
                        if (ok) showSuccess() else showError()
                    }

                }
                authService.businessRegistered()
            } catch (_: Exception) {
                showError()
            }
        }
    }

    private fun validateInput() {
        if (uiState.value.name.isBlank() || uiState.value.name.length < 3) {
            updateState { copy(invalidName = true) }
        }
        if (uiState.value.ruc.isBlank() || uiState.value.ruc.length < 3) {
            updateState { copy(invalidRuc = true) }
        }
        if (
            uiState.value.businessPhone.isNotBlank() &&
            !Regex("^((\\+507)?[0-9]{7,8}|(\\+507)?[0-9]{3,4}-[0-9]{4})$").matches(uiState.value.businessPhone)
        ) {
            updateState { copy(invalidBusinessPhone = true) }
        }
        if (uiState.value.businessEmail.isNotBlank() && !emailRegex.matches(uiState.value.businessEmail)) {
            updateState { copy(invalidBusinessEmail = true) }
        }
    }

    fun validateRUC() {
        val state = uiState.value
        if (state.ruc.isBlank() || state.ruc.length < 5) {
            return
        }

        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                println("ASDASD: Validating RUC: ${state.ruc}")
                val response = customerService.validateRUCRegister(state.ruc)
                updateState {
                    copy(
                        name = response.legalName,
                        invalidName = false,
                    )
                }
                showSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                logger.sendLog(
                    Log(
                        level = LogLevel.ERROR,
                        flow = "AddClientViewModel::validateRUC",
                        message = "Error validating RUC. Error: ${e.message ?: "UNKNOWN"}"
                    )
                )
                showError()
            }
        }
    }
}