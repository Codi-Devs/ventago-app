package com.teco.ventago.features.auth.ui.register.user.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState


data class RegisterState(
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
    val makingLogin: Boolean = false,
    val userDisabled: Boolean = false,
    val missingBusiness: Boolean = false, // Should be an event??
    val showPassword: Boolean = false,
    val invalidName: Boolean = false,
    val invalidEmail: Boolean = false,
    val invalidPassword: Boolean = false,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val forgotEmail: String = "",
    val forgotEmailInvalid: Boolean = false,
) : LoadableState<RegisterState> {
    override fun withLoading(state: LoadingBottomSheetState): RegisterState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class RegisterUiEvent {
    data object MissingBusiness : RegisterUiEvent()
    data object MakingLoginError : RegisterUiEvent()
    data object MakingLoginSuccess : RegisterUiEvent()
    data object GenericError : RegisterUiEvent()
    data object TryLater : RegisterUiEvent()
    data object CanceledGoogleLogin : RegisterUiEvent()
}