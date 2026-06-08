package com.teco.ventago.features.notifications.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.features.notifications.domain.INotificationsService
import com.teco.ventago.features.notifications.domain.NotificationActionResolution
import com.teco.ventago.features.notifications.domain.NotificationActionResolver
import com.teco.ventago.features.notifications.domain.models.InAppNotification
import com.teco.ventago.features.notifications.domain.models.NotificationDerivedState
import com.teco.ventago.features.notifications.domain.models.derivedState
import com.teco.ventago.features.notifications.domain.models.isVisibleInList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_PAGE_SIZE = 10

class NotificationsViewModel(
    private val notificationsService: INotificationsService,
    private val ioDispatcher: CoroutineDispatcher,
) : BaseViewModel<NotificationsState, NotificationsUiEvent>(NotificationsState()) {

    init {
        notificationsService.observeItems()
            .onEach { items ->
                val visibleItems = items.filter { it.isVisibleInList() }
                updateState {
                    copy(
                        items = items,
                        visibleItems = visibleItems,
                        hasMore = total > items.size
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onScreenVisible() {
        val showInitialLoading = uiState.value.items.isEmpty()
        refreshFirstPage(showInitialLoading)
    }

    fun refreshNotifications() {
        val state = uiState.value
        if (state.isInitialLoading || state.isLoadingMore || state.isRefreshing) return
        refreshFirstPage(showInitialLoading = false, showRefreshing = true)
    }

    fun loadMore() {
        val state = uiState.value
        if (state.isInitialLoading || state.isRefreshing || state.isLoadingMore || !state.hasMore) return

        updateState { copy(isLoadingMore = true) }
        viewModelScope.launch {
            try {
                val page = withContext(ioDispatcher) {
                    notificationsService.refresh(
                        limit = state.limit,
                        offset = state.items.size,
                        append = true
                    )
                }
                updatePageMetadata(page.total, page.limit, page.offset)
            } catch (_: Exception) {
                showMessage("No se pudieron cargar más notificaciones.")
            } finally {
                updateState { copy(isLoadingMore = false) }
            }
        }
    }

    fun onNotificationTapped(notification: InAppNotification) {
        notificationsService.trackOpened(notification)
        if (notification.derivedState() == NotificationDerivedState.UNREAD) {
            notificationsService.trackRead(notification)
            notificationsService.markSeenLocally(notification.id)
            viewModelScope.launch(ioDispatcher) {
                runCatching { notificationsService.markSeen(notification.id) }
                    .onFailure {
                        notificationsService.refreshUnreadCount()
                    }
            }
        }

        val action = NotificationActionResolver.resolve(notification.actionUrl)
        viewModelScope.launch {
            when (action) {
                NotificationActionResolution.None -> Unit
                is NotificationActionResolution.NavigateToAchPayment ->
                    emitEvent(NotificationsUiEvent.NavigateToAchPayment(action.paymentUid))

                is NotificationActionResolution.NavigateToOrderDetails ->
                    emitEvent(NotificationsUiEvent.NavigateToOrderDetails(action.orderNumber))

                is NotificationActionResolution.OpenExternalUrl ->
                    emitEvent(NotificationsUiEvent.OpenExternalUrl(action.url))

                is NotificationActionResolution.UnsupportedRelativeUrl ->
                    emitEvent(NotificationsUiEvent.ActionNotAvailable)
            }
        }
    }

    fun dismissNotification(notificationId: Long) {
        showLoading()
        viewModelScope.launch {
            try {
                val success = withContext(ioDispatcher) {
                    notificationsService.dismiss(notificationId)
                }
                if (success) {
                    withContext(ioDispatcher) {
                        notificationsService.refreshUnreadCount()
                    }
                    showSuccess()
                } else {
                    showError()
                }
            } catch (_: Exception) {
                showError()
            }
        }
    }

    fun dismissAllLoaded() {
        val ids = uiState.value.visibleItems.map { it.id }
        if (ids.isEmpty()) return

        showLoading()
        viewModelScope.launch {
            val allDismissed = withContext(ioDispatcher) {
                ids.all { id ->
                    runCatching { notificationsService.dismiss(id) }.getOrDefault(false)
                }
            }
            withContext(ioDispatcher) {
                runCatching { notificationsService.refreshUnreadCount() }
            }
            if (allDismissed) {
                showSuccess()
            } else {
                showError()
            }
        }
    }

    fun markAllLoadedAsRead() {
        val unreadNotifications = uiState.value.visibleItems
            .filter { it.derivedState() == NotificationDerivedState.UNREAD }
        if (unreadNotifications.isEmpty()) return

        unreadNotifications.forEach { notification ->
            notificationsService.trackRead(notification)
            notificationsService.markSeenLocally(notification.id)
        }

        showLoading()
        viewModelScope.launch {
            val allMarked = withContext(ioDispatcher) {
                unreadNotifications.all { notification ->
                    runCatching { notificationsService.markSeen(notification.id) }.getOrDefault(false)
                }
            }
            withContext(ioDispatcher) {
                runCatching { notificationsService.refreshUnreadCount() }
            }
            if (allMarked) {
                showSuccess()
            } else {
                showError()
            }
        }
    }

    fun clearSnackbarMessage() {
        updateState { copy(snackbarMessage = null) }
    }

    private fun refreshFirstPage(
        showInitialLoading: Boolean,
        showRefreshing: Boolean = false,
    ) {
        if (showInitialLoading) {
            updateState { copy(isInitialLoading = true) }
        }
        if (showRefreshing) {
            updateState { copy(isRefreshing = true) }
        }
        viewModelScope.launch {
            try {
                var page = withContext(ioDispatcher) {
                    notificationsService.refresh(
                        limit = DEFAULT_PAGE_SIZE,
                        offset = 0,
                        append = false
                    )
                }
                var loadedVisible = notificationsService.visibleItems().size
                var nextOffset = page.items.size

                // If the first payload leaves the user with <=1 visible item,
                // keep fetching the same 10-size batch contract until we fill the list
                // or the server reports no more data.
                if (loadedVisible <= 1) {
                    while (loadedVisible < DEFAULT_PAGE_SIZE && page.total > nextOffset) {
                        val nextPage = withContext(ioDispatcher) {
                            notificationsService.refresh(
                                limit = DEFAULT_PAGE_SIZE,
                                offset = nextOffset,
                                append = true
                            )
                        }
                        if (nextPage.items.isEmpty()) break
                        page = nextPage
                        nextOffset += nextPage.items.size
                        loadedVisible = notificationsService.visibleItems().size
                    }
                }

                withContext(ioDispatcher) {
                    runCatching { notificationsService.refreshUnreadCount() }
                }
                updatePageMetadata(
                    total = page.total,
                    limit = if (page.limit > 0) page.limit else DEFAULT_PAGE_SIZE,
                    offset = page.offset
                )
            } catch (_: Exception) {
                showMessage("No se pudieron cargar las notificaciones.")
            } finally {
                updateState {
                    copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false
                    )
                }
            }
        }
    }

    private fun updatePageMetadata(total: Int, limit: Int, offset: Int) {
        val loaded = uiState.value.items.size
        updateState {
            copy(
                total = total,
                limit = limit,
                offset = offset,
                hasMore = total > loaded
            )
        }
    }

    private fun showMessage(message: String) {
        updateState { copy(snackbarMessage = message) }
    }
}
