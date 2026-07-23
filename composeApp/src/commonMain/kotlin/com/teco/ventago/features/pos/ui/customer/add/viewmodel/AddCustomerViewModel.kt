package com.teco.ventago.features.pos.ui.customer.add.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.location.PanamaLocations
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.customers.domain.CustomerService
import com.teco.ventago.features.customers.domain.models.Customer
import com.teco.ventago.features.customers.domain.models.CustomerTaxRetentionCatalog
import com.teco.ventago.features.customers.ui.form.viewmodel.CustomerCountries
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.invoicing.domain.TaxPayerType
import com.teco.ventago.features.invoicing.domain.models.FeCustomerType
import com.teco.ventago.features.invoicing.domain.models.rucNeeded
import com.teco.ventago.utils.DuplicateCustomerException
import com.teco.ventago.utils.InvalidRucException
import com.teco.ventago.utils.isValidPanamaCedula
import com.teco.ventago.utils.normalizePanamaCedula
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddCustomerViewModel(
    private val financialProfileService: FinancialProfileService,
    private val customerService: CustomerService,
    private val logger: ILoggerService
) : BaseViewModel<AddCustomerState, AddCustomerStateUiEvent>(AddCustomerState()) {

    private val invalidCedulaMessage =
        "Cedula invalida. Revise el formato (ej: 1-1234-12345, 8-88-8456, PE-123-12345, E-1234-12345, N-12345-1234, 1AV1234-12345, 1PI-1234-1234)."

    var businessId = -1

    init {
        loadCountryList()
        viewModelScope.launch {
            financialProfileService.observe().onEach { profile ->
                updateState {
                    copy(
                        invoicingEnabled = profile?.invoicingActive == true,
                    )
                }
                businessId = profile?.businessId ?: -1
            }.launchIn(this)
        }
    }

    fun loadCountryList() {
        val countries = CustomerCountries.options.map { option ->
            CountryOption(option.code, option.name)
        }.filterNot { it.code == "PA" }

        updateState { copy(countryOptions = countries) }
    }

    fun onCountrySelected(code: String) {
        updateState {
            copy(
                selectedCountryCode = code,
                validationMessage = null,
            )
        }
    }

    fun onStepBack() {
        updateState {
            copy(
                currentStep = when (currentStep) {
                    AddCustomerStep.TYPE -> AddCustomerStep.TYPE
                    AddCustomerStep.MAIN_INFO -> AddCustomerStep.TYPE
                    AddCustomerStep.OPTIONAL_INFO -> AddCustomerStep.MAIN_INFO
                },
                validationMessage = null,
            )
        }
    }

    fun onCustomerTypeSelected(customerType: FeCustomerType) {
        onCustomerTypeChange(customerType)
        updateState { copy(currentStep = AddCustomerStep.MAIN_INFO) }
    }

    fun goToOptionalInfo() {
        val state = uiState.value
        val nameError = if (state.name.isBlank() && !state.customerType.rucNeeded()) {
            "El nombre es requerido"
        } else {
            null
        }
        val rucError = if (state.customerType.rucNeeded() && state.ruc.isBlank()) {
            "El RUC es requerido"
        } else {
            null
        }
        val foreignIdNumberError = if (
            state.customerType == FeCustomerType.FOREIGNER && state.foreignIdNumber.isNullOrBlank()
        ) {
            "El documento es requerido"
        } else {
            null
        }
        val normalizedCedula = normalizePanamaCedula(state.cfCedula.orEmpty())
        val cedulaError = getCedulaValidationError(
            customerType = state.customerType,
            cedula = normalizedCedula
        )

        if (listOf(nameError, rucError, foreignIdNumberError, cedulaError).any { it != null }) {
            updateState {
                copy(
                    nameError = nameError,
                    rucError = rucError,
                    foreignIdNumberError = foreignIdNumberError,
                    cfCedula = normalizedCedula,
                    cfCedulaError = cedulaError,
                    validationMessage = "Revisa los campos marcados en rojo para continuar.",
                )
            }
            return
        }

        updateState {
            copy(
                nameError = null,
                rucError = null,
                foreignIdNumberError = null,
                cfCedula = normalizedCedula,
                cfCedulaError = null,
                validationMessage = null,
            )
        }

        if (state.customerType.rucNeeded() && (state.legalName.isNullOrBlank() || state.rucCheckDigit.isNullOrBlank())) {
            validateRucAndAdvance()
            return
        }

        updateState { copy(currentStep = AddCustomerStep.OPTIONAL_INFO) }
    }


    fun onProvinceChange(province: String) {
        if (uiState.value.selectedDistrict != null && province != null &&
            PanamaLocations.districts(province).contains(uiState.value.selectedDistrict).not()
        ) {
            updateState {
                copy(
                    selectedDistrict = null,
                    selectedCorreg = null,
                )
            }
        }
        val districtOptions = PanamaLocations.districts(province)

        updateState {
            copy(
                selectedProvince = province,
                provinceError = null,
                districtError = null,
                corregimientoError = null,
                districtOptions = districtOptions,
                validationMessage = null,
            )
        }
//        enableButton()
    }

    fun onDistrictChange(district: String) {
        if (uiState.value.selectedCorreg != null && uiState.value.selectedProvince != null && district != null &&
            PanamaLocations.corregimientos(uiState.value.selectedProvince, district)
                .contains(uiState.value.selectedCorreg).not()
        ) {
            updateState { copy(selectedCorreg = null) }
        }

        val corregOptions = PanamaLocations.corregimientos(uiState.value.selectedProvince, district)
        updateState {
            copy(
                selectedDistrict = district,
                districtError = null,
                corregimientoError = null,
                corregOptions = corregOptions,
                validationMessage = null,
            )
        }
//        enableButton()
    }

    fun onCorregimientoChange(corregimiento: String) {
        updateState {
            copy(
                selectedCorreg = corregimiento,
                corregimientoError = null,
                validationMessage = null,
            )
        }
//        enableButton()
    }

    fun onCustomerTypeChange(customerType: FeCustomerType) {
        updateState {
            copy(
                customerType = customerType,
                customerTypeSelected = true,
                ruc = "",
                rucError = null,
                rucCheckDigit = "",
                legalName = "",
                name = "",
                nameError = null,
                addressLineError = null,
                provinceError = null,
                districtError = null,
                corregimientoError = null,
                selectedProvince = if (customerType == FeCustomerType.FOREIGNER) null else DEFAULT_CUSTOMER_PROVINCE,
                selectedDistrict = if (customerType == FeCustomerType.FOREIGNER) null else DEFAULT_CUSTOMER_DISTRICT,
                selectedCorreg = if (customerType == FeCustomerType.FOREIGNER) null else DEFAULT_CUSTOMER_CORREGIMIENTO,
                districtOptions = if (customerType == FeCustomerType.FOREIGNER) {
                    emptyList()
                } else {
                    PanamaLocations.districts(DEFAULT_CUSTOMER_PROVINCE)
                },
                corregOptions = if (customerType == FeCustomerType.FOREIGNER) {
                    emptyList()
                } else {
                    PanamaLocations.corregimientos(DEFAULT_CUSTOMER_PROVINCE, DEFAULT_CUSTOMER_DISTRICT)
                },
                addressLine = if (customerType == FeCustomerType.FOREIGNER) null else DEFAULT_CUSTOMER_ADDRESS_LINE,
                addressExpanded = false,
                cfCedula = "",
                cfCedulaError = null,
                foreignIdNumber = "",
                foreignIdNumberError = null,
                selectedForeignIdType = ForeignIdType.PASSPORT,
                selectedCountryCode = DEFAULT_FOREIGN_CUSTOMER_COUNTRY,
                validationMessage = null,
            )
        }
    }

    fun onTaxIdChange(taxId: String) {
        val normalizedTaxId = taxId.uppercase()
        updateState {
            copy(
                ruc = normalizedTaxId,
                rucError = null,
                validationMessage = null,
            )
        }
        if (uiState.value.customerType == FeCustomerType.CONTRIBUTING || uiState.value.customerType == FeCustomerType.GOVERNMENT) {
            updateState {
                copy(
                    legalName = "",
                    rucCheckDigit = "",
                    name = "",
                    rucError = null,
                    validationMessage = null,
                )
            }
        }
    }

    fun onNameChange(name: String) {
        updateState {
            copy(
                name = name,
                nameError = null,
                validationMessage = null,
            )
        }
    }

    fun onForeignIdTypeSelected(type: ForeignIdType) {
        updateState { copy(selectedForeignIdType = type, validationMessage = null) }
    }

    fun onForeignIdNumberChange(value: String) {
        updateState { copy(foreignIdNumber = value, foreignIdNumberError = null, validationMessage = null) }
    }

    fun onCedulaChanges(cedula: String) {
        val normalizedCedula = normalizePanamaCedula(cedula)
        updateState {
            copy(
                cfCedula = normalizedCedula,
                cfCedulaError = getCedulaValidationError(
                    customerType = customerType,
                    cedula = normalizedCedula
                ),
                validationMessage = null,
            )
        }
    }

    fun onPhoneChange(phone: String) {
        updateState {
            copy(phone = phone, validationMessage = null)
        }
    }

    fun onEmailChange(email: String) {
        updateState { copy(email = email, validationMessage = null) }
    }

    fun onAddressLineChange(addressLine: String) {
        updateState {
            copy(
                addressLine = addressLine,
                addressLineError = null,
                validationMessage = null,
            )
        }
    }


    fun onTagsChange(tags: List<String>) {
        updateState { copy(tags = tags, validationMessage = null) }
    }

    fun toggleAddressExpanded() {
        updateState { copy(addressExpanded = !addressExpanded) }
    }

    fun onTaxExemptChange(value: Boolean) {
        updateState { copy(taxExempt = value, validationMessage = null) }
    }

    fun taxRetentionLabels(): List<String> = CustomerTaxRetentionCatalog.options.map { it.label }

    fun selectedTaxRetentionIndex(): Int =
        CustomerTaxRetentionCatalog.indexOfCode(uiState.value.taxRetentionCode)

    fun onTaxRetentionSelected(index: Int) {
        val option = CustomerTaxRetentionCatalog.options.getOrNull(index)
            ?: CustomerTaxRetentionCatalog.options.first()
        updateState {
            val previousCode = CustomerTaxRetentionCatalog.normalizeCode(taxRetentionCode)
            val nextPercent = when {
                option.code.isEmpty() -> ""
                option.defaultRate != null -> option.defaultRate.toString()
                option.code == "8" && previousCode == "8" -> taxRetentionPercent
                else -> ""
            }
            copy(
                taxRetentionCode = option.code,
                taxRetentionPercent = nextPercent,
                taxRetentionPercentError = null,
                validationMessage = null,
            )
        }
    }

    fun onTaxRetentionPercentChange(value: String) {
        val filtered = value.filter { it.isDigit() }.take(3)
        val normalized = when {
            filtered.isEmpty() -> ""
            (filtered.toIntOrNull() ?: 0) > 100 -> "100"
            else -> filtered
        }
        updateState {
            copy(
                taxRetentionPercent = normalized,
                taxRetentionPercentError = null,
                validationMessage = null,
            )
        }
    }

    fun selectedTaxRetentionRequiresManualPercent(): Boolean {
        return CustomerTaxRetentionCatalog.normalizeCode(uiState.value.taxRetentionCode) == "8"
    }


    fun validateRUC() {
        val state = uiState.value
        if (state.ruc.isBlank() || state.ruc.length < 5 || businessId == -1) {
            updateState {
                copy(
                    rucError = "Ingresa un RUC valido",
                    validationMessage = "Revisa los campos marcados en rojo para continuar.",
                )
            }
            return
        }
        val businessId = businessId
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = customerService.validateRUC(state.ruc, businessId)
                updateState {
                    copy(
                        rucCheckDigit = response.dv,
                        legalName = response.legalName,
                        name = response.legalName,
                        rucError = null,
                        validationMessage = null,
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
                updateState {
                    copy(
                        rucError = "No se pudo validar el RUC",
                        validationMessage = "Verifica el RUC e intentalo nuevamente.",
                    )
                }
            }
        }
    }

    private fun validateRucAndAdvance() {
        val state = uiState.value
        if (state.ruc.isBlank() || state.ruc.length < 5 || businessId == -1) {
            updateState {
                copy(
                    rucError = "Ingresa un RUC valido",
                    validationMessage = "Revisa los campos marcados en rojo para continuar.",
                )
            }
            return
        }
        val businessId = businessId
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = customerService.validateRUC(state.ruc, businessId)
                updateState {
                    copy(
                        rucCheckDigit = response.dv,
                        legalName = response.legalName,
                        name = response.legalName,
                        rucError = null,
                        validationMessage = null,
                        currentStep = AddCustomerStep.OPTIONAL_INFO,
                    )
                }
                showSuccess()
            } catch (e: Exception) {
                logger.sendLog(
                    Log(
                        level = LogLevel.ERROR,
                        flow = "AddClientViewModel::validateRucAndAdvance",
                        message = "Error validating RUC before advancing. Error: ${e.message ?: "UNKNOWN"}"
                    )
                )
                showError()
                updateState {
                    copy(
                        rucError = "No se pudo validar el RUC",
                        validationMessage = "Verifica el RUC e intentalo nuevamente.",
                    )
                }
            }
        }
    }

    fun createCustomer() {
        val state = uiState.value
        val normalizedCedula = normalizePanamaCedula(state.cfCedula.orEmpty())

        if (applyCreateFieldErrors(state)) {
            return
        }

        val cedulaError = getCedulaValidationError(
            customerType = state.customerType,
            cedula = normalizedCedula
        )
        if (cedulaError != null) {
            updateState {
                copy(
                    cfCedula = normalizedCedula,
                    cfCedulaError = cedulaError
                )
            }
            return
        }

        updateState {
            copy(
                cfCedula = normalizedCedula,
                cfCedulaError = null,
                taxRetentionPercentError = null,
                validationMessage = null,
                errorMessage = null,
            )
        }

        showLoading()
        viewModelScope.launch(Dispatchers.IO) {

            var foreignIdType: String? = null
            if (state.customerType == FeCustomerType.FOREIGNER) {
                foreignIdType = state.selectedForeignIdType.code
            }

            val retentionCode = CustomerTaxRetentionCatalog.normalizeCode(state.taxRetentionCode)
            val retentionCodeInt = retentionCode.toIntOrNull()
            val retentionPercent = resolveRetentionPercent(state)
            val isForeign = state.customerType == FeCustomerType.FOREIGNER
            val locationCode = if (isForeign) {
                null
            } else {
                PanamaLocations.codeFor(
                    state.selectedProvince,
                    state.selectedDistrict,
                    state.selectedCorreg
                )
            }

            val customer = Customer(
                id = -1,
                name = state.name,
                phone = state.phone.ifBlank { null },
                email = state.email.ifBlank { null },
                ruc = state.ruc.ifBlank { null },
                invoiceCustomer = state.invoicingEnabled,
                rucCheckDigit = state.rucCheckDigit,
                tags = state.tags.filter { it.isNotBlank() },
                customerType = state.customerType,
                taxPayerType = state.taxPayerType,
                addressLine = if (isForeign) null else state.addressLine?.ifBlank { null },
                province = if (isForeign) null else state.selectedProvince,
                district = if (isForeign) null else state.selectedDistrict,
                corregimiento = if (isForeign) null else state.selectedCorreg,
                foreignIdType = foreignIdType,
                foreignIdNumber = if (isForeign) state.foreignIdNumber?.ifBlank { null } else null,
                countryCode = state.selectedCountryCode,
                cedulaCF = if (state.customerType == FeCustomerType.FINAL_CONSUMER) {
                    normalizedCedula.ifBlank { null }
                } else {
                    null
                },
                locationCode = locationCode,
                taxExempt = state.taxExempt,
                taxRetentionCode = retentionCodeInt,
                taxRetentionPercent = retentionPercent,
            )

            try {

                val response = customerService.createCustomer(customer, businessId)
                if (response.id != -1L) {
                    withContext(Dispatchers.Main) {
                        showSuccess()
                    }

                    delay(600)
                    withContext(Dispatchers.Main) {
                        emitEvent(AddCustomerStateUiEvent.CustomerCreated)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showError()
                    }
                }
            } catch (e: InvalidRucException) {
                logger.sendLog(
                    Log(
                        level = LogLevel.WARNING,
                        flow = "AddClientViewModel::createCustomer",
                        message = "Invalid RUC number: ${e.message ?: "UNKNOWN"}"
                    )
                )
                updateState { copy(errorMessage = "El RUC ingresado no es valido. Verifica el numero e intentalo nuevamente.") }
                hideLoading()
            } catch (e: DuplicateCustomerException) {
                val duplicateMessage = duplicateCustomerMessage(state.name, state.ruc)
                logger.sendLog(
                    Log(
                        level = LogLevel.WARNING,
                        flow = "AddClientViewModel::createCustomer",
                        message = "Duplicate customer on create. businessId: $businessId, name: ${state.name}, ruc: ${state.ruc}"
                    )
                )
                updateState { copy(errorMessage = duplicateMessage) }
                hideLoading()
            } catch (e: Exception) {
                e.printStackTrace()
                logger.sendLog(
                    Log(
                        level = LogLevel.ERROR,
                        flow = "AddClientViewModel::createReducedCustomer",
                        message = "Error creating reduced customer. Error: ${e.message ?: "UNKNOWN"}"
                    )
                )
                updateState { copy(errorMessage = "No se pudo crear el cliente. Intentalo nuevamente.") }
                hideLoading()
            }
        }

    }

    fun clearErrorMessage() {
        updateState { copy(errorMessage = null) }
    }

    private fun getCedulaValidationError(customerType: FeCustomerType, cedula: String): String? {
        if (customerType != FeCustomerType.FINAL_CONSUMER || cedula.isBlank()) {
            return null
        }
        return if (isValidPanamaCedula(cedula)) {
            null
        } else {
            invalidCedulaMessage
        }
    }

    private fun duplicateCustomerMessage(name: String, ruc: String?): String {
        val formattedName = name.ifBlank { "-" }
        val formattedRuc = ruc?.ifBlank { "-" } ?: "-"
        return "Ya existe un cliente con nombre \"$formattedName\" y RUC \"$formattedRuc\"."
    }

    private fun resolveRetentionPercent(state: AddCustomerState): Int? {
        val normalizedCode = CustomerTaxRetentionCatalog.normalizeCode(state.taxRetentionCode)
        return when {
            normalizedCode.isEmpty() -> null
            normalizedCode == "8" -> state.taxRetentionPercent.toIntOrNull()
            else -> CustomerTaxRetentionCatalog.defaultRateForCode(normalizedCode)
        }
    }

    private fun applyCreateFieldErrors(state: AddCustomerState): Boolean {
        val nameError = if (state.name.isBlank()) "El nombre es requerido" else null
        val rucError = if (state.customerType.rucNeeded() && state.ruc.isBlank()) {
            "El RUC es requerido"
        } else {
            null
        }
        val foreignIdNumberError = if (state.customerType == FeCustomerType.FOREIGNER && state.foreignIdNumber.isNullOrBlank()) {
            "El documento es requerido"
        } else {
            null
        }
        val taxRetentionPercentError = if (
            CustomerTaxRetentionCatalog.normalizeCode(state.taxRetentionCode) == "8" &&
            state.taxRetentionPercent.toIntOrNull() == null
        ) {
            "Ingresa el porcentaje de retencion"
        } else {
            null
        }
        val provinceError = if (state.customerType != FeCustomerType.FOREIGNER && state.selectedProvince.isNullOrBlank()) {
            "Selecciona una provincia"
        } else {
            null
        }
        val districtError = if (state.customerType != FeCustomerType.FOREIGNER && state.selectedDistrict.isNullOrBlank()) {
            "Selecciona un distrito"
        } else {
            null
        }
        val corregimientoError = if (state.customerType != FeCustomerType.FOREIGNER && state.selectedCorreg.isNullOrBlank()) {
            "Selecciona un corregimiento"
        } else {
            null
        }
        val addressLineError = if (state.customerType != FeCustomerType.FOREIGNER && !hasMinimumAddressCharacters(state.addressLine)) {
            "La direccion debe tener al menos 5 caracteres no vacios"
        } else {
            null
        }

        updateState {
            copy(
                nameError = nameError,
                rucError = rucError,
                foreignIdNumberError = foreignIdNumberError,
                taxRetentionPercentError = taxRetentionPercentError,
                provinceError = provinceError,
                districtError = districtError,
                corregimientoError = corregimientoError,
                addressLineError = addressLineError,
                validationMessage = if (
                    listOf(
                        nameError,
                        rucError,
                        foreignIdNumberError,
                        taxRetentionPercentError,
                        provinceError,
                        districtError,
                        corregimientoError,
                        addressLineError
                    ).any { it != null }
                ) {
                    "Revisa los campos marcados en rojo para continuar."
                } else {
                    null
                },
            )
        }

        return listOf(
            nameError,
            rucError,
            foreignIdNumberError,
            taxRetentionPercentError,
            provinceError,
            districtError,
            corregimientoError,
            addressLineError
        ).any { it != null }
    }

}
