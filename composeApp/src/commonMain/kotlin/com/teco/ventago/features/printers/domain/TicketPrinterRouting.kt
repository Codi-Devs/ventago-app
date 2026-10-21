package com.teco.ventago.features.printers.domain

internal enum class TicketPrinterChoice {
    Bluetooth,
    Network,
    None,
}

internal fun chooseTicketPrinter(
    bluetoothActive: Boolean,
    branchCode: String?,
    billingPointCode: String?,
    networkPrinterAvailable: Boolean,
): TicketPrinterChoice {
    if (bluetoothActive) return TicketPrinterChoice.Bluetooth
    if (branchCode.isNullOrBlank() || billingPointCode.isNullOrBlank() || !networkPrinterAvailable) {
        return TicketPrinterChoice.None
    }
    return TicketPrinterChoice.Network
}
