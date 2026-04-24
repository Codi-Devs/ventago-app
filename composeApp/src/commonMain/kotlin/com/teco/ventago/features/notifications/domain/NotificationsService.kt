package com.teco.ventago.features.notifications.domain

import com.teco.ventago.core.LocalStorage
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.notifications.data.repository.INotificationsRepository
import com.teco.ventago.features.notifications.domain.models.InAppNotification
import com.teco.ventago.features.notifications.domain.models.NotificationsPage
import com.teco.ventago.features.notifications.domain.models.isVisibleInList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class NotificationsService(
    private val repository: INotificationsRepository,
    private val businessService: BusinessService,
    private val localStorage: LocalStorage,
    private val appScope: CoroutineScope,
    private val json: Json,
    private val authService: IAuthService,
    private val analyticsService: AnalyticsService,
) : INotificationsService {

    private val _items = MutableStateFlow<List<InAppNotification>>(emptyList())
    private val _unreadCount = MutableStateFlow(0)
    private val _loading = MutableStateFlow(false)
    private var currentBusinessId: Int = -1
    private var hasCompletedInitialRefresh = false

    override fun observeItems(): StateFlow<List<InAppNotification>> = _items.asStateFlow()
    override fun observeUnreadCount(): StateFlow<Int> = _unreadCount.asStateFlow()
    override fun observeLoading(): StateFlow<Boolean> = _loading.asStateFlow()
    override fun visibleItems(): List<InAppNotification> = _items.value.filter { it.isVisibleInList() }

    init {
        appScope.launch(Dispatchers.IO) {
            businessService.getBusiness().onEach { business ->
                val businessId = business?.businessId ?: -1
                if (businessId <= 0 || businessId == currentBusinessId) return@onEach
                currentBusinessId = businessId
                hasCompletedInitialRefresh = false
                restoreCachedPage(businessId)
            }.launchIn(this)
        }
    }

    override suspend fun refresh(limit: Int, offset: Int, append: Boolean): NotificationsPage {
        val businessId = currentBusinessId
        if (businessId <= 0) return NotificationsPage(limit = limit, offset = offset)

        _loading.value = true
        return try {
            val page = repository.listNotifications(businessId, limit, offset)
            val existingItems = _items.value
            val existingIds = existingItems.mapTo(mutableSetOf()) { it.id }
            val merged = when {
                append -> mergeById(existingItems, page.items)
                offset == 0 && existingItems.isNotEmpty() -> mergeById(existingItems, page.items)
                else -> mergeById(emptyList(), page.items)
            }
            _items.value = merged
            cachePage(businessId, page.copy(items = merged))
            _unreadCount.value = merged.count { !it.seen && !it.dismissed && !it.removed }

            val canEmitReceived = hasCompletedInitialRefresh || existingItems.isNotEmpty()
            if (canEmitReceived) {
                val user = authService.getUserSync()
                merged
                    .asSequence()
                    .filter { it.id !in existingIds }
                    .forEach { notification ->
                        analyticsService.logNotificationReceived(
                            businessId = businessId,
                            userId = user?.userId,
                            userEmail = user?.email,
                            notificationId = notification.id,
                            notificationType = notification.kind,
                        )
                    }
            }
            hasCompletedInitialRefresh = true
            page
        } finally {
            _loading.value = false
        }
    }

    override fun trackOpened(notification: InAppNotification) {
        analyticsService.logNotificationOpened(
            businessId = currentBusinessId.takeIf { it > 0 },
            userId = authService.getUserSync()?.userId,
            userEmail = authService.getUserSync()?.email,
            notificationId = notification.id,
            notificationType = notification.kind,
        )
    }

    override fun trackRead(notification: InAppNotification) {
        analyticsService.logNotificationRead(
            businessId = currentBusinessId.takeIf { it > 0 },
            userId = authService.getUserSync()?.userId,
            userEmail = authService.getUserSync()?.email,
            notificationId = notification.id,
            notificationType = notification.kind,
        )
    }

    override suspend fun refreshUnreadCount(): Int {
        val businessId = currentBusinessId
        if (businessId <= 0) return 0
        val unread = repository.unreadCount(businessId)
        _unreadCount.value = unread
        return unread
    }

    override suspend fun markSeen(notificationId: Long): Boolean {
        val businessId = currentBusinessId
        if (businessId <= 0) return false
        val success = repository.markSeen(businessId, notificationId)
        if (success) {
            _items.value = _items.value.map {
                if (it.id == notificationId) it.copy(seen = true) else it
            }
            _unreadCount.value = _items.value.count { !it.seen && !it.dismissed && !it.removed }
        }
        return success
    }

    override fun markSeenLocally(notificationId: Long) {
        _items.value = _items.value.map {
            if (it.id == notificationId) it.copy(seen = true) else it
        }
        _unreadCount.value = _items.value.count { !it.seen && !it.dismissed && !it.removed }
    }

    override suspend fun dismiss(notificationId: Long): Boolean {
        val businessId = currentBusinessId
        if (businessId <= 0) return false
        val currentNotification = _items.value.firstOrNull { it.id == notificationId }
        val success = repository.dismiss(businessId, notificationId)
        if (success) {
            _items.value = _items.value.map {
                if (it.id == notificationId) it.copy(dismissed = true) else it
            }
            _unreadCount.value = _items.value.count { !it.seen && !it.dismissed && !it.removed }
            analyticsService.logNotificationArchived(
                businessId = businessId,
                userId = authService.getUserSync()?.userId,
                userEmail = authService.getUserSync()?.email,
                notificationId = notificationId,
                notificationType = currentNotification?.kind,
            )
        }
        return success
    }

    override suspend fun remove(notificationId: Long): Boolean {
        val businessId = currentBusinessId
        if (businessId <= 0) return false
        val success = repository.remove(businessId, notificationId)
        if (success) {
            _items.value = _items.value.filterNot { it.id == notificationId }
            _unreadCount.value = _items.value.count { !it.seen && !it.dismissed && !it.removed }
        }
        return success
    }

    private fun mergeById(existing: List<InAppNotification>, incoming: List<InAppNotification>): List<InAppNotification> {
        val map = LinkedHashMap<Long, InAppNotification>()
        existing.forEach { map[it.id] = it }
        incoming.forEach { map[it.id] = it }
        return map.values.sortedByDescending { it.id }
    }

    private fun cachePage(businessId: Int, page: NotificationsPage) {
        val encoded = runCatching { json.encodeToString(NotificationsPage.serializer(), page) }.getOrNull() ?: return
        localStorage.set(cacheKey(businessId), encoded)
    }

    private fun restoreCachedPage(businessId: Int) {
        val encoded = localStorage.string(cacheKey(businessId)) ?: return
        val page = runCatching { json.decodeFromString(NotificationsPage.serializer(), encoded) }.getOrNull() ?: return
        val deduped = mergeById(emptyList(), page.items)
        _items.value = deduped
        _unreadCount.value = deduped.count { !it.seen && !it.dismissed && !it.removed }
    }

    private fun cacheKey(businessId: Int): String = "notifications_cache:$businessId"
}
