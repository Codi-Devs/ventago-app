package com.teco.ventago.core.version

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.gms.tasks.Tasks
import com.teco.ventago.MainActivityHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidAppUpdateLauncher(
    private val context: Context,
    private val activityHolder: MainActivityHolder,
    private val buildInfo: AppBuildInfo,
) : IAppUpdateLauncher {
    override suspend fun startForcedUpdate() {
        if (!startPlayUpdate(AppUpdateType.IMMEDIATE)) {
            openStoreListing()
        }
    }

    override suspend fun startRecommendedUpdate() {
        if (!startPlayUpdate(AppUpdateType.FLEXIBLE)) {
            openStoreListing()
        }
    }

    override fun openStoreListing() {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(buildInfo.channel.marketUri())).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage("com.android.vending")
        }
        try {
            context.startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(buildInfo.channel.httpsListingUrl())).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    private suspend fun startPlayUpdate(type: Int): Boolean {
        val activity = runCatching { activityHolder.activity }.getOrNull() ?: return false
        return try {
            val manager = AppUpdateManagerFactory.create(context)
            val info = withContext(Dispatchers.IO) { Tasks.await(manager.appUpdateInfo) }
            val availability = info.updateAvailability()
            val inProgress = availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            val available = availability == UpdateAvailability.UPDATE_AVAILABLE || inProgress
            if (!available || !info.isUpdateTypeAllowed(type)) {
                return false
            }
            withContext(Dispatchers.Main) {
                Tasks.await(
                    manager.startUpdateFlow(
                        info,
                        activity,
                        AppUpdateOptions.newBuilder(type).build(),
                    )
                )
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
