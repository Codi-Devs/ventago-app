package com.teco.ventago.features.customers.ui.form.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.location.PanamaLocations
import com.teco.ventago.features.customers.domain.CustomerService
import com.teco.ventago.features.customers.domain.models.Customer
import com.teco.ventago.features.customers.domain.models.CustomerTaxRetentionCatalog
import com.teco.ventago.features.customers.domain.models.UpdateCustomerDetailsRequest
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.invoicing.domain.models.FeCustomerType
import com.teco.ventago.features.invoicing.domain.models.rucNeeded
import com.teco.ventago.utils.DuplicateCustomerException
import com.teco.ventago.utils.isValidPanamaCedula
import com.teco.ventago.utils.normalizePanamaCedula
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomerFormViewModel(
    private val customerService: CustomerService,
    private val financialProfileService: FinancialProfileService,
) : BaseViewModel<CustomerFormState, CustomerFormUiEvent>(CustomerFormState()) {

    private val invalidCedulaMessage =
        "Cedula invalida. Revise el formato (ej: 1-1234-12345, 8-88-8456, PE-123-12345, E-1234-12345, N-12345-1234, 1AV1234-12345, 1PI-1234-1234)."

    private var businessId: Int = -1
    private var pendingEditCustomerId: Long? = null

    init {
        viewModelScope.launch {
            financialProfileService.observe().onEach { profile ->
                val newBusinessId = profile?.businessId ?: -1
                if (newBusinessId <= 0) return@onEach
                businessId = newBusinessId
                updateState {
                    copy(invoicingEnabled = profile?.invoicingActive == true)
                }
                pendingEditCustomerId?.let { customerId ->
                    if (uiState.value.mode == CustomerFormMode.EDIT && uiState.value.customerId == customerId && uiState.value.isFetching) {
                        loadCustomerForEdit(customerId)
                    }
                }
            }.launchIn(this)
        }
    }

    fun initCreate() {
        pendingEditCustomerId = null
        updateState {
            CustomerFormState(
                mode = CustomerFormMode.CREATE,
                invoicingEnabled = this.invoicingEnabled
            )
        }
    }

    fun loadForEdit(customerId: Long) {
        pendingEditCustomerId = customerId
        updateState {
            copy(
                mode = CustomerFormMode.EDIT,
                customerId = customerId,
                isFetching = true,
                errorMessage = null
            )
        }
        if (businessId <= 0) {
            return
        }
        loadCustomerForEdit(customerId)
    }

    private fun loadCustomerForEdit(customerId: Long) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    customerService.getCustomerById(businessId = businessId, customerId = customerId)
                }
            }.onSuccess { details ->
                val type = details.feCustomerType?.let { code ->
                    FeCustomerType.entries.firstOrNull { it.code == code }
                } ?: FeCustomerType.FINAL_CONSUMER

                val districtOptions = PanamaLocations.districts(details.province)
                val corregimientoOptions = PanamaLocations.corregimientos(details.province, details.district)

                updateState {
                    copy(
                        mode = CustomerFormMode.EDIT,
                        customerId = details.id,
                        customerType = type,
                        name = details.legalName.orEmpty(),
                        legalName = details.legalName.orEmpty(),
                        email = details.email.orEmpty(),
                        phone = details.phone1.orEmpty(),
                        ruc = details.rucNumber.orEmpty(),
                        rucCheckDigit = details.rucCheckDigit.orEmpty(),
                        cedulaCF = details.cedulaCf.orEmpty(),
                        cedulaError = null,
                        taxExempt = details.taxExempt,
                        taxRetentionCode = CustomerTaxRetentionCatalog.normalizeCode(details.taxRetentionCode),
                        taxRetentionPercent = details.taxRetentionPercent?.toString().orEmpty(),
                        addressLine = details.addressLine.orEmpty(),
                        selectedProvince = details.province,
                        selectedDistrict = details.district,
                        selectedCorregimiento = details.corregimiento,
                        selectedCountryCode = details.countryCode ?: "PA",
                        districtOptions = districtOptions,
                        corregimientoOptions = corregimientoOptions,
                        isFetching = false,
                        errorMessage = null,
                    )
                }
            }.onFailure {
                updateState {
                    copy(
                        isFetching = false,
                        errorMessage = it.message ?: "Failed to load customer"
                    )
                }
            }
        }
    }

    fun onCustomerTypeChange(value: FeCustomerType) {
        updateState {
            copy(
                customerType = value,
                customerTypeSelected = true,
                ruc = "",
                rucCheckDigit = "",
                rucError = null,
                legalName = "",
                name = "",
                nameError = null,
                cedulaCF = if (value == FeCustomerType.FINAL_CONSUMER) cedulaCF else "",
                cedulaError = null,
                foreignIdNumber = if (value == FeCustomerType.FOREIGNER) foreignIdNumber else "",
                foreignIdNumberError = null,
                foreignIdType = CustomerForeignIdType.PASSPORT,
                selectedCountryCode = DEFAULT_FOREIGN_CUSTOMER_COUNTRY,
                addressLine = if (value == FeCustomerType.FOREIGNER) "" else DEFAULT_CUSTOMER_FORM_ADDRESS_LINE,
                selectedProvince = if (value == FeCustomerType.FOREIGNER) null else DEFAULT_CUSTOMER_FORM_PROVINCE,
                selectedDistrict = if (value == FeCustomerType.FOREIGNER) null else DEFAULT_CUSTOMER_FORM_DISTRICT,
                selectedCorregimiento = if (value == FeCustomerType.FOREIGNER) null else DEFAULT_CUSTOMER_FORM_CORREGIMIENTO,
                districtOptions = if (value == FeCustomerType.FOREIGNER) {
                    emptyList()
                } else {
                    PanamaLocations.districts(DEFAULT_CUSTOMER_FORM_PROVINCE)
                },
                corregimientoOptions = if (value == FeCustomerType.FOREIGNER) {
                    emptyList()
                } else {
                    PanamaLocations.corregimientos(DEFAULT_CUSTOMER_FORM_PROVINCE, DEFAULT_CUSTOMER_FORM_DISTRICT)
                },
                addressExpanded = false,
                addressLineError = null,
                provinceError = null,
                districtError = null,
                corregimientoError = null,
                validationMessage = null,
            )
        }
    }

    fun onCustomerTypeSelected(value: FeCustomerType) {
        onCustomerTypeChange(value)
        updateState { copy(currentStep = CustomerFormStep.MAIN_INFO) }
    }

    fun onStepBack() {
        updateState {
            copy(
                currentStep = when (currentStep) {
                    CustomerFormStep.TYPE -> CustomerFormStep.TYPE
                    CustomerFormStep.MAIN_INFO -> CustomerFormStep.TYPE
                    CustomerFormStep.OPTIONAL_INFO -> CustomerFormStep.MAIN_INFO
                },
                validationMessage = null,
            )
        }
    }

    fun toggleAddressExpanded() {
        updateState { copy(addressExpanded = !addressExpanded) }
    }

    fun goToOptionalInfo() {
        val current = uiState.value
        val nameError = if (current.name.isBlank() && !current.customerType.rucNeeded()) {
            "El nombre es requerido"
        } else {
            null
        }
        val rucError = if (current.customerType.rucNeeded() && current.ruc.isBlank()) {
            "El RUC es requerido"
        } else {
            null
        }
        val foreignIdNumberError = if (current.customerType == FeCustomerType.FOREIGNER && current.foreignIdNumber.isBlank()) {
            "El documento es requerido"
        } else {
            null
        }
        val normalizedCedula = normalizePanamaCedula(current.cedulaCF)
        val cedulaError = getCedulaValidationError(
            mode = current.mode,
            customerType = current.customerType,
            cedula = normalizedCedula
        )

        if (listOf(nameError, rucError, foreignIdNumberError, cedulaError).any { it != null }) {
            updateState {
                copy(
                    nameError = nameError,
                    rucError = rucError,
                    foreignIdNumberError = foreignIdNumberError,
                    cedulaCF = normalizedCedula,
                    cedulaError = cedulaError,
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
                cedulaCF = normalizedCedula,
                cedulaError = null,
                validationMessage = null,
            )
        }

        if (current.customerType.rucNeeded() && (current.legalName.isBlank() || current.rucCheckDigit.isBlank())) {
            validateRucAndAdvance()
            return
        }

        updateState { copy(currentStep = CustomerFormStep.OPTIONAL_INFO) }
    }

    fun onNameChange(value: String) = updateState { copy(name = value, nameError = null, validationMessage = null) }
    fun onEmailChange(value: String) = updateState { copy(email = value, validationMessage = null) }
    fun onPhoneChange(value: String) = updateState { copy(phone = value, validationMessage = null) }
    fun onRucChange(value: String) = updateState {
        copy(
            ruc = value.uppercase(),
            rucError = null,
            rucCheckDigit = "",
            legalName = "",
            name = if (customerType.rucNeeded()) "" else name,
            validationMessage = null,
        )
    }
    fun onCedulaChange(value: String) {
        val normalizedCedula = normalizePanamaCedula(value)
        updateState {
            copy(
                cedulaCF = normalizedCedula,
                cedulaError = getCedulaValidationError(
                    mode = mode,
                    customerType = customerType,
                    cedula = normalizedCedula
                ),
                validationMessage = null,
            )
        }
    }
    fun onAddressLineChange(value: String) = updateState { copy(addressLine = value, addressLineError = null, validationMessage = null) }
    fun onForeignIdNumberChange(value: String) = updateState {
        copy(foreignIdNumber = value, foreignIdNumberError = null, validationMessage = null)
    }
    fun onTaxExemptChange(value: Boolean) = updateState { copy(taxExempt = value, validationMessage = null) }

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
        updateState { copy(taxRetentionPercent = normalized, taxRetentionPercentError = null, validationMessage = null) }
    }

    fun selectedTaxRetentionRequiresManualPercent(): Boolean {
        return CustomerTaxRetentionCatalog.normalizeCode(uiState.value.taxRetentionCode) == "8"
    }

    fun onForeignIdTypeChange(value: CustomerForeignIdType) {
        updateState { copy(foreignIdType = value, validationMessage = null) }
    }

    fun onCountryCodeChange(value: String) {
        updateState { copy(selectedCountryCode = value, validationMessage = null) }
    }

    fun onProvinceChange(province: String) {
        val districts = PanamaLocations.districts(province)
        updateState {
            copy(
                selectedProvince = province,
                provinceError = null,
                districtError = null,
                corregimientoError = null,
                selectedDistrict = null,
                selectedCorregimiento = null,
                districtOptions = districts,
                corregimientoOptions = emptyList(),
                validationMessage = null,
            )
        }
    }

    fun onDistrictChange(district: String) {
        val corregimientos = PanamaLocations.corregimientos(uiState.value.selectedProvince, district)
        updateState {
            copy(
                selectedDistrict = district,
                districtError = null,
                corregimientoError = null,
                selectedCorregimiento = null,
                corregimientoOptions = corregimientos,
                validationMessage = null,
            )
        }
    }

    fun onCorregimientoChange(corregimiento: String) {
        updateState { copy(selectedCorregimiento = corregimiento, corregimientoError = null, validationMessage = null) }
    }

    fun validateRuc() {
        val current = uiState.value
        if (businessId <= 0 || current.ruc.isBlank() || !current.customerType.rucNeeded()) {
            updateState {
                copy(
                    rucError = "Ingresa un RUC valido",
                    validationMessage = "Revisa los campos marcados en rojo para continuar.",
                )
            }
            return
        }

        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    customerService.validateRUC(current.ruc, businessId)
                }
            }.onSuccess { validated ->
                updateState {
                    copy(
                        rucCheckDigit = validated.dv,
                        legalName = validated.legalName,
                        name = validated.legalName,
                        rucError = null,
                        validationMessage = null,
                    )
                }
                showSuccess()
            }.onFailure {
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
        val current = uiState.value
        if (businessId <= 0 || current.ruc.isBlank() || !current.customerType.rucNeeded()) {
            updateState {
                copy(
                    rucError = "Ingresa un RUC valido",
                    validationMessage = "Revisa los campos marcados en rojo para continuar.",
                )
            }
            return
        }

        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    customerService.validateRUC(current.ruc, businessId)
                }
            }.onSuccess { validated ->
                updateState {
                    copy(
                        rucCheckDigit = validated.dv,
                        legalName = validated.legalName,
                        name = validated.legalName,
                        rucError = null,
                        validationMessage = null,
                        currentStep = CustomerFormStep.OPTIONAL_INFO,
                    )
                }
                showSuccess()
            }.onFailure {
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

    fun saveCustomer() {
        if (businessId <= 0) return
        val current = uiState.value
        if (current.mode == CustomerFormMode.CREATE && !current.customerTypeSelected) {
            updateState { copy(validationMessage = "Selecciona un tipo de cliente para continuar.") }
            return
        }
        val normalizedCedula = normalizePanamaCedula(current.cedulaCF)

        if (current.mode == CustomerFormMode.CREATE && applyCreateFieldErrors(current)) {
            return
        }

        val cedulaError = getCedulaValidationError(
            mode = current.mode,
            customerType = current.customerType,
            cedula = normalizedCedula
        )
        if (cedulaError != null) {
            updateState {
                copy(
                    cedulaCF = normalizedCedula,
                    cedulaError = cedulaError
                )
            }
            viewModelScope.launch {
                emitEvent(CustomerFormUiEvent.ValidationError(cedulaError))
            }
            return
        }

        updateState {
            copy(
                cedulaCF = normalizedCedula,
                cedulaError = null,
                errorMessage = null,
            )
        }

        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    val retentionCode = CustomerTaxRetentionCatalog.normalizeCode(current.taxRetentionCode)
                        .toIntOrNull()
                    val retentionPercent = resolveRetentionPercent(current)
                    if (current.isEditMode) {
                        val locationCode = if (current.customerType != FeCustomerType.FOREIGNER) {
                            PanamaLocations.codeFor(
                                current.selectedProvince,
                                current.selectedDistrict,
                                current.selectedCorregimiento
                            )
                        } else {
                            null
                        }

                        customerService.updateCustomerDetails(
                            businessId = businessId,
                            customerId = requireNotNull(current.customerId),
                            request = UpdateCustomerDetailsRequest(
                                email = current.email.ifBlank { null },
                                phone1 = current.phone.ifBlank { null },
                                addressLine = current.addressLine.ifBlank { null },
                                locationCode = locationCode,
                                taxExempt = current.taxExempt,
                                taxRetentionCode = retentionCode,
                                taxRetentionPercent = retentionPercent,
                            )
                        )
                    } else {
                        val isForeign = current.customerType == FeCustomerType.FOREIGNER
                        val locationCode = if (!isForeign) {
                            PanamaLocations.codeFor(
                                current.selectedProvince,
                                current.selectedDistrict,
                                current.selectedCorregimiento
                            )
                        } else {
                            null
                        }

                        val customer = Customer(
                            id = -1,
                            name = current.name,
                            phone = current.phone.ifBlank { null },
                            email = current.email.ifBlank { null },
                            ruc = current.ruc.ifBlank { null },
                            invoiceCustomer = current.invoicingEnabled,
                            rucCheckDigit = current.rucCheckDigit.ifBlank { null },
                            tags = emptyList(),
                            customerType = current.customerType,
                            taxPayerType = current.taxPayerType,
                            addressLine = if (isForeign) null else current.addressLine.ifBlank { null },
                            province = if (isForeign) null else current.selectedProvince,
                            district = if (isForeign) null else current.selectedDistrict,
                            corregimiento = if (isForeign) null else current.selectedCorregimiento,
                            locationCode = locationCode,
                            foreignIdType = if (current.customerType == FeCustomerType.FOREIGNER) {
                                current.foreignIdType.code
                            } else {
                                null
                            },
                            foreignIdNumber = if (current.customerType == FeCustomerType.FOREIGNER) {
                                current.foreignIdNumber.ifBlank { null }
                            } else {
                                null
                            },
                            cedulaCF = if (current.customerType == FeCustomerType.FINAL_CONSUMER) {
                                normalizedCedula.ifBlank { null }
                            } else {
                                null
                            },
                            countryCode = current.selectedCountryCode,
                            taxExempt = current.taxExempt,
                            taxRetentionCode = retentionCode,
                            taxRetentionPercent = retentionPercent,
                        )

                        customerService.createCustomer(
                            customer = customer,
                            businessId = businessId,
                        )
                    }
                }
            }.onSuccess {
                showSuccess()
                delay(700)
                emitEvent(CustomerFormUiEvent.Saved)
            }.onFailure { throwable ->
                if (throwable is DuplicateCustomerException) {
                    updateState {
                        copy(errorMessage = duplicateCustomerMessage(current.name, current.ruc))
                    }
                    hideLoading()
                    return@onFailure
                }
                showError()
            }
        }
    }

    fun clearErrorMessage() {
        updateState { copy(errorMessage = null) }
    }

    private fun duplicateCustomerMessage(name: String, ruc: String): String {
        val formattedName = name.ifBlank { "-" }
        val formattedRuc = ruc.ifBlank { "-" }
        return "Ya existe un cliente con nombre \"$formattedName\" y RUC \"$formattedRuc\"."
    }

    private fun resolveRetentionPercent(state: CustomerFormState): Int? {
        val normalizedCode = CustomerTaxRetentionCatalog.normalizeCode(state.taxRetentionCode)
        return when {
            normalizedCode.isEmpty() -> null
            normalizedCode == "8" -> state.taxRetentionPercent.toIntOrNull()
            else -> CustomerTaxRetentionCatalog.defaultRateForCode(normalizedCode)
        }
    }

    private fun getCedulaValidationError(
        mode: CustomerFormMode,
        customerType: FeCustomerType,
        cedula: String
    ): String? {
        if (mode != CustomerFormMode.CREATE || customerType != FeCustomerType.FINAL_CONSUMER || cedula.isBlank()) {
            return null
        }
        return if (isValidPanamaCedula(cedula)) {
            null
        } else {
            invalidCedulaMessage
        }
    }

    private fun applyCreateFieldErrors(state: CustomerFormState): Boolean {
        val nameError = if (state.name.isBlank()) "El nombre es requerido" else null
        val rucError = if (state.customerType.rucNeeded() && state.ruc.isBlank()) {
            "El RUC es requerido"
        } else {
            null
        }
        val foreignIdNumberError = if (state.customerType == FeCustomerType.FOREIGNER && state.foreignIdNumber.isBlank()) {
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
        val corregimientoError = if (state.customerType != FeCustomerType.FOREIGNER && state.selectedCorregimiento.isNullOrBlank()) {
            "Selecciona un corregimiento"
        } else {
            null
        }
        val addressLineError = if (state.customerType != FeCustomerType.FOREIGNER && state.addressLine.isBlank()) {
            "La direccion es requerida"
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
