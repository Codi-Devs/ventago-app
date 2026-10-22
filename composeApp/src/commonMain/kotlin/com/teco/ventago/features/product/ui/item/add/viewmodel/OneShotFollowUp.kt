package com.teco.ventago.features.product.ui.item.add.viewmodel

internal class OneShotFollowUp {
    private var action: (suspend () -> Unit)? = null

    fun arm(action: suspend () -> Unit) {
        this.action = action
    }

    fun consume(): (suspend () -> Unit)? {
        val current = action
        action = null
        return current
    }
}
