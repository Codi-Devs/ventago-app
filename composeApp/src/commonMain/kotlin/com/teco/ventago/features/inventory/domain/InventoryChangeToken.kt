package com.teco.ventago.features.inventory.domain

enum class InventoryChangeTokenAction {
    Ignore,
    StoreOnly,
    EmitInvalidate,
}

fun resolveInventoryChangeToken(
    local: String?,
    remote: String?,
): InventoryChangeTokenAction {
    val remoteValue = remote?.trim().orEmpty()
    if (remoteValue.isEmpty()) return InventoryChangeTokenAction.Ignore
    val localValue = local?.trim().orEmpty()
    if (localValue.isEmpty()) return InventoryChangeTokenAction.StoreOnly
    if (remoteValue != localValue) return InventoryChangeTokenAction.EmitInvalidate
    return InventoryChangeTokenAction.Ignore
}
