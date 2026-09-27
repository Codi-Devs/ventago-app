package com.teco.ventago.core.version

import com.teco.ventago.core.LocalStorage

class RecommendedDismissalStore(
    private val storage: LocalStorage,
) {
    fun isDismissed(recommendedVersion: String, installedVersion: String): Boolean {
        return storage.string(KEY_RECOMMENDED) == recommendedVersion &&
            storage.string(KEY_INSTALLED) == installedVersion
    }

    fun dismiss(recommendedVersion: String, installedVersion: String) {
        storage.set(KEY_RECOMMENDED, recommendedVersion)
        storage.set(KEY_INSTALLED, installedVersion)
    }

    companion object {
        private const val KEY_RECOMMENDED = "version_gate.dismissed_recommended_version"
        private const val KEY_INSTALLED = "version_gate.dismissed_installed_version"
    }
}
