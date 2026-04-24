package com.teco.ventago.features.notifications.domain

import com.teco.ventago.features.notifications.domain.models.InAppNotification
import com.teco.ventago.features.notifications.domain.models.NotificationsPage
import kotlinx.coroutines.flow.StateFlow

interface INotificationsService {
    fun observeItems(): StateFlow<List<InAppNotification>>
    fun observeUnreadCount(): StateFlow<Int>
    fun observeLoading(): StateFlow<Boolean>
    fun visibleItems(): List<InAppNotification>

    suspend fun refresh(limit: Int = 10, offset: Int = 0, append: Boolean = false): NotificationsPage
    suspend fun refreshUnreadCount(): Int
    fun trackOpened(notification: InAppNotification)
    fun trackRead(notification: InAppNotification)
    suspend fun markSeen(notificationId: Long): Boolean
    fun markSeenLocally(notificationId: Long)
    suspend fun dismiss(notificationId: Long): Boolean
    suspend fun remove(notificationId: Long): Boolean
}
