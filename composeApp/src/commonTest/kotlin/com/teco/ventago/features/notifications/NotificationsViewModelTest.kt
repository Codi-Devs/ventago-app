package com.teco.ventago.features.notifications

import com.teco.ventago.features.notifications.domain.INotificationsService
import com.teco.ventago.features.notifications.domain.models.InAppNotification
import com.teco.ventago.features.notifications.domain.models.NotificationDerivedState
import com.teco.ventago.features.notifications.domain.models.NotificationsPage
import com.teco.ventago.features.notifications.domain.models.derivedState
import com.teco.ventago.features.notifications.ui.viewmodel.NotificationsUiEvent
import com.teco.ventago.features.notifications.ui.viewmodel.NotificationsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun firstLoadAndLoadMoreUpdatePaginationState() = runTest(dispatcher) {
        val first = notification(id = 1, seen = false)
        val second = notification(id = 2, seen = true)
        val third = notification(id = 3, seen = false)

        val fakeService = FakeNotificationsService(
            pages = mapOf(
                0 to NotificationsPage(items = listOf(first, second), total = 3, limit = 2, offset = 0),
                2 to NotificationsPage(items = listOf(third), total = 3, limit = 2, offset = 2),
            )
        )
        val viewModel = NotificationsViewModel(fakeService, ioDispatcher = dispatcher)

        viewModel.onScreenVisible()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.visibleItems.size)
        assertTrue(viewModel.uiState.value.hasMore)

        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.visibleItems.size)
        assertFalse(viewModel.uiState.value.hasMore)
        assertEquals(listOf(0, 2), fakeService.refreshOffsets)
    }

    @Test
    fun dismissAndDismissAllHideVisibleItems() = runTest(dispatcher) {
        val first = notification(id = 11, seen = false)
        val second = notification(id = 22, seen = false)

        val fakeService = FakeNotificationsService(
            pages = mapOf(
                0 to NotificationsPage(items = listOf(first, second), total = 2, limit = 20, offset = 0)
            )
        )
        val viewModel = NotificationsViewModel(fakeService, ioDispatcher = dispatcher)

        viewModel.onScreenVisible()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.visibleItems.size)

        viewModel.dismissNotification(11)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.visibleItems.size)

        viewModel.dismissAllLoaded()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.visibleItems.isEmpty())
    }

    @Test
    fun tapUnreadNotificationMarksReadOptimisticallyAndEmitsNavigationEvent() = runTest(dispatcher) {
        val target = notification(
            id = 99,
            seen = false,
            actionUrl = "/payments/ach/index.html?payment_uid=pi_abc123"
        )
        val fakeService = FakeNotificationsService(
            pages = mapOf(0 to NotificationsPage(items = listOf(target), total = 1, limit = 20, offset = 0))
        )
        val viewModel = NotificationsViewModel(fakeService, ioDispatcher = dispatcher)

        viewModel.onScreenVisible()
        advanceUntilIdle()
        val eventDeferred = async { viewModel.events.first() }

        viewModel.onNotificationTapped(target)
        advanceUntilIdle()

        val event = eventDeferred.await()
        assertIs<NotificationsUiEvent.NavigateToAchPayment>(event)
        assertEquals("pi_abc123", event.paymentUid)
        assertEquals(1, fakeService.markSeenCalls)
        assertEquals(1, fakeService.markSeenLocalCalls)
        assertEquals(NotificationDerivedState.READ, viewModel.uiState.value.items.first().derivedState())
    }

    @Test
    fun markAllLoadedAsReadOnlyMarksUnreadItems() = runTest(dispatcher) {
        val unreadA = notification(id = 5, seen = false)
        val unreadB = notification(id = 6, seen = false)
        val alreadyRead = notification(id = 7, seen = true)
        val fakeService = FakeNotificationsService(
            pages = mapOf(
                0 to NotificationsPage(
                    items = listOf(unreadA, unreadB, alreadyRead),
                    total = 3,
                    limit = 20,
                    offset = 0
                )
            )
        )
        val viewModel = NotificationsViewModel(fakeService, ioDispatcher = dispatcher)

        viewModel.onScreenVisible()
        advanceUntilIdle()
        viewModel.markAllLoadedAsRead()
        advanceUntilIdle()

        assertEquals(listOf(5L, 6L), fakeService.markSeenIds.sorted())
        assertEquals(0, viewModel.uiState.value.visibleItems.count { !it.seen })
    }

    @Test
    fun firstLoadTopUpsWhenFirstPageHasOneVisibleItem() = runTest(dispatcher) {
        val hiddenDismissed = notification(id = 1, seen = true).copy(dismissed = true)
        val onlyVisibleInFirstPage = notification(id = 2, seen = false)
        val nextVisibleA = notification(id = 3, seen = false)
        val nextVisibleB = notification(id = 4, seen = true)
        val fakeService = FakeNotificationsService(
            pages = mapOf(
                0 to NotificationsPage(
                    items = listOf(hiddenDismissed, onlyVisibleInFirstPage),
                    total = 4,
                    limit = 10,
                    offset = 0
                ),
                2 to NotificationsPage(
                    items = listOf(nextVisibleA, nextVisibleB),
                    total = 4,
                    limit = 10,
                    offset = 2
                )
            )
        )
        val viewModel = NotificationsViewModel(fakeService, ioDispatcher = dispatcher)

        viewModel.onScreenVisible()
        advanceUntilIdle()

        assertEquals(listOf(0, 2), fakeService.refreshOffsets)
        assertEquals(3, viewModel.uiState.value.visibleItems.size)
        assertFalse(viewModel.uiState.value.hasMore)
    }

    private fun notification(
        id: Long,
        seen: Boolean,
        actionUrl: String? = null,
    ): InAppNotification {
        return InAppNotification(
            id = id,
            kind = "payment.success",
            title = "Title $id",
            message = "Message $id",
            actionUrl = actionUrl,
            seen = seen,
            createdAt = "2026-04-17T09:30:00-05:00",
            updatedAt = "2026-04-17T09:30:00-05:00"
        )
    }
}

private class FakeNotificationsService(
    private val pages: Map<Int, NotificationsPage>,
) : INotificationsService {
    private val itemsFlow = MutableStateFlow<List<InAppNotification>>(emptyList())
    private val unreadFlow = MutableStateFlow(0)
    private val loadingFlow = MutableStateFlow(false)

    val refreshOffsets = mutableListOf<Int>()
    var markSeenCalls = 0
    var markSeenLocalCalls = 0
    val markSeenIds = mutableListOf<Long>()

    override fun observeItems(): StateFlow<List<InAppNotification>> = itemsFlow.asStateFlow()

    override fun observeUnreadCount(): StateFlow<Int> = unreadFlow.asStateFlow()

    override fun observeLoading(): StateFlow<Boolean> = loadingFlow.asStateFlow()

    override fun visibleItems(): List<InAppNotification> {
        return itemsFlow.value.filter { !it.dismissed && !it.removed }
    }

    override suspend fun refresh(limit: Int, offset: Int, append: Boolean): NotificationsPage {
        refreshOffsets += offset
        val page = pages[offset] ?: NotificationsPage(
            items = emptyList(),
            total = itemsFlow.value.size,
            limit = limit,
            offset = offset
        )
        itemsFlow.value = if (append) merge(itemsFlow.value, page.items) else page.items
        recalculateUnread()
        return page
    }

    override suspend fun refreshUnreadCount(): Int {
        recalculateUnread()
        return unreadFlow.value
    }

    override fun trackOpened(notification: InAppNotification) = Unit

    override fun trackRead(notification: InAppNotification) = Unit

    override suspend fun markSeen(notificationId: Long): Boolean {
        markSeenCalls += 1
        markSeenIds += notificationId
        itemsFlow.value = itemsFlow.value.map {
            if (it.id == notificationId) it.copy(seen = true) else it
        }
        recalculateUnread()
        return true
    }

    override fun markSeenLocally(notificationId: Long) {
        markSeenLocalCalls += 1
        itemsFlow.value = itemsFlow.value.map {
            if (it.id == notificationId) it.copy(seen = true) else it
        }
        recalculateUnread()
    }

    override suspend fun dismiss(notificationId: Long): Boolean {
        itemsFlow.value = itemsFlow.value.map {
            if (it.id == notificationId) it.copy(dismissed = true) else it
        }
        recalculateUnread()
        return true
    }

    override suspend fun remove(notificationId: Long): Boolean {
        itemsFlow.value = itemsFlow.value.filterNot { it.id == notificationId }
        recalculateUnread()
        return true
    }

    private fun recalculateUnread() {
        unreadFlow.value = itemsFlow.value.count { !it.seen && !it.dismissed && !it.removed }
    }

    private fun merge(
        existing: List<InAppNotification>,
        incoming: List<InAppNotification>,
    ): List<InAppNotification> {
        val map = linkedMapOf<Long, InAppNotification>()
        existing.forEach { map[it.id] = it }
        incoming.forEach { map[it.id] = it }
        return map.values.sortedByDescending { it.id }
    }
}
