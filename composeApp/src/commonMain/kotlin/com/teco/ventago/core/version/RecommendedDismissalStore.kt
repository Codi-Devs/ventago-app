package com.teco.ventago.core.version

import com.teco.ventago.core.LocalStorage

class RecommendedDismissalStore(
    private val storage: LocalStorage,
) {
    fun isDismissed(recommendedBuild: Long, installedBuild: Long): Boolean {
        return storage.long(KEY_RECOMMENDED) == recommendedBuild &&
            storage.long(KEY_INSTALLED) == installedBuild
    }

    fun dismiss(recommendedBuild: Long, installedBuild: Long) {
        storage.set(KEY_RECOMMENDED, recommendedBuild)
        storage.set(KEY_INSTALLED, installedBuild)
    }

    companion object {
        private const val KEY_RECOMMENDED = "version_gate.dismissed_recommended_build"
        private const val KEY_INSTALLED = "version_gate.dismissed_installed_build"
    }
}
