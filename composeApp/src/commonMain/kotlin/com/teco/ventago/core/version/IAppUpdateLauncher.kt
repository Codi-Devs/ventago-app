package com.teco.ventago.core.version

interface IAppUpdateLauncher {
    suspend fun startForcedUpdate()
    suspend fun startRecommendedUpdate()
    fun openStoreListing()
}
