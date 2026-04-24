package com.teco.ventago.features.notifications.domain

fun formatUnreadBadge(count: Int): String? {
    if (count <= 0) return null
    if (count > 99) return "99+"
    return count.toString()
}
