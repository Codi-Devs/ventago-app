package com.teco.ventago.utils

import com.teco.ventago.features.business.domain.model.BusinessAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class AddressAutocompleteSessionTest {
    @Test
    fun missingActivityCancelsInsteadOfCrashing() {
        var calls = 0
        AddressAutocompleteSession().launch { assertNull(it); calls++ }
        assertEquals(1, calls)
    }

    @Test
    fun failedInitializationOrLaunchCancelsAndAllowsRetry() {
        var failures = 0
        var cancellations = 0
        val session = AddressAutocompleteSession { failures++ }
        val owner = Any()
        session.attach(owner) { throw IllegalStateException("Places unavailable") }
        session.launch { assertNull(it); cancellations++ }
        session.attach(owner) {}
        val address = BusinessAddress("id", "address", 1.0, 2.0)
        session.launch { assertSame(address, it) }
        session.complete(address)
        assertEquals(1, failures)
        assertEquals(1, cancellations)
    }

    @Test
    fun selectionIsDeliveredOnlyOnce() {
        val session = AddressAutocompleteSession()
        var calls = 0
        val address = BusinessAddress("id", "address", 1.0, 2.0)
        session.attach(Any()) {}
        session.launch { assertSame(address, it); calls++ }
        session.complete(address)
        session.complete(null)
        assertEquals(1, calls)
    }

    @Test
    fun duplicateTapDoesNotReplacePendingSelection() {
        val session = AddressAutocompleteSession()
        var launches = 0
        var selections = 0
        var cancellations = 0
        session.attach(Any()) { launches++ }
        session.launch { selections++ }
        session.launch { assertNull(it); cancellations++ }
        session.complete(BusinessAddress("id", "address", 1.0, 2.0))
        assertEquals(1, launches)
        assertEquals(1, selections)
        assertEquals(1, cancellations)
    }

    @Test
    fun destroyingOwnerCancelsAndRemovesLauncher() {
        val session = AddressAutocompleteSession()
        val owner = Any()
        var launches = 0
        var cancellations = 0
        session.attach(owner) { launches++ }
        session.launch { assertNull(it); cancellations++ }
        session.detach(owner)
        session.launch { assertNull(it); cancellations++ }
        session.complete(BusinessAddress("id", "address", 1.0, 2.0))
        assertEquals(1, launches)
        assertEquals(2, cancellations)
    }

    @Test
    fun oldActivityDestructionDoesNotDetachReplacement() {
        val session = AddressAutocompleteSession()
        val oldOwner = Any()
        val newOwner = Any()
        var oldCancellations = 0
        var launches = 0
        session.attach(oldOwner) {}
        session.launch { assertNull(it); oldCancellations++ }
        session.attach(newOwner) { launches++ }
        session.detach(oldOwner)
        session.launch {}
        assertEquals(1, oldCancellations)
        assertEquals(1, launches)
    }

    @Test
    fun configurationRecreationRetainsPendingSelectionForReplacement() {
        val session = AddressAutocompleteSession()
        val oldOwner = Any()
        val newOwner = Any()
        val address = BusinessAddress("id", "address", 1.0, 2.0)
        var calls = 0
        session.attach(oldOwner) {}
        session.launch { assertSame(address, it); calls++ }
        session.detach(oldOwner, keepPendingResult = true)
        session.attach(newOwner) {}
        session.complete(address)
        session.complete(null)
        assertEquals(1, calls)
    }

    @Test
    fun pendingConfigurationResultSurvivesLaunchAttemptBeforeReattachment() {
        val session = AddressAutocompleteSession()
        val owner = Any()
        val address = BusinessAddress("id", "address", 1.0, 2.0)
        var selections = 0
        var rejectedRequests = 0
        session.attach(owner) {}
        session.launch { assertSame(address, it); selections++ }
        session.detach(owner, keepPendingResult = true)
        session.launch { assertNull(it); rejectedRequests++ }
        session.attach(Any()) {}
        session.complete(address)
        assertEquals(1, selections)
        assertEquals(1, rejectedRequests)
    }

    @Test
    fun resultWithoutPendingCallbackIsIgnoredAndAllowsFreshRequest() {
        val restored = AddressAutocompleteSession()
        val address = BusinessAddress("id", "address", 1.0, 2.0)
        restored.complete(address)
        restored.attach(Any()) {}
        var calls = 0
        restored.launch { assertSame(address, it); calls++ }
        restored.complete(address)
        assertEquals(1, calls)
    }
}
