package com.teco.ventago.features.printers.domain

import com.teco.ventago.features.printers.domain.model.NARROW_CANVAS_WIDTH_DOTS
import com.teco.ventago.features.printers.domain.model.PrintCommand

data class BluetoothPrintOutcome(
    val success: Boolean,
    val message: String,
    val printerName: String? = null,
)

data class NearbyBluetoothPrinter(
    val id: String,
    val name: String,
    val pairedOnly: Boolean,
)

data class BluetoothPrinterAvailability(
    val enabled: Boolean,
    val needsPermission: Boolean = false,
    val printers: List<NearbyBluetoothPrinter> = emptyList(),
    val message: String? = null,
)

/**
 * Envia bytes ESC/POS a la impresora Bluetooth de este equipo.
 * Android usa el puerto serie clasico. iOS usa BLE.
 * [deviceId] es la MAC en Android o el UUID del peripheral en iOS.
 */
expect suspend fun sendBluetoothEscPos(payload: ByteArray, deviceId: String?): BluetoothPrintOutcome

expect suspend fun listBluetoothPrinters(): BluetoothPrinterAvailability

expect suspend fun isSavedBluetoothPrinterAvailable(deviceId: String): Boolean

expect fun openBluetoothSettings()

expect fun openAppSettings()

suspend fun printBluetoothTicket(
    commands: List<PrintCommand>,
    deviceId: String? = null,
    paperWidthDots: Int = NARROW_CANVAS_WIDTH_DOTS,
): BluetoothPrintOutcome {
    if (commands.isEmpty()) {
        return BluetoothPrintOutcome(success = false, message = "No hay contenido para imprimir.")
    }
    val prepared = commandsForBluetooth(commands, paperWidthDots)
    if (prepared.isEmpty()) {
        return BluetoothPrintOutcome(success = false, message = "No hay contenido para imprimir.")
    }
    return sendBluetoothEscPos(EscPosTicketEncoder.encode(prepared), deviceId)
}
