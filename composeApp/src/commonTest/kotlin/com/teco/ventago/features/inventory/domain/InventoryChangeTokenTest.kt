package com.teco.ventago.features.inventory.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InventoryChangeTokenTest {

    @Test
    fun firstAttachWithoutLocalTokenOnlyStores() {
        assertEquals(
            InventoryChangeTokenAction.StoreOnly,
            resolveInventoryChangeToken(local = null, remote = "uuid-1"),
        )
        assertEquals(
            InventoryChangeTokenAction.StoreOnly,
            resolveInventoryChangeToken(local = "", remote = "uuid-1"),
        )
    }

    @Test
    fun matchingTokensDoNotInvalidate() {
        assertEquals(
            InventoryChangeTokenAction.Ignore,
            resolveInventoryChangeToken(local = "uuid-1", remote = "uuid-1"),
        )
    }

    @Test
    fun differentRemoteTokenInvalidates() {
        assertEquals(
            InventoryChangeTokenAction.EmitInvalidate,
            resolveInventoryChangeToken(local = "uuid-1", remote = "uuid-2"),
        )
    }

    @Test
    fun emptyRemoteIsIgnored() {
        assertEquals(
            InventoryChangeTokenAction.Ignore,
            resolveInventoryChangeToken(local = "uuid-1", remote = null),
        )
        assertEquals(
            InventoryChangeTokenAction.Ignore,
            resolveInventoryChangeToken(local = "", remote = "  "),
        )
    }
}

class InventoryLocalCacheKeyTest {

    @Test
    fun matchesInventoryKeysForBusiness() {
        assertTrue(InventoryLocalCache.inventoryCacheKeyBelongsToBusiness("cache:inventory:access:42", 42))
        assertTrue(InventoryLocalCache.inventoryCacheKeyBelongsToBusiness("cache:inventory:dashboard:42", 42))
        assertTrue(InventoryLocalCache.inventoryCacheKeyBelongsToBusiness("cache:inventory:availability:42:9:10", 42))
        assertTrue(InventoryLocalCache.inventoryCacheKeyBelongsToBusiness("cache:inventory:sale-location:42:0000:001", 42))
    }

    @Test
    fun ignoresKeysFromOtherBusinesses() {
        assertFalse(InventoryLocalCache.inventoryCacheKeyBelongsToBusiness("cache:inventory:access:41", 42))
        assertFalse(InventoryLocalCache.inventoryCacheKeyBelongsToBusiness("cache:inventory:access:420", 42))
        assertFalse(InventoryLocalCache.inventoryCacheKeyBelongsToBusiness("changed_inventory", 42))
        assertFalse(InventoryLocalCache.inventoryCacheKeyBelongsToBusiness("cache:changed_inventory", 42))
    }
}
