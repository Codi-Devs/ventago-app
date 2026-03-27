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
                ruc = "",
                rucCheckDigit = "",
                legalName = "",
                name = if (value.rucNeeded()) "" else name,
                cedulaCF = if (value == FeCustomerType.FINAL_CONSUMER) cedulaCF else "",
                cedulaError = null,
                foreignIdNumber = if (value == FeCustomerType.FOREIGNER) foreignIdNumber else "",
            )
        }
    }

    fun onNameChange(value: String) = updateState { copy(name = value) }
    fun onEmailChange(value: String) = updateState { copy(email = value) }
    fun onPhoneChange(value: String) = updateState { copy(phone = value) }
    fun onRucChange(value: String) = updateState { copy(ruc = value) }
    fun onCedulaChange(value: String) {
        val normalizedCedula = normalizePanamaCedula(value)
        updateState {
            copy(
                cedulaCF = normalizedCedula,
                cedulaError = getCedulaValidationError(
                    mode = mode,
                    customerType = customerType,
                    cedula = normalizedCedula
                )
            )
        }
    }
    fun onAddressLineChange(value: String) = updateState { copy(addressLine = value) }
    fun onForeignIdNumberChange(value: String) = updateState { copy(foreignIdNumber = value) }
    fun onTaxExemptChange(value: Boolean) = updateState { copy(taxExempt = value) }

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
        updateState { copy(taxRetentionPercent = normalized) }
    }

    fun selectedTaxRetentionRequiresManualPercent(): Boolean {
        return CustomerTaxRetentionCatalog.normalizeCode(uiState.value.taxRetentionCode) == "8"
    }

    fun onForeignIdTypeChange(value: CustomerForeignIdType) {
        updateState { copy(foreignIdType = value) }
    }

    fun onCountryCodeChange(value: String) {
        updateState { copy(selectedCountryCode = value) }
    }

    fun onProvinceChange(province: String) {
        val districts = PanamaLocations.districts(province)
        updateState {
            copy(
                selectedProvince = province,
                selectedDistrict = null,
                selectedCorregimiento = null,
                districtOptions = districts,
                corregimientoOptions = emptyList(),
            )
        }
    }

    fun onDistrictChange(district: String) {
        val corregimientos = PanamaLocations.corregimientos(uiState.value.selectedProvince, district)
        updateState {
            copy(
                selectedDistrict = district,
                selectedCorregimiento = null,
                corregimientoOptions = corregimientos,
            )
        }
    }

    fun onCorregimientoChange(corregimiento: String) {
        updateState { copy(selectedCorregimiento = corregimiento) }
    }

    fun validateRuc() {
        val current = uiState.value
        if (businessId <= 0 || current.ruc.isBlank() || !current.customerType.rucNeeded()) return

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
                    )
                }
                showSuccess()
            }.onFailure {
                showError()
            }
        }
    }

    fun saveCustomer() {
        if (businessId <= 0) return
        val current = uiState.value
        val normalizedCedula = normalizePanamaCedula(current.cedulaCF)

        if (current.mode == CustomerFormMode.CREATE && current.name.isBlank()) {
            viewModelScope.launch {
                emitEvent(CustomerFormUiEvent.ValidationError("El nombre es requerido"))
            }
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
                        val locationCode = if (current.customerType != FeCustomerType.FOREIGNER) {
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
                            addressLine = current.addressLine.ifBlank { null },
                            province = current.selectedProvince,
                            district = current.selectedDistrict,
                            corregimiento = current.selectedCorregimiento,
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
}
