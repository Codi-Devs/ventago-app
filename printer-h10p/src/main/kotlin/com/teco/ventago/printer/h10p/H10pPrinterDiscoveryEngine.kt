package com.teco.ventago.printer.h10p

import com.teco.ventago.features.printers.domain.PrinterDiscoveryEngine
import com.teco.ventago.features.printers.domain.model.DiscoveredPrinterCandidate

class H10pPrinterDiscoveryEngine : PrinterDiscoveryEngine {
    override fun isDiscoverySupported(): Boolean = false

    override suspend fun start(onDiscovered: (DiscoveredPrinterCandidate) -> Unit) = Unit

    override suspend fun stop() = Unit
}
