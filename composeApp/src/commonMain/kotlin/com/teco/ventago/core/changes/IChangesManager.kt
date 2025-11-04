package com.teco.ventago.core.changes

import kotlinx.coroutines.flow.Flow

interface IChangesManager {

    fun productsListener(): Flow<Int>
    fun businessListener(): Flow<Int>
    fun financialListener(): Flow<Int>
    fun customersListener(): Flow<Int>
    fun branchesListener(): Flow<Int>
    fun userListener(): Flow<Int>
    fun purchaseListener(): Flow<Int>
    fun addedBusinessListener(): Flow<Int>

    suspend fun productsChanged()
    suspend fun businessChanged()
    suspend fun branchesChanged()
    suspend fun financialChanged()
    suspend fun customersChanged()
    suspend fun userChanged()
    fun removeListeners()
    fun initialize(businessId: Int, menuId: Int, userId: Int)
}