package com.teco.ventago.core.version

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.teco.ventago.MainActivityHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidAppUpdateLauncher(
    private val context: Context,
    private val activityHolder: MainActivityHolder,
    private val buildInfo: AppBuildInfo,
) : IAppUpdateLauncher {
    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(context)
    private val flexibleListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            manager.completeUpdate()
        }
    }

    init {
        manager.registerListener(flexibleListener)
    }

    override suspend fun startForcedUpdate() {
        startPlayUpdate(
            preferredType = AppUpdateType.IMMEDIATE,
            fallbackType = AppUpdateType.FLEXIBLE,
        )
    }

    override suspend fun startRecommendedUpdate() {
        startPlayUpdate(
            preferredType = AppUpdateType.FLEXIBLE,
            fallbackType = AppUpdateType.IMMEDIATE,
        )
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

    private suspend fun startPlayUpdate(preferredType: Int, fallbackType: Int) {
        val activity = runCatching { activityHolder.activity }.getOrNull()
        if (activity == null) {
            Log.w(TAG, "play in-app skipped: no activity")
            fallbackToStore("no_activity")
            return
        }
        val info = runCatching {
            withContext(Dispatchers.IO) { Tasks.await(manager.appUpdateInfo) }
        }.onFailure { error ->
            Log.w(TAG, "play appUpdateInfo failed: ${error.message}")
        }.getOrNull()
        if (info == null) {
            fallbackToStore("info_failed")
            return
        }

        val availability = info.updateAvailability()
        Log.i(
            TAG,
            "play in-app availability=$availability " +
                "flexible=${info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)} " +
                "immediate=${info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)} " +
                "installed=${buildInfo.versionName}",
        )
        val inProgress = availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
        val available = availability == UpdateAvailability.UPDATE_AVAILABLE || inProgress
        if (!available) {
            fallbackToStore("availability_$availability")
            return
        }

        val started = startFlow(info, activity, preferredType) || startFlow(info, activity, fallbackType)
        if (!started) {
            fallbackToStore("type_not_allowed")
        }
    }

    private suspend fun startFlow(
        info: AppUpdateInfo,
        activity: android.app.Activity,
        type: Int,
    ): Boolean {
        if (!info.isUpdateTypeAllowed(type)) return false
        return try {
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
        } catch (error: Exception) {
            Log.w(TAG, "play startUpdateFlow type=$type failed: ${error.message}")
            false
        }
    }

    private fun fallbackToStore(reason: String) {
        Log.w(TAG, "play in-app fallback store: $reason")
        if (isDebuggable) {
            Toast.makeText(
                context,
                "Play no ofrece actualización in-app en este install. Usa Internal testing o Internal app sharing.",
                Toast.LENGTH_LONG,
            ).show()
            return
        }
        openStoreListing()
    }

    private val isDebuggable: Boolean
        get() = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    companion object {
        private const val TAG = "version_gate"
    }
}
