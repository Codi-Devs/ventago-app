package com.teco.ventago.core.flags

import kotlinx.coroutines.flow.StateFlow

data class AppFlagsState(
    val maintenanceMode: Boolean = false,
    val dgiDown: Boolean = false,
)

interface IFlagsService {
    fun flags(): StateFlow<AppFlagsState>
    fun initialize()
    fun destroy()
    fun isInitialized(): Boolean
    fun getMaintenanceMode(): Boolean
    fun getDgiDown(): Boolean
}
