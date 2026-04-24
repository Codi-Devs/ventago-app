package com.teco.ventago.features.notifications.ui.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.notifications.domain.models.InAppNotification

data class NotificationsState(
    val items: List<InAppNotification> = emptyList(),
    val visibleItems: List<InAppNotification> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val total: Int = 0,
    val limit: Int = 10,
    val offset: Int = 0,
    val hasMore: Boolean = false,
    val snackbarMessage: String? = null,
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<NotificationsState> {
    override fun withLoading(state: LoadingBottomSheetState): NotificationsState {
        return copy(loadingBottomSheet = state)
    }
}
