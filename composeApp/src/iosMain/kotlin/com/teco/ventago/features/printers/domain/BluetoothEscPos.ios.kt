@file:OptIn(
    kotlinx.cinterop.BetaInteropApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class,
)

package com.teco.ventago.features.printers.domain

import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreBluetooth.CBAdvertisementDataLocalNameKey
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBCharacteristicProperties
import platform.CoreBluetooth.CBCharacteristicPropertyWrite
import platform.CoreBluetooth.CBCharacteristicPropertyWriteWithoutResponse
import platform.CoreBluetooth.CBCharacteristicWriteType
import platform.CoreBluetooth.CBCharacteristicWriteWithResponse
import platform.CoreBluetooth.CBCharacteristicWriteWithoutResponse
import platform.CoreBluetooth.CBManagerStatePoweredOff
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBManagerStateUnauthorized
import platform.CoreBluetooth.CBManagerStateUnsupported
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.CoreBluetooth.CBService
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.create
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSObject
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import kotlin.coroutines.resume

private const val PRINTER_NAME_SCORE = 20
private const val IMMEDIATE_CONNECT_SCORE = 100

private var activeBleSession: IosBleEscPosSession? = null

actual suspend fun sendBluetoothEscPos(
    payload: ByteArray,
    deviceId: String?,
): BluetoothPrintOutcome {
    if (payload.isEmpty()) {
        return BluetoothPrintOutcome(false, "No hay contenido para imprimir.")
    }
    return suspendCancellableCoroutine { continuation ->
        val session = IosBleEscPosSession(payload, deviceId, continuation)
        activeBleSession = session
        continuation.invokeOnCancellation { session.stop() }
        session.start()
    }
}

class IosBleEscPosSession(
    private val payload: ByteArray,
    private val deviceId: String?,
    private val continuation: CancellableContinuation<BluetoothPrintOutcome>,
) : NSObject(), CBCentralManagerDelegateProtocol, CBPeripheralDelegateProtocol {
    private var central: CBCentralManager? = null
    private var target: CBPeripheral? = null
    private var writeCharacteristic: CBCharacteristic? = null
    private var writeType: CBCharacteristicWriteType = CBCharacteristicWriteWithoutResponse
    private val candidates = linkedMapOf<String, Candidate>()
    private var pendingServices = 0
    private var writeOffset = 0
    private var finished = false
    private var connecting = false
    private var poweredOn = false
    private var printerName: String = "impresora"

    fun start() {
        central = CBCentralManager(delegate = this, queue = dispatch_get_main_queue())
        after(6_000) {
            if (!poweredOn) {
                finish(BluetoothPrintOutcome(false, "Bluetooth no respondió. Revisa que esté encendido y que VentaGO tenga permiso."))
            }
        }
    }

    fun stop() {
        finish(BluetoothPrintOutcome(false, "Impresión cancelada."))
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        when (central.state) {
            CBManagerStatePoweredOn -> {
                poweredOn = true
                central.scanForPeripheralsWithServices(null, null)
                after(8_000) { connectBest() }
            }
            CBManagerStatePoweredOff -> finish(BluetoothPrintOutcome(false, "Bluetooth está apagado. Enciéndelo y vuelve a intentar."))
            CBManagerStateUnauthorized -> finish(BluetoothPrintOutcome(false, "Falta permiso de Bluetooth para VentaGO."))
            CBManagerStateUnsupported -> finish(BluetoothPrintOutcome(false, "Este iPhone no puede usar Bluetooth de baja energía."))
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
        val id = didDiscoverPeripheral.identifier.UUIDString
        if (!deviceId.isNullOrBlank() && id != deviceId) return
        val score = bluetoothPrinterNameScore(name)
        if (deviceId.isNullOrBlank() && score <= 0) return
        val usableScore = if (deviceId.isNullOrBlank()) score else maxOf(score, PRINTER_NAME_SCORE)
        candidates[id] = Candidate(didDiscoverPeripheral, id, name ?: id, usableScore)
        if (!deviceId.isNullOrBlank() || usableScore >= IMMEDIATE_CONNECT_SCORE) {
            connectBest()
        }
    }

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
        didConnectPeripheral.delegate = this
        didConnectPeripheral.discoverServices(null)
    }

    @ObjCSignatureOverride
    override fun centralManager(
        central: CBCentralManager,
        didFailToConnectPeripheral: CBPeripheral,
        error: NSError?,
    ) {
        finish(
            BluetoothPrintOutcome(
                false,
                "No pude conectar ${didFailToConnectPeripheral.name ?: printerName}. ${error?.localizedDescription ?: ""}".trim(),
            ),
        )
    }

    @ObjCSignatureOverride
    override fun centralManager(
        central: CBCentralManager,
        didDisconnectPeripheral: CBPeripheral,
        error: NSError?,
    ) {
        if (writeOffset >= payload.size && writeCharacteristic != null) {
            finish(successOutcome())
        }
    }

    override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
        if (didDiscoverServices != null) {
            finish(BluetoothPrintOutcome(false, "No pude leer los servicios de la impresora."))
            return
        }
        val services = peripheral.serviceList()
        if (services.isEmpty()) {
            finish(BluetoothPrintOutcome(false, "La impresora no anuncia servicios Bluetooth."))
            return
        }
        pendingServices = services.size
        services.forEach { service ->
            peripheral.discoverCharacteristics(null, service)
        }
    }

    override fun peripheral(
        peripheral: CBPeripheral,
        didDiscoverCharacteristicsForService: CBService,
        error: NSError?,
    ) {
        pendingServices -= 1
        if (pendingServices > 0) return
        val chosen = peripheral.serviceList()
            .flatMap { service ->
                service.characteristicList().map { characteristic ->
                    characteristic to characteristicScore(service, characteristic)
                }
            }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first
        if (chosen == null) {
            val services = peripheral.serviceList().joinToString { it.UUID.UUIDString }
            finish(BluetoothPrintOutcome(false, "La impresora no tiene un canal de escritura. Servicios: $services"))
            return
        }
        writeCharacteristic = chosen
        writeType = if (chosen.properties.hasFlag(CBCharacteristicPropertyWriteWithoutResponse)) {
            CBCharacteristicWriteWithoutResponse
        } else {
            CBCharacteristicWriteWithResponse
        }
        writeOffset = 0
        writeNext()
    }

    override fun peripheral(
        peripheral: CBPeripheral,
        didWriteValueForCharacteristic: CBCharacteristic,
        error: NSError?,
    ) {
        if (error != null) {
            finish(BluetoothPrintOutcome(false, "La impresora rechazó los datos. ${error.localizedDescription}"))
            return
        }
        if (writeType == CBCharacteristicWriteWithResponse) {
            writeNext()
        }
    }

    private fun connectBest() {
        if (finished || connecting) return
        val best = candidates.values.maxByOrNull { it.score }
        if (best == null || best.score < PRINTER_NAME_SCORE) {
            val message = if (!deviceId.isNullOrBlank()) {
                "No encontré la impresora de este equipo. Enciéndela y acércala."
            } else {
                val seen = candidates.values.joinToString { it.name }.ifBlank { "ninguno con nombre de impresora" }
                "No encontré una impresora Bluetooth cerca. Vi: $seen"
            }
            finish(BluetoothPrintOutcome(false, message))
            return
        }
        connecting = true
        printerName = best.name
        target = best.peripheral
        central?.stopScan()
        best.peripheral.delegate = this
        central?.connectPeripheral(best.peripheral, null)
        after(12_000) {
            if (writeCharacteristic == null && !finished) {
                finish(BluetoothPrintOutcome(false, "Tiempo agotado al conectar con ${best.name}."))
            }
        }
    }

    private fun writeNext() {
        val peripheral = target ?: return
        val characteristic = writeCharacteristic ?: return
        if (finished) return
        if (writeOffset >= payload.size) {
            after(700) { finish(successOutcome()) }
            return
        }
        val chunkSize = peripheral.maximumWriteValueLengthForType(writeType).toInt().coerceIn(20, 180)
        val end = (writeOffset + chunkSize).coerceAtMost(payload.size)
        val slice = payload.copyOfRange(writeOffset, end)
        peripheral.writeValue(slice.toNsData(), characteristic, writeType)
        writeOffset = end
        if (writeType == CBCharacteristicWriteWithoutResponse) {
            after(25) { writeNext() }
        }
    }

    private fun successOutcome(): BluetoothPrintOutcome {
        return BluetoothPrintOutcome(
            success = true,
            message = "Listo. Imprimí en $printerName (BLE).",
            printerName = printerName,
        )
    }

    private fun characteristicScore(service: CBService, characteristic: CBCharacteristic): Int {
        val canWrite = characteristic.properties.hasFlag(CBCharacteristicPropertyWrite) ||
            characteristic.properties.hasFlag(CBCharacteristicPropertyWriteWithoutResponse)
        if (!canWrite) return 0
        val uuid = characteristic.UUID.UUIDString.uppercase()
        val serviceUuid = service.UUID.UUIDString.uppercase()
        var score = 10
        if (uuid.contains("FFE1") || uuid.contains("2AF1") || uuid.contains("8841")) score += 100
        if (serviceUuid.contains("FFE0") || serviceUuid.contains("18F0") || serviceUuid.contains("49535343")) {
            score += 40
        }
        if (characteristic.properties.hasFlag(CBCharacteristicPropertyWriteWithoutResponse)) score += 30
        return score
    }

    private fun finish(outcome: BluetoothPrintOutcome) {
        if (finished || !continuation.isActive) return
        finished = true
        runCatching { central?.stopScan() }
        target?.let { peripheral -> runCatching { central?.cancelPeripheralConnection(peripheral) } }
        activeBleSession = null
        continuation.resume(outcome)
    }

    private fun after(millis: Long, block: () -> Unit) {
        dispatch_after(
            dispatch_time(DISPATCH_TIME_NOW, millis * 1_000_000),
            dispatch_get_main_queue(),
            block,
        )
    }
}

private fun CBPeripheral.serviceList(): List<CBService> {
    return services.orEmpty().mapNotNull { it as? CBService }
}

private fun CBService.characteristicList(): List<CBCharacteristic> {
    return characteristics.orEmpty().mapNotNull { it as? CBCharacteristic }
}

private fun CBCharacteristicProperties.hasFlag(flag: CBCharacteristicProperties): Boolean {
    return (this and flag) != 0uL
}

private data class Candidate(
    val peripheral: CBPeripheral,
    val id: String,
    val name: String,
    val score: Int,
)

private fun ByteArray.toNsData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}
