package com.teco.ventago.features.pos.ui.customer.edit.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.core.location.PanamaLocations
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.invoicing.domain.TaxPayerType
import com.teco.ventago.features.invoicing.domain.models.FeCustomerType

data class EditCustomerState(
    val customerId: Int? = null,
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

    val foreignIdType: String? = null, // passport, identity_card, drivers_license, other
    val foreignIdNumber: String? = null,
    val cfCedula: String? = null,
    val cfCedulaError: String? = null,
    val countryCode: String = "PA", // ISO 3166-1 alpha-2 country code

    val invoicingEnabled: Boolean = false,

    val taxInfoIncomplete: Boolean = false,

    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<EditCustomerState> {
    override fun withLoading(state: LoadingBottomSheetState): EditCustomerState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class EditCustomerStateUiEvent {
    data object InvalidRucNumber : EditCustomerStateUiEvent()
    data object CustomerUpdated : EditCustomerStateUiEvent()
}
