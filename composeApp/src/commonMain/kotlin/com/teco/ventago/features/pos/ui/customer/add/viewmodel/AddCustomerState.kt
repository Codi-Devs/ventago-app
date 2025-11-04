package com.teco.ventago.features.pos.ui.customer.add.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.core.location.PanamaLocations
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.invoicing.domain.models.FeCustomerType
import com.teco.ventago.features.invoicing.domain.TaxPayerType


data class CountryOption(
    val code: String, // e.g. "PA"
    val name: String  // e.g. "Panama"
)

enum class ForeignIdType(val code: String, val description: String) {
    PASSPORT("passport", "Passport"),
    FOREIGN_TAX_ID("foreing_taxid", "Foreign Tax ID");

    companion object {
        fun fromCode(code: String?): ForeignIdType? =
            entries.find { it.code == code }
    }
}

data class AddCustomerState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val ruc: String = "",
    val tags: List<String> = emptyList(),

    val customerType: FeCustomerType = FeCustomerType.FINAL_CONSUMER, // individual or business
    val taxPayerType: TaxPayerType = TaxPayerType.NATURAL, // final_consumer, registered_taxpayer, exempted_taxpayer
    val rucCheckDigit: String? = null,
    val legalName: String? = null,
    val addressLine: String? = null,

    val selectedProvince: String? = null,
    val selectedDistrict: String? = null,
    val selectedCorreg: String? = null,
    val provinceOptions: List<String> = PanamaLocations.provinces,
    val districtOptions: List<String> = emptyList(),
    val corregOptions: List<String> = emptyList(),

    val customerTypeOptions: List<FeCustomerType> = listOf(
        FeCustomerType.FINAL_CONSUMER,
        FeCustomerType.CONTRIBUTING,
        FeCustomerType.GOVERNMENT,
        FeCustomerType.FOREIGNER,
    ),

    val foreignIdTypeOptions: List<ForeignIdType> = listOf(ForeignIdType.PASSPORT, ForeignIdType.FOREIGN_TAX_ID),
    val selectedForeignIdType: ForeignIdType = ForeignIdType.PASSPORT,
    val foreignIdNumber: String? = null,
    val countryCode: String = "PA", // ISO 3166-1 alpha-2 country code

    val cfCedula: String? = null,

    val countryOptions: List<CountryOption> = emptyList(),
    val selectedCountryCode: String = "PA", // default Panama

    val invoicingEnabled: Boolean = false,
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<AddCustomerState> {
    override fun withLoading(state: LoadingBottomSheetState): AddCustomerState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class AddCustomerStateUiEvent {
    data object InvalidRucNumber : AddCustomerStateUiEvent()
    data object CustomerCreated : AddCustomerStateUiEvent()
}