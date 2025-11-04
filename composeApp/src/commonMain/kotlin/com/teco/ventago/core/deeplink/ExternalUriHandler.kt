package com.teco.ventago.core.deeplink


object ExternalUriHandler {
    private var cached: String? = null
    var listener: ((String) -> Unit)? = null
        set(value) {
            field = value
            cached?.let {
                value?.invoke(it)
                cached = null
            }
        }

    fun onNewUri(uri: String) {
        listener?.invoke(uri) ?: run { cached = uri }
    }
}