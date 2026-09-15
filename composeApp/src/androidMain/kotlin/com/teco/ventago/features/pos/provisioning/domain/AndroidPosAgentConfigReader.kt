package com.teco.ventago.features.pos.provisioning.domain

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.teco.ventago.features.pos.provisioning.domain.model.PosAgentConfigResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class AndroidPosAgentConfigReader(
    private val context: Context,
) : IPosAgentConfigReader {
    override suspend fun getDeviceConfig(): PosAgentConfigResult? = withContext(Dispatchers.Main) {
        withTimeoutOrNull(AGENT_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val intent = Intent(ACTION_GET_DEVICE_CONFIG).apply {
                    setPackage(POS_AGENT_PACKAGE)
                }
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent?) {
                        if (!continuation.isActive) return
                        val extras = getResultExtras(false)
                        val activated = extras?.getBoolean(EXTRA_ACTIVATED, false) ?: false
                        val configJson = extras?.getString(EXTRA_CONFIG_JSON)
                        if (extras == null || !activated) {
                            continuation.resume(null)
                            return
                        }
                        continuation.resume(
                            PosAgentConfigResult(
                                activated = true,
                                configJson = configJson.orEmpty(),
                            )
                        )
                    }
                }
                context.sendOrderedBroadcast(
                    intent,
                    null,
                    receiver,
                    null,
                    Activity.RESULT_CANCELED,
                    null,
                    null
                )
            }
        }
    }

    private companion object {
        const val POS_AGENT_PACKAGE = "com.teco.ventago.ventagoposagent"
        const val ACTION_GET_DEVICE_CONFIG = "com.teco.ventago.ventagoposagent.action.GET_DEVICE_CONFIG"
        const val EXTRA_ACTIVATED = "com.teco.ventago.ventagoposagent.extra.ACTIVATED"
        const val EXTRA_CONFIG_JSON = "com.teco.ventago.ventagoposagent.extra.CONFIG_JSON"
        const val AGENT_TIMEOUT_MS = 3_000L
    }
}
