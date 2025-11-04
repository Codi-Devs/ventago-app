package com.teco.ventago.features.auth.ui.login.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.auth.data.provider.getGoogleAuthProvider
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.auth.domain.model.requests.EmailLoginRequest
import com.teco.ventago.features.auth.ui.login.LoginScreen
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.AuthException
import com.teco.ventago.utils.emailRegex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(
    private val service: IAuthService,
    private val analytics: AnalyticsService,
) : BaseViewModel<LoginState, LoginUiEvent>(LoginState()) {

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

    fun passwordVisible() {
        updateState { copy(showPassword = !this.showPassword) }
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
                            emitEvent(LoginUiEvent.MissingBusiness)
                        } else {
                            emitEvent(LoginUiEvent.MakingLoginSuccess)
                        }
                    }
                } catch (e: AuthException) {
                    handleAuthException(e)
                    showError()
                } catch (_: Throwable) {
                    withContext(Dispatchers.Main) {
                        emitEvent(LoginUiEvent.GenericError)
                        showError()
//                        CrashlyticsService.sendLog(CrashlyticsLog.FAILED_GOOGLE_SIGN_IN)
                    }
                }
            }
        }
    }

    fun launchGoogleLogin() {
        viewModelScope.launch {
            val googleUser = getGoogleAuthProvider().signIn()
            if (googleUser != null) {
                googleLogin(googleUser.idToken)
                // Handle the Google user login here, e.g., send to ViewModel or directly to the next screen
            } else {
                emitEvent(LoginUiEvent.CanceledGoogleLogin)
            }
        }
    }

    fun emailLogin() {
        updateState {
            copy(
                invalidPassword = !isPasswordValid(),
                invalidEmail = !isValidEmail()
            )
        }
        if (uiState.value.invalidEmail || uiState.value.invalidPassword) {
            return
        }

        showLoading()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val request = EmailLoginRequest(uiState.value.email, uiState.value.password)
                    val response = service.emailLogin(request)
                    if (response.uid.isBlank()) {
                        throw Exception("Error in Login")
                    }
                    withContext(Dispatchers.Main) {
                        showSuccess()
                        if (response.missingBusiness) {
                            emitEvent(LoginUiEvent.MissingBusiness)
                        } else {
                            emitEvent(LoginUiEvent.MakingLoginSuccess)
                        }
//                        CrashlyticsService.sendLog(CrashlyticsLog.SIGN_IN)
                        analytics.setUserId(response.userId)
                        analytics.setDefaultEventParameters(analytics.defaultBundle(response))
                        analytics.logEvent("user_login", analytics.authBundle(response, false))
                    }
                } catch (e: AuthException) {
                    handleAuthException(e)
                    showError()
                } catch (_: Throwable) {
                    withContext(Dispatchers.Main) {
                        showError()
                        emitEvent(LoginUiEvent.GenericError)
                    }
                }
            }
        }
    }

    // Forgot Password Section
    fun forgotPassword() {
        updateState { copy(forgotEmailInvalid = !isValidEmail(true)) }

        if (uiState.value.forgotEmailInvalid) {
            return
        }

        try {
            showLoading()
            viewModelScope.launch(Dispatchers.IO) {
                delay(3000)
                service.sendPasswordResetEmail(uiState.value.forgotEmail)
                withContext(Dispatchers.Main) {
                    showSuccess()
                }
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                showError()
            }
        }

    }

    fun forgotEmailChanged(email: String) {
        updateState { copy(forgotEmail = email) }
        if (isValidEmail()) {
            updateState { copy(forgotEmailInvalid = false) }
        }
    }


    /*
     * Private Methods
     */
    private fun isValidEmail(forgotEmail: Boolean = false): Boolean {
        return if (forgotEmail) {
            emailRegex.matches(uiState.value.forgotEmail)
        } else {
            emailRegex.matches(uiState.value.email)
        }
    }

    private fun isPasswordValid(): Boolean {
        val pass = uiState.value.password
        if (pass.length < 3) {
            return false
        }
        return true
    }

    private suspend fun handleAuthException(e: AuthException) {
        when (e.error) {
            ApiError.F_AUTH_005 -> {
                withContext(Dispatchers.Main) {
                    emitEvent(LoginUiEvent.TryLater)
                }
            }

            ApiError.F_AUTH_001 -> {
                withContext(Dispatchers.Main) {
                    emitEvent(LoginUiEvent.MakingLoginError)
                }
            }

            ApiError.F_AUTH_002 -> {
                withContext(Dispatchers.Main) {
                    emitEvent(LoginUiEvent.ShowRegisterDialog)
                }
            }

            ApiError.F_AUTH_004 -> {
                withContext(Dispatchers.Main) {
                    emitEvent(LoginUiEvent.MakingLoginError)
                }
            }

            else -> {
                withContext(Dispatchers.Main) {
                    emitEvent(LoginUiEvent.GenericError)
                }
            }
        }
    }

}