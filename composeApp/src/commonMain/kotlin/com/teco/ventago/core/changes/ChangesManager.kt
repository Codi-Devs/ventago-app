package com.teco.ventago.core.changes

import com.teco.ventago.core.LocalStorage
import com.teco.ventago.features.inventory.domain.InventoryChangeTokenAction
import com.teco.ventago.features.inventory.domain.resolveInventoryChangeToken
import com.teco.ventago.utils.randomUUID
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChangesManager(private val storage: LocalStorage): IChangesManager {
    private val db =
        Firebase.database("https://ventago-25d2c-default-rtdb.firebaseio.com/")

    private val changesRef =
        db.reference("changes")
    private val userChangesRef =
        db.reference("user_changes")

    private val userChanged = MutableStateFlow(1)
    private val menuChanged = MutableStateFlow(1)
    private val inventoryChanged = MutableStateFlow(1)
    private val businessChanged = MutableStateFlow(1)
    private val financialChanged = MutableStateFlow(1)
    private val customerChanged = MutableStateFlow(1)
    private val branchesChanged = MutableStateFlow(1)
    private val addedBusiness = MutableStateFlow(1)
    private val purchaseChanged = MutableStateFlow(1)
    private val posCustomerChanged = MutableSharedFlow<String>(replay = 1)

    private var listenersJob: Job? = null
    private var authListenerJob: Job? = null
    private var userId: Int? = null
    private var businessId: Int? = null
    private var menuId: Int? = null
    private var pendingInitParams: InitParams? = null

    private var initialized = false
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun productsListener(): Flow<Int> = menuChanged
    override fun inventoryListener(): Flow<Int> = inventoryChanged
    override fun businessListener(): Flow<Int> = businessChanged
    override fun financialListener(): Flow<Int> = financialChanged
    override fun customersListener(): Flow<Int> = customerChanged
    override fun branchesListener(): Flow<Int> = branchesChanged
    override fun userListener(): Flow<Int> = userChanged
    override fun purchaseListener(): Flow<Int> = purchaseChanged
    override fun addedBusinessListener(): Flow<Int> = addedBusiness

    override fun initialize(businessId: Int, menuId: Int, userId: Int) {
        pendingInitParams = InitParams(businessId = businessId, menuId = menuId, userId = userId)
        observeAuthState()
        if (Firebase.auth.currentUser == null) return
        startListenersIfNeeded()
    }

    private fun observeAuthState() {
        if (authListenerJob != null) return

        authListenerJob = Firebase.auth.authStateChanged.onEach { firebaseUser ->
            if (firebaseUser == null) {
                stopActiveListeners()
                return@onEach
            }
            startListenersIfNeeded()
        }.launchIn(scope)
    }

    private fun startListenersIfNeeded() {
        val params = pendingInitParams ?: return
        if (initialized && this.businessId == params.businessId && this.menuId == params.menuId && this.userId == params.userId) return

        stopActiveListeners()

        val currentBusinessId = params.businessId
        val currentUserId = params.userId

        this.businessId = params.businessId
        this.menuId = params.menuId
        this.userId = params.userId
        initialized = true
        listenersJob = scope.launch {
            changesRef.child("$currentBusinessId").valueEvents.onEach {
                var changedBusinessDB = ""
                if (it.child("changed_business").value != null) {
                    changedBusinessDB = it.child("changed_business").value<String?>() ?: ""
                }

                var changedFinancialDB = ""
                if (it.child("changed_financial").value != null) {
                    changedFinancialDB = it.child("changed_financial").value<String?>() ?: ""
                }

                var changedCustomersDB = ""
                if (it.child("customer_cache").value != null) {
                    changedCustomersDB = it.child("customer_cache").value<String?>() ?: ""
                }

                var changedBranchesDB = ""
                if (it.child("branches_cache").value != null) {
                    changedBranchesDB = it.child("branches_cache").value<String?>() ?: ""
                }

                var changedMenuDB = ""
                if (it.child("changed_menu").value != null) {
                    changedMenuDB = it.child("changed_menu").value<String?>() ?: ""
                }

                var changedInventoryDB = ""
                if (it.child("changed_inventory").value != null) {
                    changedInventoryDB = it.child("changed_inventory").value<String?>() ?: ""
                }

                val changedBusinessCache = storage.string("changed_business") ?: ""
                val changedMenuCache = storage.string("changed_menu") ?: ""
                val changedInventoryCache = storage.string("changed_inventory") ?: ""
                val changedFinancialCache = storage.string("changed_financial") ?: ""
                val changedCustomerCache = storage.string("customer_cache") ?: ""
                val changedBranchCache = storage.string("branches_cache") ?: ""

                if (changedBusinessDB != changedBusinessCache) {
                    businessChanged.update { actual -> actual + 1 }
                    storage.set("changed_business", changedBusinessDB)
                }

                if (changedFinancialDB != changedFinancialCache) {
                    financialChanged.update { actual -> actual + 1 }
                    storage.set("changed_financial", changedFinancialDB)
                }

                if (changedCustomersDB != changedCustomerCache) {
                    customerChanged.update { actual -> actual + 1 }
                    storage.set("customer_cache", changedCustomersDB)
                }

                if (changedBranchesDB != changedBranchCache) {
                    branchesChanged.update { actual -> actual + 1 }
                    storage.set("branches_cache", changedBranchesDB)
                }

                if (changedMenuDB != changedMenuCache) {
                    menuChanged.update {actual -> actual + 1 }
                    storage.set("changed_menu", changedMenuDB)
                }

                when (resolveInventoryChangeToken(changedInventoryCache, changedInventoryDB)) {
                    InventoryChangeTokenAction.StoreOnly -> {
                        storage.set("changed_inventory", changedInventoryDB)
                    }
                    InventoryChangeTokenAction.EmitInvalidate -> {
                        inventoryChanged.update { actual -> actual + 1 }
                        storage.set("changed_inventory", changedInventoryDB)
                    }
                    InventoryChangeTokenAction.Ignore -> Unit
                }
            }.launchIn(this)

            userChangesRef.child(currentUserId.toString()).valueEvents.onEach {
                var addedBusinessDB = ""
                if (it.child("added_business").exists) {
                    addedBusinessDB = it.child("added_business").value.toString()
                }

                var changedUserBD = ""
                if(it.child("changed_user").value != null){
                    changedUserBD = it.child("changed_user").value.toString()
                }

                var userPurchaseBD = ""
                if(it.child("user_purchase").value != null){
                    userPurchaseBD = it.child("user_purchase").value.toString()
                }

                val changedUserCache = storage.string("changed_user") ?: ""
                val userPurchaseCache = storage.string("user_purchase") ?: ""
                val addedBusinessCache = storage.string("added_business") ?: ""

                if(changedUserBD != changedUserCache) {
                    userChanged.update {actual ->
                        actual + 1
                    }
                    storage.set("changed_user", changedUserBD)
                }

                if(userPurchaseBD != userPurchaseCache) {
                    purchaseChanged.update {actual ->
                        actual + 1
                    }
                    storage.set("user_purchase", userPurchaseBD)
                }

                if (addedBusinessDB != addedBusinessCache) {
                    addedBusiness.update {actual ->
                        actual + 1
                    }
                    storage.set("added_business", addedBusinessDB)
                }
            }.launchIn(this)
        }
    }

    private fun stopActiveListeners() {
        initialized = false
        listenersJob?.cancel()
        listenersJob = null
        userId = null
        businessId = null
        menuId = null
    }

    override suspend fun productsChanged() {
        if (!initialized) return
        val changesRef = db.reference("changes/$businessId")
        val uniqueId = randomUUID()
        storage.set("changed_menu", uniqueId)
        changesRef.child("changed_menu").setValue(uniqueId)

    }

    override suspend fun businessChanged() {
        if (!initialized) return
        val changesRef = db.reference("changes/$businessId")
        val uniqueId = randomUUID()
        storage.set("changed_business", uniqueId)
        changesRef.child("changed_business").setValue(uniqueId)
    }

    override suspend fun financialChanged() {
        if (!initialized) return
        val changesRef = db.reference("changes/$businessId")
        val uniqueId = randomUUID()
        storage.set("changed_financial", uniqueId)
        changesRef.child("changed_financial").setValue(uniqueId)
    }

    override suspend fun customersChanged() {
        if (!initialized) return
        val changesRef = db.reference("changes/$businessId")
        val uniqueId = randomUUID()
        storage.set("customer_cache", uniqueId)
        changesRef.child("customer_cache").setValue(uniqueId)
    }

    override suspend fun branchesChanged() {
        if (!initialized) return
        val changesRef = db.reference("changes/$businessId")
        val uniqueId = randomUUID()
        storage.set("branches_cache", uniqueId)
        changesRef.child("branches_cache").setValue(uniqueId)
    }

    override suspend fun userChanged() {
        if (!initialized) return
        val changesRef = db.reference("user_changes/$userId")
        val uniqueId = randomUUID()
        storage.set("changed_user", uniqueId)
        changesRef.child("changed_user").setValue(uniqueId)
    }

    override fun removeListeners() {
        pendingInitParams = null
        stopActiveListeners()
        authListenerJob?.cancel()
        authListenerJob = null
    }

    private data class InitParams(
        val businessId: Int,
        val menuId: Int,
        val userId: Int
    )
}
