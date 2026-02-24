package com.teco.ventago.features.customers.ui.form.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.core.location.PanamaLocations
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.invoicing.domain.TaxPayerType
import com.teco.ventago.features.invoicing.domain.models.FeCustomerType

enum class CustomerFormMode {
    CREATE,
    EDIT
}

enum class CustomerForeignIdType(val code: String, val description: String) {
    PASSPORT("PASAPORTE", "Pasaporte"),
    OTHER("OTRO", "Otro")
}

data class CustomerCountryOption(
    val code: String,
    val name: String,
)

data class CustomerFormState(
    val mode: CustomerFormMode = CustomerFormMode.CREATE,
    val customerId: Long? = null,

    val customerType: FeCustomerType = FeCustomerType.FINAL_CONSUMER,
    val customerTypeOptions: List<FeCustomerType> = listOf(
        FeCustomerType.FINAL_CONSUMER,
        FeCustomerType.CONTRIBUTING,
        FeCustomerType.GOVERNMENT,
        FeCustomerType.FOREIGNER,
    ),

    val taxPayerType: TaxPayerType = TaxPayerType.NATURAL,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val ruc: String = "",
    val rucCheckDigit: String = "",
    val legalName: String = "",
    val cedulaCF: String = "",
    val foreignIdType: CustomerForeignIdType = CustomerForeignIdType.PASSPORT,
    val foreignIdNumber: String = "",

    val addressLine: String = "",
    val selectedProvince: String? = null,
    val selectedDistrict: String? = null,
    val selectedCorregimiento: String? = null,
    val selectedCountryCode: String = "PA",

    val provinceOptions: List<String> = PanamaLocations.provinces,
    val districtOptions: List<String> = emptyList(),
    val corregimientoOptions: List<String> = emptyList(),
    val countryOptions: List<CustomerCountryOption> = listOf(
        CustomerCountryOption("PA", "Panamá"),
        CustomerCountryOption("US", "Estados Unidos"),
        CustomerCountryOption("CO", "Colombia"),
        CustomerCountryOption("MX", "México"),
        CustomerCountryOption("ES", "España"),
    ),

    val invoicingEnabled: Boolean = false,
    val isFetching: Boolean = false,
    val errorMessage: String? = null,

    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<CustomerFormState> {
    val isEditMode: Boolean get() = mode == CustomerFormMode.EDIT

    override fun withLoading(state: LoadingBottomSheetState): CustomerFormState =
        copy(loadingBottomSheet = state)
}

sealed class CustomerFormUiEvent {
    data object Saved : CustomerFormUiEvent()
    data class ValidationError(val message: String) : CustomerFormUiEvent()
}
