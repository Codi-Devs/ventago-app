package com.teco.ventago.features.auth.ui.register.business.viewmodel

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentUiState

data class BusinessRegisterState(
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
    val userName: String = "",
    val name: String = "",
    val ruc: String = "",
    val businessEmail: String = "",
    val businessPhone: String = "",
    val businessWeb: String = "",
    val domain: String = "",
    val invalidName: Boolean = false,
    val invalidRuc: Boolean = false,
    val invalidWeb: Boolean = false,
    val invalidBusinessEmail: Boolean = false,
    val invalidBusinessPhone: Boolean = false,
) : LoadableState<BusinessRegisterState> {
    override fun withLoading(state: LoadingBottomSheetState): BusinessRegisterState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class BusinessRegisterUiEvent {

}