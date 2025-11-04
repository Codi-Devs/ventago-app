package com.teco.ventago.features.pos.ui.customer.add.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.location.PanamaLocations
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.customers.domain.CustomerService
import com.teco.ventago.features.customers.domain.models.Customer
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.invoicing.domain.TaxPayerType
import com.teco.ventago.features.invoicing.domain.models.FeCustomerType
import com.teco.ventago.features.pos.domain.PosService
import com.teco.ventago.utils.InvalidRucException
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
        val countries = listOf(
            CountryOption("AF", "Afghanistan"),
            CountryOption("AL", "Albania"),
            CountryOption("DZ", "Algeria"),
            CountryOption("AS", "American Samoa"),
            CountryOption("AD", "Andorra"),
            CountryOption("AO", "Angola"),
            CountryOption("AR", "Argentina"),
            CountryOption("AM", "Armenia"),
            CountryOption("AW", "Aruba"),
            CountryOption("AU", "Australia"),
            CountryOption("AT", "Austria"),
            CountryOption("AZ", "Azerbaijan"),
            CountryOption("BS", "Bahamas"),
            CountryOption("BH", "Bahrain"),
            CountryOption("BD", "Bangladesh"),
            CountryOption("BB", "Barbados"),
            CountryOption("BY", "Belarus"),
            CountryOption("BE", "Belgium"),
            CountryOption("BZ", "Belize"),
            CountryOption("BJ", "Benin"),
            CountryOption("BM", "Bermuda"),
            CountryOption("BO", "Bolivia"),
            CountryOption("BR", "Brazil"),
            CountryOption("BG", "Bulgaria"),
            CountryOption("BF", "Burkina Faso"),
            CountryOption("BI", "Burundi"),
            CountryOption("KH", "Cambodia"),
            CountryOption("CM", "Cameroon"),
            CountryOption("CA", "Canada"),
            CountryOption("CL", "Chile"),
            CountryOption("CN", "China"),
            CountryOption("CO", "Colombia"),
            CountryOption("CR", "Costa Rica"),
            CountryOption("CU", "Cuba"),
            CountryOption("CY", "Cyprus"),
            CountryOption("CZ", "Czech Republic"),
            CountryOption("DK", "Denmark"),
            CountryOption("DO", "Dominican Republic"),
            CountryOption("EC", "Ecuador"),
            CountryOption("EG", "Egypt"),
            CountryOption("SV", "El Salvador"),
            CountryOption("EE", "Estonia"),
            CountryOption("ET", "Ethiopia"),
            CountryOption("FI", "Finland"),
            CountryOption("FR", "France"),
            CountryOption("DE", "Germany"),
            CountryOption("GR", "Greece"),
            CountryOption("GT", "Guatemala"),
            CountryOption("HN", "Honduras"),
            CountryOption("HK", "Hong Kong"),
            CountryOption("HU", "Hungary"),
            CountryOption("IS", "Iceland"),
            CountryOption("IN", "India"),
            CountryOption("ID", "Indonesia"),
            CountryOption("IE", "Ireland"),
            CountryOption("IL", "Israel"),
            CountryOption("IT", "Italy"),
            CountryOption("JM", "Jamaica"),
            CountryOption("JP", "Japan"),
            CountryOption("JO", "Jordan"),
            CountryOption("KE", "Kenya"),
            CountryOption("KR", "Korea, Republic of"),
            CountryOption("KW", "Kuwait"),
            CountryOption("LV", "Latvia"),
            CountryOption("LB", "Lebanon"),
            CountryOption("LT", "Lithuania"),
            CountryOption("LU", "Luxembourg"),
            CountryOption("MY", "Malaysia"),
            CountryOption("MT", "Malta"),
            CountryOption("MX", "Mexico"),
            CountryOption("MD", "Moldova"),
            CountryOption("MC", "Monaco"),
            CountryOption("MN", "Mongolia"),
            CountryOption("MA", "Morocco"),
            CountryOption("MZ", "Mozambique"),
            CountryOption("NP", "Nepal"),
            CountryOption("NL", "Netherlands"),
            CountryOption("NZ", "New Zealand"),
            CountryOption("NI", "Nicaragua"),
            CountryOption("NG", "Nigeria"),
            CountryOption("NO", "Norway"),
            CountryOption("OM", "Oman"),
            CountryOption("PK", "Pakistan"),
            CountryOption("PA", "Panama"),
            CountryOption("PY", "Paraguay"),
            CountryOption("PE", "Peru"),
            CountryOption("PH", "Philippines"),
            CountryOption("PL", "Poland"),
            CountryOption("PT", "Portugal"),
            CountryOption("QA", "Qatar"),
            CountryOption("RO", "Romania"),
            CountryOption("RU", "Russian Federation"),
            CountryOption("SA", "Saudi Arabia"),
            CountryOption("RS", "Serbia"),
            CountryOption("SG", "Singapore"),
            CountryOption("SK", "Slovakia"),
            CountryOption("SI", "Slovenia"),
            CountryOption("ZA", "South Africa"),
            CountryOption("ES", "Spain"),
            CountryOption("SE", "Sweden"),
            CountryOption("CH", "Switzerland"),
            CountryOption("TH", "Thailand"),
            CountryOption("TR", "Turkey"),
            CountryOption("UA", "Ukraine"),
            CountryOption("AE", "United Arab Emirates"),
            CountryOption("GB", "United Kingdom"),
            CountryOption("US", "United States"),
            CountryOption("UY", "Uruguay"),
            CountryOption("VE", "Venezuela"),
            CountryOption("VN", "Vietnam"),
            CountryOption("ZM", "Zambia"),
            CountryOption("ZW", "Zimbabwe")
        ).sortedBy { it.name }

        updateState { copy(countryOptions = countries) }
    }

    fun onCountrySelected(code: String) {
        updateState { copy(selectedCountryCode = code) }
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

    fun onForeignIdTypeSelected(type: ForeignIdType) {
        updateState { copy(selectedForeignIdType = type) }
    }

    fun onForeignIdNumberChange(value: String) {
        updateState { copy(foreignIdNumber = value) }
    }

    fun onCedulaChanges(cedula: String) {
        updateState {
            copy(cfCedula = cedula)
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

    fun createCustomer() {
        val state = uiState.value
        viewModelScope.launch(Dispatchers.IO) {

            var foreignIdType: String? = null
            if (state.customerType == FeCustomerType.FOREIGNER) {
                foreignIdType = state.selectedForeignIdType.code
            }

            val customer = Customer(
                id = -1,
                name = state.name,
                phone = state.phone,
                email = state.email,
                ruc = state.ruc,
                invoiceCustomer = state.invoicingEnabled,
                rucCheckDigit = state.rucCheckDigit,
                tags = state.tags,
                customerType = state.customerType,
                taxPayerType = state.taxPayerType,
                addressLine = state.addressLine,
                province = state.selectedProvince,
                district = state.selectedDistrict,
                corregimiento = state.selectedCorreg,
                foreignIdType = foreignIdType,
                foreignIdNumber = state.foreignIdNumber,
                countryCode = state.selectedCountryCode,
                cedulaCF = state.cfCedula,
                locationCode = PanamaLocations.codeFor(
                    state.selectedProvince,
                    state.selectedDistrict,
                    state.selectedCorreg
                )
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
                emitEvent(AddCustomerStateUiEvent.InvalidRucNumber)
            } catch (e: Exception) {
                e.printStackTrace()
                logger.sendLog(
                    Log(
                        level = LogLevel.ERROR,
                        flow = "AddClientViewModel::createReducedCustomer",
                        message = "Error creating reduced customer. Error: ${e.message ?: "UNKNOWN"}"
                    )
                )
                showError()
            }
        }

    }

}