package com.teco.ventago.features.printers.domain

import android.content.Context
import android.os.Build
import com.epson.epos2.Epos2Exception
import com.epson.epos2.discovery.DeviceInfo
import com.epson.epos2.discovery.Discovery
import com.epson.epos2.discovery.DiscoveryListener
import com.epson.epos2.discovery.FilterOption
import com.teco.ventago.features.printers.domain.model.DiscoveredPrinterCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class AndroidEpsonPrinterDiscoveryEngine(
    private val context: Context,
) : PrinterDiscoveryEngine {
    override fun isDiscoverySupported(): Boolean = !isRunningOnAndroidEmulator()

    override suspend fun start(onDiscovered: (DiscoveredPrinterCandidate) -> Unit) {
        withContext(Dispatchers.IO) {
            if (!isDiscoverySupported()) return@withContext

            val filterOption = FilterOption().apply {
                portType = Discovery.PORTTYPE_TCP
                deviceType = Discovery.TYPE_PRINTER
                deviceModel = Discovery.MODEL_ALL
                epsonFilter = Discovery.FILTER_NAME
            }
            val listener = DiscoveryListener { deviceInfo: DeviceInfo ->
                onDiscovered(
                    DiscoveredPrinterCandidate(
                        target = deviceInfo.target,
                        deviceName = deviceInfo.deviceName,
                        ipAddress = deviceInfo.ipAddress,
                        macAddress = deviceInfo.macAddress,
                        deviceType = deviceInfo.deviceType,
                    )
                )
            }

            Discovery.start(context, filterOption, listener)
        }
    }

    override suspend fun stop() {
        withContext(Dispatchers.IO) {
            try {
                Discovery.stop()
            } catch (error: Epos2Exception) {
                if (error.errorStatus != Epos2Exception.ERR_ILLEGAL) {
                    throw error
                }
            }
        }
    }
}

private fun isRunningOnAndroidEmulator(): Boolean {
    return Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.MODEL.contains("google_sdk", ignoreCase = true) ||
        Build.MODEL.contains("Emulator", ignoreCase = true) ||
        Build.MODEL.contains("Android SDK built for x86", ignoreCase = true) ||
        Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
        Build.PRODUCT.contains("sdk_gphone", ignoreCase = true)
}
