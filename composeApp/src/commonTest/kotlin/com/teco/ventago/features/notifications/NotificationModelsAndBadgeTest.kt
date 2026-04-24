package com.teco.ventago.features.notifications

import com.teco.ventago.features.notifications.domain.formatUnreadBadge
import com.teco.ventago.features.notifications.domain.models.InAppNotification
import com.teco.ventago.features.notifications.domain.models.NotificationDerivedState
import com.teco.ventago.features.notifications.domain.models.derivedState
import com.teco.ventago.features.notifications.domain.models.isVisibleInList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationModelsAndBadgeTest {

    @Test
    fun derivedStateRespectsRemovedDismissedSeenPrecedence() {
        val unread = baseNotification()
        val read = baseNotification(seen = true)
        val dismissed = baseNotification(seen = true, dismissed = true)
        val removed = baseNotification(seen = true, dismissed = true, removed = true)

        assertEquals(NotificationDerivedState.UNREAD, unread.derivedState())
        assertEquals(NotificationDerivedState.READ, read.derivedState())
        assertEquals(NotificationDerivedState.DISMISSED, dismissed.derivedState())
        assertEquals(NotificationDerivedState.REMOVED, removed.derivedState())
    }

    @Test
    fun visibleInListHidesDismissedAndRemovedItems() {
        assertTrue(baseNotification().isVisibleInList())
        assertFalse(baseNotification(dismissed = true).isVisibleInList())
        assertFalse(baseNotification(removed = true).isVisibleInList())
    }

    @Test
    fun unreadBadgeFormattingCapsAtNinetyNinePlus() {
        assertNull(formatUnreadBadge(0))
        assertEquals("1", formatUnreadBadge(1))
        assertEquals("99", formatUnreadBadge(99))
        assertEquals("99+", formatUnreadBadge(100))
    }

    private fun baseNotification(
        seen: Boolean = false,
        dismissed: Boolean = false,
        removed: Boolean = false,
    ): InAppNotification {
        return InAppNotification(
            id = 10,
            kind = "payment.success",
            title = "Pago recibido",
            message = "Tu pago fue aprobado.",
            seen = seen,
            dismissed = dismissed,
            removed = removed,
            createdAt = "2026-04-17T09:30:00-05:00",
            updatedAt = "2026-04-17T09:30:00-05:00"
        )
    }
}
