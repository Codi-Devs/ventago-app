@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.teco.ventago.features.printers.domain

import com.teco.ventago.features.printers.domain.model.DiscoveredPrinterCandidate
import com.teco.ventago.vendor.epson.EPOS2_PORTTYPE_TCP
import com.teco.ventago.vendor.epson.EPOS2_SUCCESS
import com.teco.ventago.vendor.epson.EPOS2_TYPE_PRINTER
import com.teco.ventago.vendor.epson.Epos2DeviceInfo
import com.teco.ventago.vendor.epson.Epos2Discovery
import com.teco.ventago.vendor.epson.Epos2DiscoveryDelegateProtocol
import com.teco.ventago.vendor.epson.Epos2FilterOption
import platform.darwin.NSObject

class IosEpsonPrinterDiscoveryEngine : PrinterDiscoveryEngine {
    private var delegate: Epos2DiscoveryDelegateProtocol? = null

    override suspend fun start(onDiscovered: (DiscoveredPrinterCandidate) -> Unit) {
        val filterOption = Epos2FilterOption().apply {
            portType = EPOS2_PORTTYPE_TCP.toInt()
            deviceType = EPOS2_TYPE_PRINTER.toInt()
        }

        val callbackDelegate = object : NSObject(), Epos2DiscoveryDelegateProtocol {
            override fun onDiscovery(deviceInfo: Epos2DeviceInfo?) {
                if (deviceInfo == null) return
                onDiscovered(
                    DiscoveredPrinterCandidate(
                        target = deviceInfo.target,
                        deviceName = deviceInfo.deviceName,
                        ipAddress = deviceInfo.ipAddress,
                        macAddress = deviceInfo.macAddress,
                        deviceType = deviceInfo.deviceType.toInt(),
                    )
                )
            }
        }

        delegate = callbackDelegate
        val code = Epos2Discovery.start(filterOption, callbackDelegate).toInt()
        if (code != EPOS2_SUCCESS.toInt()) {
            delegate = null
            error("Epson iOS discovery start failed: code=$code")
        }
    }

    override suspend fun stop() {
        runCatching { Epos2Discovery.stop().toInt() }
        delegate = null
    }
}
