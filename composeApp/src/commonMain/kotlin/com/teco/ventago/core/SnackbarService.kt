package com.teco.ventago.core

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

class SnackbarService {
    var hostState: SnackbarHostState? = null

    suspend fun show(message: String) {
        hostState?.showSnackbar(message)
    }

    suspend fun showWithAction(message: String, actionLabel: String): SnackbarResult {
        return hostState?.showSnackbar(
            message = message,
            actionLabel = actionLabel
        ) ?: SnackbarResult.Dismissed
    }
}