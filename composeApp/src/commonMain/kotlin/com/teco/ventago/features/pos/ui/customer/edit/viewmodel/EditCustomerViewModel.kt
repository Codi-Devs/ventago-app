package com.teco.ventago.features.pos.ui.customer.edit.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.location.PanamaLocations
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.customers.domain.CustomerService
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.invoicing.domain.models.FeCustomerType
import com.teco.ventago.utils.isValidPanamaCedula
import com.teco.ventago.utils.normalizePanamaCedula
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class EditCustomerViewModel(
    private val financialProfileService: FinancialProfileService,
    private val customerService: CustomerService,
    private val logger: ILoggerService
) : BaseViewModel<EditCustomerState, EditCustomerStateUiEvent>(EditCustomerState()) {

    private val invalidCedulaMessage =
        "Cedula invalida. Revise el formato (ej: 1-1234-12345, 8-88-8456, PE-123-12345, E-1234-12345, N-12345-1234, 1AV1234-12345, 1PI-1234-1234)."

    var businessId = -1

    init {
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

//        customerService.editingCustomer?.let {
//            updateState {
//                copy(
//                    customerId = it.id,
//                    email = it.email ?: "",
//                    tags = it.tags,
//                    addressLine = it.addressLine ?: "",
//                    selectedProvince = it.province,
//                    selectedDistrict = it.district,
//                    selectedCorreg = it.corregimiento,
//                    taxInfoIncomplete = !it.invoiceCustomer && financialProfileService.invoicingEnabled()
//                )
//            }
//        }
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
                districtOptions = districtOptions
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
            copy(selectedDistrict = district, corregOptions = corregOptions)
        }
//        enableButton()
    }

    fun onCorregimientoChange(corregimiento: String) {
        updateState {
            copy(selectedCorreg = corregimiento)
        }
//        enableButton()
    }

    fun onCustomerTypeChange(customerType: FeCustomerType) {
        updateState {
            copy(
                customerType = customerType,
                ruc = "",
                legalName = "",
                name = "",
                cfCedula = "",
                cfCedulaError = null,
            )
        }
    }

    fun onTaxIdChange(taxId: String) {
        updateState {
            copy(ruc = taxId)
        }
        if (uiState.value.customerType == FeCustomerType.CONTRIBUTING || uiState.value.customerType == FeCustomerType.GOVERNMENT) {
            updateState {
                copy(
                    legalName = "",
                    rucCheckDigit = "",
                    name = "",
                )
            }
        }
    }

    fun onNameChange(name: String) {
        updateState {
            copy(name = name)
        }
    }

    fun onPhoneChange(phone: String) {
        updateState {
            copy(phone = phone)
        }
    }

    fun onEmailChange(email: String) {
        updateState { copy(email = email) }
    }

    fun onAddressLineChange(addressLine: String) {
        updateState { copy(addressLine = addressLine) }
    }

    fun onCedulaChanges(cedula: String) {
        val normalizedCedula = normalizePanamaCedula(cedula)
        updateState {
            copy(
                cfCedula = normalizedCedula,
                cfCedulaError = getCedulaValidationError(
                    customerType = customerType,
                    cedula = normalizedCedula
                )
            )
        }
    }


    fun onTagsChange(tags: List<String>) {
        updateState { copy(tags = tags) }
    }

    fun validateRUC() {
        val state = uiState.value
        if (state.ruc.isBlank() || state.ruc.length < 5 || businessId == -1) {
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
}
