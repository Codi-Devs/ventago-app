package com.teco.ventago.utils

import androidx.compose.runtime.mutableStateOf

class ViewState {
    val isLoading = mutableStateOf(false)
    val isSuccess = mutableStateOf(false)
    val isError = mutableStateOf(false)

    fun setLoading() {
        isLoading.value = true
        isSuccess.value = false
        isError.value = false
    }

    fun setSuccess() {
        isLoading.value = false
        isSuccess.value = true
        isError.value = false
    }

    fun setError() {
        isLoading.value = false
        isSuccess.value = false
        isError.value = true
    }

    fun reset() {
        isLoading.value = false
        isSuccess.value = false
        isError.value = false
    }
}