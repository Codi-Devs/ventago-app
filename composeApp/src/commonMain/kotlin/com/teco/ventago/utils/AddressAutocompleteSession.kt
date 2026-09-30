package com.teco.ventago.utils

import com.teco.ventago.features.business.domain.model.BusinessAddress

/** One address request owned by the current Android activity; used on the main thread. */
internal class AddressAutocompleteSession(
    private val onFailure: (RuntimeException) -> Unit = {}
) {
    private var owner: Any? = null
    private var launchAction: (() -> Unit)? = null
    private var onResult: ((BusinessAddress?) -> Unit)? = null

    fun attach(owner: Any, launchAction: () -> Unit) {
        if (this.owner != null && this.owner !== owner) complete(null)
        this.owner = owner
        this.launchAction = launchAction
    }

    fun detach(owner: Any, keepPendingResult: Boolean = false) {
        // An old activity can be destroyed after its replacement has attached.
        if (this.owner !== owner) return
        this.owner = null
        launchAction = null
        // A configuration recreation retains the requesting ViewModel/callback.
        if (!keepPendingResult) complete(null)
    }

    fun launch(onResult: (BusinessAddress?) -> Unit) {
        val action = launchAction
        if (action == null || this.onResult != null) {
            onResult(null)
            return
        }
        this.onResult = onResult
        try {
            action()
        } catch (error: RuntimeException) {
            onFailure(error)
            complete(null)
        }
    }

    fun complete(address: BusinessAddress?) {
        val callback = onResult
        onResult = null
        callback?.invoke(address)
    }
}
