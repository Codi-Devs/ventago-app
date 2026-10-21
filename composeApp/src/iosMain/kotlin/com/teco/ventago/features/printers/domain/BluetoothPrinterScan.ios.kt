@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.teco.ventago.features.printers.domain

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreBluetooth.CBAdvertisementDataLocalNameKey
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBManagerStatePoweredOff
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBManagerStateUnauthorized
import platform.CoreBluetooth.CBManagerStateUnsupported
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSObject
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import kotlin.coroutines.resume

private const val SCAN_WINDOW_MS = 4_000L
private const val POWER_WINDOW_MS = 6_000L

private var activePrinterScan: IosBlePrinterScan? = null
private var activeAvailabilityProbe: IosBleAvailabilityProbe? = null

actual suspend fun listBluetoothPrinters(): BluetoothPrinterAvailability {
    return suspendCancellableCoroutine { continuation ->
        val scan = IosBlePrinterScan(continuation)
        activePrinterScan = scan
        continuation.invokeOnCancellation { scan.stop() }
        scan.start()
    }
}

actual suspend fun isSavedBluetoothPrinterAvailable(deviceId: String): Boolean {
    if (deviceId.isBlank()) return false
    return suspendCancellableCoroutine { continuation ->
        val probe = IosBleAvailabilityProbe(deviceId, continuation)
        activeAvailabilityProbe = probe
        continuation.invokeOnCancellation { probe.stop(false) }
        probe.start()
    }
}

actual fun openBluetoothSettings() {
    openUrl(UIApplicationOpenSettingsURLString)
}

actual fun openAppSettings() {
    openUrl(UIApplicationOpenSettingsURLString)
}

private fun openUrl(value: String): Boolean {
    val url = NSURL.URLWithString(value) ?: return false
    val app = UIApplication.sharedApplication
    if (!app.canOpenURL(url)) return false
    app.openURL(url)
    return true
}

class IosBlePrinterScan(
    private val continuation: CancellableContinuation<BluetoothPrinterAvailability>,
) : NSObject(), CBCentralManagerDelegateProtocol {
    private var central: CBCentralManager? = null
    private val found = linkedMapOf<String, NearbyBluetoothPrinter>()
    private var finished = false
    private var poweredOn = false

    fun start() {
        central = CBCentralManager(delegate = this, queue = dispatch_get_main_queue())
        after(POWER_WINDOW_MS) {
            if (!poweredOn) {
                finish(BluetoothPrinterAvailability(enabled = false, message = "Bluetooth no respondió."))
            }
        }
    }

    fun stop() {
        finish(BluetoothPrinterAvailability(enabled = poweredOn, printers = found.values.toList()))
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        when (central.state) {
            CBManagerStatePoweredOn -> {
                poweredOn = true
                rememberConnected(central)
                central.scanForPeripheralsWithServices(null, null)
                after(SCAN_WINDOW_MS) { finish(currentAvailability()) }
            }
            CBManagerStatePoweredOff -> {
                finish(BluetoothPrinterAvailability(enabled = false, message = "Bluetooth está apagado."))
            }
            CBManagerStateUnauthorized -> {
                finish(
                    BluetoothPrinterAvailability(
                        enabled = false,
                        needsPermission = true,
                        message = "VentaGO necesita permiso de Bluetooth.",
                    )
                )
            }
            CBManagerStateUnsupported -> {
                finish(BluetoothPrinterAvailability(enabled = false, message = "Este iPhone no puede usar Bluetooth."))
            }
            else -> Unit
        }
    }

    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: NSNumber,
    ) {
        val name = didDiscoverPeripheral.name
            ?: advertisementData[CBAdvertisementDataLocalNameKey] as? String
        remember(didDiscoverPeripheral, name, pairedOnly = false)
    }

    private fun rememberConnected(central: CBCentralManager) {
        val services = listOf(
            CBUUID.UUIDWithString("FFE0"),
            CBUUID.UUIDWithString("18F0"),
            CBUUID.UUIDWithString("49535343-FE7D-4AE5-8FA9-9FAFD205E455"),
        )
        val connected = runCatching {
            central.retrieveConnectedPeripheralsWithServices(services)
        }.getOrNull().orEmpty()
        connected.forEach { item ->
            val peripheral = item as? CBPeripheral ?: return@forEach
            remember(peripheral, peripheral.name, pairedOnly = false)
        }
    }

    private fun remember(peripheral: CBPeripheral, name: String?, pairedOnly: Boolean) {
        if (bluetoothPrinterNameScore(name) < 20) return
        val id = peripheral.identifier.UUIDString
        val current = found[id]
        found[id] = NearbyBluetoothPrinter(
            id = id,
            name = name?.takeIf { it.isNotBlank() } ?: current?.name ?: id,
            pairedOnly = (current?.pairedOnly != false) && pairedOnly,
        )
    }

    private fun currentAvailability(): BluetoothPrinterAvailability {
        return BluetoothPrinterAvailability(
            enabled = true,
            printers = found.values.sortedBy { it.name },
        )
    }

    private fun finish(availability: BluetoothPrinterAvailability) {
        if (finished) return
        finished = true
        runCatching { central?.stopScan() }
        if (activePrinterScan === this) activePrinterScan = null
        if (continuation.isActive) continuation.resume(availability)
    }

    private fun after(millis: Long, block: () -> Unit) {
        dispatch_after(
            dispatch_time(DISPATCH_TIME_NOW, millis * 1_000_000),
            dispatch_get_main_queue(),
            block,
        )
    }
}

private class IosBleAvailabilityProbe(
    private val deviceId: String,
    private val continuation: CancellableContinuation<Boolean>,
) : NSObject(), CBCentralManagerDelegateProtocol {
    private var central: CBCentralManager? = null
    private var finished = false

    fun start() {
        central = CBCentralManager(delegate = this, queue = dispatch_get_main_queue())
        after(2_500) { stop(false) }
    }

    fun stop(available: Boolean) {
        if (finished) return
        finished = true
        runCatching { central?.stopScan() }
        if (activeAvailabilityProbe === this) activeAvailabilityProbe = null
        if (continuation.isActive) continuation.resume(available)
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        when (central.state) {
            CBManagerStatePoweredOn -> {
                if (matchesConnected(central)) {
                    stop(true)
                    return
                }
                central.scanForPeripheralsWithServices(null, null)
            }
            CBManagerStatePoweredOff, CBManagerStateUnauthorized, CBManagerStateUnsupported -> stop(false)
            else -> Unit
        }
    }

    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: NSNumber,
    ) {
        val name = didDiscoverPeripheral.name
            ?: advertisementData[CBAdvertisementDataLocalNameKey] as? String
        if (bluetoothPrinterNameScore(name) < 20) return
        if (didDiscoverPeripheral.identifier.UUIDString.equals(deviceId, ignoreCase = true)) {
            stop(true)
        }
    }

    private fun matchesConnected(central: CBCentralManager): Boolean {
        val services = listOf(
            CBUUID.UUIDWithString("FFE0"),
            CBUUID.UUIDWithString("18F0"),
            CBUUID.UUIDWithString("49535343-FE7D-4AE5-8FA9-9FAFD205E455"),
        )
        val connected = runCatching { central.retrieveConnectedPeripheralsWithServices(services) }.getOrNull().orEmpty()
        return connected.any { item ->
            (item as? CBPeripheral)?.identifier?.UUIDString.equals(deviceId, ignoreCase = true)
        }
    }

    private fun after(millis: Long, block: () -> Unit) {
        dispatch_after(
            dispatch_time(DISPATCH_TIME_NOW, millis * 1_000_000),
            dispatch_get_main_queue(),
            block,
        )
    }
}
