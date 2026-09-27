package com.teco.ventago.core.version

interface IVersionPolicySource {
    suspend fun refresh(): VersionPolicy
    fun current(): VersionPolicy
}
