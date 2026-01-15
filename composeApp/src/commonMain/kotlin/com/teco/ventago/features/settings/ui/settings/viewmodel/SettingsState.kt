package com.teco.ventago.features.settings.ui.settings.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import com.teco.ventago.core.LoadableState
import com.teco.ventago.core.camera.SharedImage
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.business.domain.model.BusinessAddress
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentMethodItem

data class SettingsState(
    // Logo state
    val imgUrl: String? = null,

    val sharedImage: SharedImage? = null,
    val imageBitmap: ImageBitmap? = null,
    val showUploadImageSheet: Boolean = false,
    val showPermissionRationalDialog: Boolean = false,

    // name state
    val actualName: String = "",
    val newName: String = "",

    // phone state
    val actualPhone: String = "",
    val newPhone: String = "",

    // businessEmail state
    val actualEmail: String = "",
    val newEmail: String = "",

    // RUC state
    val actualRuc: String = "",
    val newRuc: String = "",

    // Web state
    val actualWeb: String = "",
    val newWeb: String = "",

    // Address state
    val actualAddress: String = "",
    val newAddress: BusinessAddress? = null,

    // Payment methods
    val availablePaymentMethods: Map<String, PaymentMethodItem> = emptyMap(),
    val loadingPaymentMethods: Boolean = true,

    // User info
    val isVerified: Boolean = true,
    val canChangePassword: Boolean = false,
    val signOut: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,

    val invoicingEnabled: Boolean = false,

    // Quotes
    val hasQuotesAccess: Boolean = false,
    val defaultQuoteAdditionalInfo: String = "",
    val defaultQuoteStyle: String = "style1",
    val defaultQuoteIncludePaymentButton: Boolean = false,

    // Loading state
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<SettingsState> {

    override fun withLoading(state: LoadingBottomSheetState): SettingsState {
        return copy(loadingBottomSheet = state)
    }

//    val canChangePassword = mutableStateOf(false)
//    val showDeleteAccountDialog = mutableStateOf(false)
//    val signOut = mutableStateOf(false)
//    val isVerified = mutableStateOf(true)


    val isNameFilled: Boolean
        get() = newName.isNotBlank() && newName != "null"


    val isAddressFilled: Boolean
        get() = actualAddress.isNotBlank() && actualAddress != "null"

    val isPhoneFilled: Boolean
        get() = newPhone.isNotBlank() && newPhone != "null"

//    val isWAPhoneFilled: Boolean
//        get() = newWAPhone.isNotBlank() && newWAPhone != "null"
}

sealed class SettingsStateUiEvent {
    data object NoAddressSelected: SettingsStateUiEvent()
    data object LaunchCamera: SettingsStateUiEvent()
    data object LaunchGallery: SettingsStateUiEvent()
    data object LaunchSettings: SettingsStateUiEvent()
}
