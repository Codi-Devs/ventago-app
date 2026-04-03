package com.teco.ventago.features.printers.domain

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.printers.domain.model.PrinterDiscoveryState
import com.teco.ventago.features.printers.domain.model.PrinterDiscoveryStatus
import com.teco.ventago.features.printers.domain.model.discoveryDedupeKey
import com.teco.ventago.features.printers.domain.model.toDiscoveredPrinter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PrinterDiscoveryService(
    private val engine: PrinterDiscoveryEngine,
    private val logger: ILoggerService,
) {
    private val state = MutableStateFlow(PrinterDiscoveryState())
    private val controlMutex = Mutex()
    private var running = false

    fun observe(): StateFlow<PrinterDiscoveryState> = state.asStateFlow()

    suspend fun start(resetResults: Boolean = true) {
        controlMutex.withLock {
            if (!engine.isDiscoverySupported()) {
                state.value = PrinterDiscoveryState(
                    status = PrinterDiscoveryStatus.UNSUPPORTED,
                    printers = if (resetResults) emptyList() else state.value.printers,
                    message = "La búsqueda automática no está disponible en este dispositivo. Usa el modo manual.",
                )
                return
            }

            val existingPrinters = if (resetResults) emptyList() else state.value.printers
            runCatching { engine.stop() }

            state.value = PrinterDiscoveryState(
                status = PrinterDiscoveryStatus.SCANNING,
                printers = existingPrinters,
                message = null,
            )

            runCatching {
                engine.start discoveryCallback@{ candidate ->
                    val discovered = candidate.toDiscoveredPrinter() ?: return@discoveryCallback
                    state.update { current ->
                        val updated = current.printers.toMutableList()
                        val dedupeKey = discoveryDedupeKey(discovered)
                        val index = updated.indexOfFirst {
                            discoveryDedupeKey(it) == dedupeKey ||
                                it.target.equals(discovered.target, ignoreCase = true)
                        }
                        if (index >= 0) {
                            updated[index] = discovered
                        } else {
                            updated += discovered
                        }

                        current.copy(
                            printers = updated.sortedBy { it.displayName.lowercase() },
                            message = null,
                        )
                    }
                }
                running = true
            }.onFailure { error ->
                running = false
                logger.sendLog(
                    Log(
                        level = LogLevel.ERROR,
                        flow = "PrinterDiscoveryService::start",
                        message = error.message ?: "UNKNOWN",
                    )
                )
                state.value = state.value.copy(
                    status = PrinterDiscoveryStatus.ERROR,
                    message = "No se pudo iniciar la búsqueda automática.",
                )
            }
        }
    }

    suspend fun stop(markCompleted: Boolean = true) {
        controlMutex.withLock {
            if (!running && state.value.status != PrinterDiscoveryStatus.SCANNING) return

            runCatching { engine.stop() }
                .onFailure { error ->
                    logger.sendLog(
                        Log(
                            level = LogLevel.WARNING,
                            flow = "PrinterDiscoveryService::stop",
                            message = error.message ?: "UNKNOWN",
                        )
                    )
                }

            running = false
            val current = state.value
            if (current.status == PrinterDiscoveryStatus.SCANNING) {
                state.value = if (markCompleted) {
                    current.copy(
                        status = PrinterDiscoveryStatus.COMPLETED,
                        message = if (current.printers.isEmpty()) {
                            "No encontramos impresoras en la red. Puedes reintentar o usar el modo manual."
                        } else {
                            null
                        }
                    )
                } else {
                    current.copy(status = PrinterDiscoveryStatus.IDLE)
                }
            }
        }
    }

    suspend fun clear() {
        controlMutex.withLock {
            runCatching { engine.stop() }
            running = false
            state.value = PrinterDiscoveryState()
        }
    }
}
