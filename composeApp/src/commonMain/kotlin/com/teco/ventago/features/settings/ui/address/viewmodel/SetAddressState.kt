package com.teco.ventago.features.settings.ui.address.viewmodel

import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.business.domain.model.BusinessAddress

data class SetAddressState (
    val actualAddress: String = "",
    val newAddress: BusinessAddress? = null,
    val updateButtonEnabled: Boolean = false,
    val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) {

    val isAddressFilled: Boolean
        get() = actualAddress.isNotBlank() && actualAddress != "null"
}

sealed class SetAddressUiEvent {
    data object LaunchPlacesScreen: SetAddressUiEvent()
}