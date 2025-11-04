package com.teco.ventago.features.auth.ui.login.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState

data class LoginState(
    val makingLogin: Boolean = false,
    val userDisabled: Boolean = false,
    val missingBusiness: Boolean = false, // Should be an event??
    val showPassword: Boolean = false,
    val invalidEmail: Boolean = false,
    val invalidPassword: Boolean = false,
    val email: String = "",
    val password: String = "",
    val forgotEmail: String = "",
    val forgotEmailInvalid: Boolean = false,
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<LoginState> {

    override fun withLoading(state: LoadingBottomSheetState): LoginState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class LoginUiEvent {
    data object MissingBusiness : LoginUiEvent()
    data object MakingLoginError : LoginUiEvent()
    data object MakingLoginSuccess : LoginUiEvent()
    data object GenericError : LoginUiEvent()
    data object TryLater : LoginUiEvent()
    data object CanceledGoogleLogin : LoginUiEvent()
    data object ShowRegisterDialog : LoginUiEvent()
}