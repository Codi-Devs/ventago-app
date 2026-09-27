package com.teco.ventago.core.version

interface IReachabilityProbe {
    suspend fun isReachable(): Boolean
}
