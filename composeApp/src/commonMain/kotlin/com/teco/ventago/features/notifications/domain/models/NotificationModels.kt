package com.teco.ventago.features.notifications.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class InAppNotification(
    @SerialName("id") val id: Long,
    @SerialName("kind") val kind: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("priority") val priority: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("action_type") val actionType: String? = null,
    @SerialName("action_url") val actionUrl: String? = null,
    @SerialName("metadata") val metadata: JsonObject = JsonObject(emptyMap()),
    @SerialName("seen") val seen: Boolean = false,
    @SerialName("dismissed") val dismissed: Boolean = false,
    @SerialName("removed") val removed: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class NotificationsPage(
    @SerialName("items") val items: List<InAppNotification> = emptyList(),
    @SerialName("total") val total: Int = 0,
    @SerialName("limit") val limit: Int = 20,
    @SerialName("offset") val offset: Int = 0,
)

@Serializable
data class NotificationsUnreadCount(
    @SerialName("unread_count") val unreadCount: Int = 0,
)

enum class NotificationDerivedState {
    UNREAD,
    READ,
    DISMISSED,
    REMOVED,
}

fun InAppNotification.derivedState(): NotificationDerivedState {
    return when {
        removed -> NotificationDerivedState.REMOVED
        dismissed -> NotificationDerivedState.DISMISSED
        seen -> NotificationDerivedState.READ
        else -> NotificationDerivedState.UNREAD
    }
}

fun InAppNotification.isVisibleInList(): Boolean {
    return derivedState() != NotificationDerivedState.DISMISSED &&
        derivedState() != NotificationDerivedState.REMOVED
}
