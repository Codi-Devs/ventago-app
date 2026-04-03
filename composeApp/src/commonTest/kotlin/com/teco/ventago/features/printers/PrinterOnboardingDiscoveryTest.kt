package com.teco.ventago.features.printers

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.features.printers.domain.PrinterDiscoveryEngine
import com.teco.ventago.features.printers.domain.PrinterDiscoveryService
import com.teco.ventago.features.printers.domain.model.DiscoveredPrinter
import com.teco.ventago.features.printers.domain.model.DiscoveredPrinterCandidate
import com.teco.ventago.features.printers.domain.model.PrinterDiscoveryState
import com.teco.ventago.features.printers.domain.model.PrinterDiscoveryStatus
import com.teco.ventago.features.printers.domain.model.parseDiscoveryTarget
import com.teco.ventago.features.printers.domain.model.toDiscoveredPrinter
import com.teco.ventago.features.printers.ui.viewmodel.PrinterBillingPointOption
import com.teco.ventago.features.printers.ui.viewmodel.PrinterBranchOption
import com.teco.ventago.features.printers.ui.viewmodel.PrinterConfigMode
import com.teco.ventago.features.printers.ui.viewmodel.PrinterOnboardingState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PrinterOnboardingDiscoveryTest {

    @Test
    fun parseDiscoveryTarget_supportsTcpTcpsAndCustomPortTargets() {
        val tcp = parseDiscoveryTarget("TCP:192.168.0.12")
        val tcps = parseDiscoveryTarget("TCPS:192.168.0.12")
        val customPort = parseDiscoveryTarget("TCP:192.168.0.12:8008")

        assertEquals("192.168.0.12", tcp.host)
        assertEquals(443, tcp.port)
        assertEquals("TCP:192.168.0.12", tcp.target)

        assertEquals("192.168.0.12", tcps.host)
        assertEquals(443, tcps.port)
        assertEquals("TCPS:192.168.0.12", tcps.target)

        assertEquals("192.168.0.12", customPort.host)
        assertEquals(8008, customPort.port)
        assertEquals("TCP:192.168.0.12:8008", customPort.target)
    }

    @Test
    fun discoveryService_deduplicatesByIpAndStopsAsCompleted() = runBlocking {
        val engine = FakeDiscoveryEngine()
        val service = PrinterDiscoveryService(engine, FakeLogger())

        service.start(resetResults = true)
        assertEquals(PrinterDiscoveryStatus.SCANNING, service.observe().value.status)

        engine.emit(
            DiscoveredPrinterCandidate(
                target = "TCP:192.168.0.12",
                deviceName = "TM-T20III",
                ipAddress = "192.168.0.12"
            )
        )
        engine.emit(
            DiscoveredPrinterCandidate(
                target = "TCP:192.168.0.12:8008",
                deviceName = "TM-T20III",
                ipAddress = "192.168.0.12"
            )
        )

        val scanningState = service.observe().value
        assertEquals(1, scanningState.printers.size)
        assertEquals("192.168.0.12", scanningState.printers.first().host)

        service.stop(markCompleted = true)
        assertEquals(PrinterDiscoveryStatus.COMPLETED, service.observe().value.status)
    }

    @Test
    fun discoveryService_resetResultsClearsPreviousPrintersBeforeRescan() = runBlocking {
        val engine = FakeDiscoveryEngine()
        val service = PrinterDiscoveryService(engine, FakeLogger())

        service.start(resetResults = true)
        engine.emit(
            DiscoveredPrinterCandidate(
                target = "TCP:192.168.0.12",
                deviceName = "TM-T20III",
                ipAddress = "192.168.0.12"
            )
        )

        assertEquals(1, service.observe().value.printers.size)

        service.start(resetResults = true)

        val restartedState = service.observe().value
        assertEquals(PrinterDiscoveryStatus.SCANNING, restartedState.status)
        assertTrue(restartedState.printers.isEmpty())
    }

    @Test
    fun onboardingState_canSaveRequiresSelectionInAutomaticMode() {
        val branch = PrinterBranchOption(
            code = "001",
            label = "Sucursal 1",
            billingPoints = listOf(PrinterBillingPointOption(code = "001", label = "Caja 1"))
        )

        val autoMissingSelection = PrinterOnboardingState(
            configMode = PrinterConfigMode.AUTOMATIC,
            branches = listOf(branch),
            selectedBranchCode = "001",
            selectedBillingPointCode = "001",
            printerName = "TM-T20III",
            host = "192.168.0.12",
            lastTestStatus = "success",
            discoveryState = PrinterDiscoveryState(
                status = PrinterDiscoveryStatus.COMPLETED,
                printers = emptyList(),
            ),
            selectedDiscoveredPrinterId = null,
        )

        assertFalse(autoMissingSelection.canSave)

        val discovered = DiscoveredPrinter(
            id = "192.168.0.12|tcp",
            displayName = "TM-T20III",
            target = "TCP:192.168.0.12",
            host = "192.168.0.12",
            port = 443,
            deviceType = 1,
        )
        val autoWithSelection = autoMissingSelection.copy(
            discoveryState = autoMissingSelection.discoveryState.copy(printers = listOf(discovered)),
            selectedDiscoveredPrinterId = discovered.id,
        )

        assertNotNull(autoWithSelection.selectedDiscoveredPrinter)
        assertTrue(autoWithSelection.canSave)
    }

    @Test
    fun toDiscoveredPrinter_prefersRealIpWhenTargetContainsMacAddress() {
        val candidate = DiscoveredPrinterCandidate(
            target = "64:C6:D2:FA:8D:70",
            deviceName = "TM-T20III",
            ipAddress = "192.168.0.12",
            macAddress = "64:C6:D2:FA:8D:70"
        )

        val discovered = candidate.toDiscoveredPrinter()

        assertNotNull(discovered)
        assertEquals("192.168.0.12", discovered.host)
        assertEquals("TCP:192.168.0.12", discovered.target)
        assertEquals("192.168.0.12", discovered.ipAddress)
    }

    private class FakeDiscoveryEngine(
        private val supported: Boolean = true,
    ) : PrinterDiscoveryEngine {
        private var callback: ((DiscoveredPrinterCandidate) -> Unit)? = null

        override fun isDiscoverySupported(): Boolean = supported

        override suspend fun start(onDiscovered: (DiscoveredPrinterCandidate) -> Unit) {
            callback = onDiscovered
        }

        override suspend fun stop() {
            callback = null
        }

        fun emit(candidate: DiscoveredPrinterCandidate) {
            callback?.invoke(candidate)
        }
    }

    private class FakeLogger : ILoggerService {
        override fun sendLog(log: Log) = Unit
    }
}
