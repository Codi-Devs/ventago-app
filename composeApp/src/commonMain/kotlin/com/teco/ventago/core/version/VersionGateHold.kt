package com.teco.ventago.core.version

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class VersionGateHold {
    private val holds = MutableStateFlow(0)

    fun acquire(): () -> Unit {
        holds.update { it + 1 }
        var released = false
        return {
            if (!released) {
                released = true
                holds.update { current -> (current - 1).coerceAtLeast(0) }
            }
        }
    }

    fun hasHold(): Boolean = holds.value > 0
}
