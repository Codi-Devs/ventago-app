package com.teco.ventago.features.printers.domain

import com.teco.ventago.features.printers.domain.model.DiscoveredPrinterCandidate

interface PrinterDiscoveryEngine {
    fun isDiscoverySupported(): Boolean = true

    suspend fun start(onDiscovered: (DiscoveredPrinterCandidate) -> Unit)

    suspend fun stop()
}
