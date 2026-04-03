package com.teco.ventago.features.printers.domain

import com.teco.ventago.features.printers.domain.model.PrintCommand
import com.teco.ventago.features.printers.domain.model.PrintContext
import com.teco.ventago.features.printers.domain.model.PrintResult
import com.teco.ventago.features.printers.domain.model.PrinterConfig

interface PrinterEngine {
    suspend fun execute(
        printerConfig: PrinterConfig,
        commands: List<PrintCommand>,
        context: PrintContext,
        maxAttempts: Int = printerConfig.retryCount + 1,
    ): PrintResult
}

