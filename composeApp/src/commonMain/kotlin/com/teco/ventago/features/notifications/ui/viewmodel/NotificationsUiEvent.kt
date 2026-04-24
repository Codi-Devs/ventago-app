package com.teco.ventago.features.notifications.ui.viewmodel

sealed class NotificationsUiEvent {
    data class NavigateToAchPayment(val paymentUid: String) : NotificationsUiEvent()
    data class OpenExternalUrl(val url: String) : NotificationsUiEvent()
    data object ActionNotAvailable : NotificationsUiEvent()
}
