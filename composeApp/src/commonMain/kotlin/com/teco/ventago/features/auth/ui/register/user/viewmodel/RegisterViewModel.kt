package com.teco.ventago.features.auth.ui.register.user.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.auth.data.provider.getGoogleAuthProvider
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.auth.domain.model.requests.CreateUserRequest
import com.teco.ventago.features.auth.ui.login.viewmodel.LoginUiEvent
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.AuthException
import com.teco.ventago.utils.emailRegex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterViewModel(
    private val service: IAuthService,
    private val analytics: AnalyticsService
) : BaseViewModel<RegisterState, RegisterUiEvent>(RegisterState()) {

    fun emailChanged(email: String) {
        updateState { copy(email = email) }
        if (isValidEmail()) {
            updateState { copy(invalidEmail = false) }
        }
    }

    fun passwordChanged(password: String) {
        updateState { copy(password = password) }
        if (isPasswordValid()) {
            updateState { copy(invalidPassword = false) }
        }
    }

    fun nameChanged(name: String) {
        updateState { copy(name = name) }
        if (isNameValid()) {
            updateState { copy(invalidName = false) }
        }
    }

    fun passwordVisible() {
        updateState { copy(showPassword = !this.showPassword) }
    }

    private fun isValidEmail(): Boolean {
        return emailRegex.matches(uiState.value.email)
    }

    private fun isPasswordValid(): Boolean {
        val pass = uiState.value.password
        if (pass.length < 8) {
            return false
        }

        if (pass.trim().length < 8) {
            return false
        }

        if (!pass.contains("[0-9]".toRegex())) {
            return false
        }

        return pass.contains("[a-z]".toRegex())
    }

    private fun isNameValid(): Boolean {
        val name = uiState.value.name
        if (name.length < 3) {
            return false
        }

        if (name.trim().length < 3) {
            return false
        }

        return true
    }

    fun launchGoogleLogin() {
        viewModelScope.launch {
            val googleUser = getGoogleAuthProvider().signIn()
            if (googleUser != null) {
                googleLogin(googleUser.idToken)
                // Handle the Google user login here, e.g., send to ViewModel or directly to the next screen
            } else {
                emitEvent(RegisterUiEvent.CanceledGoogleLogin)
            }
        }
    }


    fun emailRegister() {
        updateState {
            copy(
                invalidPassword = !isPasswordValid(),
                invalidEmail = !isValidEmail(),
                invalidName = !isNameValid()
            )
        }
        if (uiState.value.invalidEmail || uiState.value.invalidPassword || uiState.value.invalidName) {
            return
        }

        showLoading()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val request = CreateUserRequest(uiState.value.email, uiState.value.password, uiState.value.name)
                    val response = service.emailRegister(request)
                    if (response.uid.isBlank()) {
                        throw Exception("Error in Login")
                    }
                    withContext(Dispatchers.Main) {
                        showSuccess()
//                        CrashlyticsService.sendLog(CrashlyticsLog.ACCOUNT_CREATED)
                        if (response.missingBusiness) {
                            emitEvent(RegisterUiEvent.MissingBusiness)
                        } else {
                            emitEvent(RegisterUiEvent.MakingLoginSuccess)
                        }
                        analytics.setUserId(response.userId)
                        analytics.setDefaultEventParameters(analytics.defaultBundle(response))
                        analytics.logEvent("user_register", analytics.authBundle(response))
                    }
                } catch (e: AuthException) {
                    handleAuthException(e)
                    showError()
                } catch (_: Throwable) {
                    withContext(Dispatchers.Main) {
                        showError()
                        emitEvent(RegisterUiEvent.GenericError)
                    }
                }
            }
        }
    }

    fun googleLogin(idToken: String) {
        showLoading()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val response = service.googleLogin(idToken)
                    if (response.uid.isBlank()) {
                        throw Exception("Error in Login")
                    }

                    withContext(Dispatchers.Main) {
                        showSuccess()
//                        CrashlyticsService.sendLog(CrashlyticsLog.GOOGLE_SIGN_IN)
                        analytics.setUserId(response.userId)
                        analytics.setDefaultEventParameters(analytics.defaultBundle(response))
                        analytics.logEvent("user_login", analytics.authBundle(response, true))
                        if (response.missingBusiness) {
                            emitEvent(RegisterUiEvent.MissingBusiness)
                        } else {
                            emitEvent(RegisterUiEvent.MakingLoginSuccess)
                        }
                    }
                } catch (e: AuthException) {
                    handleAuthException(e)
                    showError()
                } catch (_: Throwable) {
                    withContext(Dispatchers.Main) {
                        emitEvent(RegisterUiEvent.GenericError)
                        showError()
//                        CrashlyticsService.sendLog(CrashlyticsLog.FAILED_GOOGLE_SIGN_IN)
                    }
                }
            }
        }
    }

    private suspend fun handleAuthException(e: AuthException) {
        when (e.error) {
            ApiError.F_AUTH_005 -> {
                withContext(Dispatchers.Main) {
                    emitEvent(RegisterUiEvent.TryLater)
                }
            }

            ApiError.F_AUTH_001 -> {
                withContext(Dispatchers.Main) {
                    emitEvent(RegisterUiEvent.MakingLoginError)
                }
            }

            ApiError.F_AUTH_004 -> {
                withContext(Dispatchers.Main) {
                    emitEvent(RegisterUiEvent.MakingLoginError)
                }
            }

            else -> {
                withContext(Dispatchers.Main) {
                    emitEvent(RegisterUiEvent.GenericError)
                }
            }
        }
    }


}