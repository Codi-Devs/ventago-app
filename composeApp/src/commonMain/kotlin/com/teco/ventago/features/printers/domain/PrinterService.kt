package com.teco.ventago.features.printers.domain

import com.teco.ventago.AppDistribution
import com.teco.ventago.core.LocalStorage
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.printers.data.repository.IPrinterRepository
import com.teco.ventago.features.printers.domain.model.CreatePrinterRequest
import com.teco.ventago.features.printers.domain.model.PrintCommand
import com.teco.ventago.features.printers.domain.model.PrintContext
import com.teco.ventago.features.printers.domain.model.PrintResult
import com.teco.ventago.features.printers.domain.model.PrintResultContext
import com.teco.ventago.features.printers.domain.model.PRINTER_INTEGRATION_H10P_INTERNAL
import com.teco.ventago.features.printers.domain.model.PrinterConfig
import com.teco.ventago.features.printers.domain.model.PrinterListFilters
import com.teco.ventago.features.printers.domain.model.PrinterSelectionOption
import com.teco.ventago.features.printers.domain.model.ReprintTicketState
import com.teco.ventago.features.printers.domain.model.TicketDocumentPayload
import com.teco.ventago.features.printers.domain.model.TicketBlockStyle
import com.teco.ventago.features.printers.domain.model.TicketLayout
import com.teco.ventago.features.printers.domain.model.UpdatePrinterRequest
import com.teco.ventago.features.printers.domain.model.NARROW_PAPER_COLUMNS
import com.teco.ventago.features.printers.domain.model.isNarrowPaperWidth
import com.teco.ventago.features.printers.domain.model.toPaperColumns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PrinterService(
    private val repository: IPrinterRepository,
    private val engine: PrinterEngine,
    private val businessService: BusinessService,
    private val storage: LocalStorage,
    private val logger: ILoggerService,
    private val cacheSyncService: PrinterCacheSyncService,
    private val json: Json,
    private val appScope: CoroutineScope,
    private val appDistribution: AppDistribution,
) {
    private val state = MutableStateFlow<List<PrinterConfig>>(emptyList())
    private val refreshMutex = Mutex()
    private var currentBusinessId: Int? = null
    private val parser = TicketLayoutParser()

    init {
        businessService.getBusiness().onEach { business ->
            val businessId = business?.businessId
            if (businessId == null) {
                clear()
            } else if (currentBusinessId != businessId) {
                initialize(businessId)
            }
        }.launchIn(appScope)
    }

    fun observe(): StateFlow<List<PrinterConfig>> = state.asStateFlow()

    fun initialize(businessId: Int) {
        if (currentBusinessId == businessId) return
        currentBusinessId = businessId
        appScope.launch(Dispatchers.IO) {
            loadCachedPrinters(businessId)?.let { state.value = it }
            if (state.value.isEmpty() || isCacheExpired(businessId)) {
                runCatching { refresh(businessId, force = true) }
            }
            cacheSyncService.start(businessId) { refresh(it, force = true) }
        }
    }

    suspend fun refresh(businessId: Int = requireBusinessId(), force: Boolean = false): Boolean {
        return refreshMutex.withLock {
            val printers = repository.listPrinters(businessId)
            state.value = printers
            saveCache(businessId, printers)
            if (!force) {
                storage.set(PrinterCacheSyncService.tokenKey(businessId), storage.string(PrinterCacheSyncService.tokenKey(businessId)) ?: "")
            }
            true
        }
    }

    suspend fun createPrinter(request: CreatePrinterRequest): PrinterConfig {
        val businessId = requireBusinessId()
        val printer = repository.createPrinter(businessId, request)
        updateLocalPrinters { current ->
            current.filterNot {
                it.branchCode == printer.branchCode && it.billingPointCode == printer.billingPointCode
            } + printer
        }
        return printer
    }

    suspend fun updatePrinter(branchCode: String, billingPointCode: String, request: UpdatePrinterRequest): PrinterConfig {
        val businessId = requireBusinessId()
        val printer = repository.updatePrinter(businessId, branchCode, billingPointCode, request)
        updateLocalPrinters { current ->
            current.map {
                if (it.branchCode == branchCode && it.billingPointCode == billingPointCode) printer else it
            }
        }
        return printer
    }

    suspend fun deletePrinter(branchCode: String, billingPointCode: String): Boolean {
        val businessId = requireBusinessId()
        val deleted = repository.deletePrinter(businessId, branchCode, billingPointCode)
        if (deleted) {
            updateLocalPrinters { current ->
                current.filterNot { it.branchCode == branchCode && it.billingPointCode == billingPointCode }
            }
        }
        return deleted
    }

    suspend fun getPrinter(branchCode: String, billingPointCode: String): PrinterConfig {
        val businessId = requireBusinessId()
        return repository.getPrinter(businessId, branchCode, billingPointCode)
    }

    fun getCachedPrinters(): List<PrinterConfig> = state.value

    fun hasPersistedPrinterConfig(): Boolean = state.value.isNotEmpty()

    fun resolveActivePrinter(branchCode: String, billingPointCode: String): PrinterConfig? {
        return state.value.firstOrNull {
            it.branchCode == branchCode &&
                it.billingPointCode == billingPointCode &&
                it.isAvailableForPrint()
        } ?: syntheticH10pPrinter(branchCode, billingPointCode)
    }

    fun resolveActivePrinters(): List<PrinterConfig> {
        return state.value.filter { it.isAvailableForPrint() }
    }

    fun resolveSelectionOptions(): List<PrinterSelectionOption> {
        val printers = resolveActivePrinters().ifEmpty {
            syntheticH10pPrinter(branchCode = "", billingPointCode = "")?.let(::listOf).orEmpty()
        }
        return printers.map { printer ->
            PrinterSelectionOption(
                printerConfig = printer,
                displayLabel = if (printer.isInternalDevice()) {
                    printer.printerModel
                } else {
                    "${printer.printerModel} - ${printer.branchCode} - ${printer.billingPointCode}"
                }
            )
        }
    }

    fun shouldRequestTicket(branchCode: String, billingPointCode: String): Boolean {
        return resolveActivePrinter(branchCode, billingPointCode) != null
    }

    private fun syntheticH10pPrinter(branchCode: String, billingPointCode: String): PrinterConfig? {
        if (!appDistribution.isPosBuild) return null
        return PrinterConfig(
            branchCode = branchCode,
            billingPointCode = billingPointCode,
            printerBrand = "h10p",
            printerModel = "H10P Internal",
            integrationType = PRINTER_INTEGRATION_H10P_INTERNAL,
            host = "",
            port = 0,
            deviceId = "h10p_internal",
            paperWidthMm = 57,
            supportsCutter = false,
            timeoutMs = 10_000,
            retryCount = 1,
            printByDefault = true,
            isActive = true,
            lastTestStatus = "not_tested",
        )
    }

    suspend fun parseTicketLayout(payload: TicketDocumentPayload): TicketLayout = parser.parse(payload)

    suspend fun fetchOrderTicketLayout(orderId: Int, businessId: Int? = null): TicketDocumentPayload {
        val resolvedBusinessId = businessId ?: requireBusinessId()
        return repository.getOrderTicketLayout(resolvedBusinessId, orderId)
    }

    suspend fun reprintTicket(
        reprintState: ReprintTicketState,
        printerConfig: PrinterConfig,
    ): PrintResult {
        val layout = reprintState.ticketLayout
            ?: throw IllegalStateException("ReprintTicketState.ticketLayout es obligatorio para imprimir")
        return printLayout(
            printerConfig = printerConfig,
            layout = layout,
            context = PrintContext(
                source = "order_details_reprint",
                orderId = reprintState.orderId
            )
        )
    }

    suspend fun printTicketPayload(
        printerConfig: PrinterConfig,
        ticketPayload: TicketDocumentPayload,
        context: PrintContext,
    ): PrintResult {
        return printLayout(printerConfig, parser.parse(ticketPayload), context)
    }

    suspend fun printLayout(
        printerConfig: PrinterConfig,
        layout: TicketLayout,
        context: PrintContext,
    ): PrintResult {
        val commands = parser.toCommands(layout, printerConfig)
        return engine.execute(
            printerConfig = printerConfig,
            commands = commands,
            context = context,
            maxAttempts = printerConfig.retryCount + 1
        )
    }

    suspend fun testPrint(printerConfig: PrinterConfig): PrintResult {
        val paperColumns = printerConfig.paperWidthMm.toPaperColumns()
        val isNarrowPaper = printerConfig.paperWidthMm.isNarrowPaperWidth()
        val contentColumns = if (isNarrowPaper) {
            NARROW_TEST_PRINT_COLUMNS
        } else {
            paperColumns
        }
        val titleStyle = if (isNarrowPaper) {
            TicketBlockStyle(bold = true)
        } else {
            TicketBlockStyle(
                bold = true,
                doubleWidth = true,
                doubleHeight = true
            )
        }
        val titleColumns = if (titleStyle.doubleWidth) {
            (contentColumns / 2).coerceAtLeast(1)
        } else {
            contentColumns
        }

        val commands = mutableListOf<PrintCommand>()
        wrapTextForColumns("VentaGo", titleColumns).forEach { line ->
            commands += PrintCommand.Text(
                text = line,
                alignment = com.teco.ventago.features.printers.domain.model.PrintAlignment.CENTER,
                style = titleStyle
            )
        }
        wrapTextForColumns("Prueba de impresora exitosa", titleColumns).forEach { line ->
            commands += PrintCommand.Text(
                text = line,
                alignment = com.teco.ventago.features.printers.domain.model.PrintAlignment.CENTER,
                style = titleStyle
            )
        }
        commands += PrintCommand.Feed(2)

        listOf(
            "Modelo: ${printerConfig.printerModel}",
            "IP: ${printerConfig.host}"
        ).forEach { value ->
            wrapTextForColumns(value, contentColumns).forEach { line ->
                commands += PrintCommand.Text(
                    text = line,
                    alignment = com.teco.ventago.features.printers.domain.model.PrintAlignment.LEFT
                )
            }
        }

        commands += PrintCommand.Feed(3)
        commands += PrintCommand.Cut()

        return engine.execute(
            printerConfig = printerConfig,
            commands = commands,
            context = PrintContext(source = "printer_test"),
            maxAttempts = printerConfig.retryCount + 1
        )
    }

    fun clear() {
        currentBusinessId = null
        state.value = emptyList()
        cacheSyncService.stop()
    }

    private suspend fun updateLocalPrinters(transform: (List<PrinterConfig>) -> List<PrinterConfig>) {
        val businessId = requireBusinessId()
        val updated = transform(state.value).sortedBy { "${it.branchCode}-${it.billingPointCode}" }
        state.value = updated
        saveCache(businessId, updated)
    }

    private fun loadCachedPrinters(businessId: Int): List<PrinterConfig>? {
        val raw = storage.string(cachePayloadKey(businessId)) ?: return null
        return runCatching { json.decodeFromString<List<PrinterConfig>>(raw) }
            .onFailure {
                logger.sendLog(
                    Log(
                        LogLevel.WARNING,
                        "PrinterService",
                        "Printer cache decode failed. businessId=$businessId error=${it.message ?: "UNKNOWN"}"
                    )
                )
            }
            .getOrNull()
    }

    private fun saveCache(businessId: Int, printers: List<PrinterConfig>) {
        runCatching {
            storage.set(cachePayloadKey(businessId), json.encodeToString(printers))
            storage.set(cacheTimestampKey(businessId), Clock.System.now().toEpochMilliseconds())
        }.onFailure {
            logger.sendLog(
                Log(
                    LogLevel.WARNING,
                    "PrinterService",
                    "Printer cache save failed. businessId=$businessId error=${it.message ?: "UNKNOWN"}"
                )
            )
        }
    }

    private fun isCacheExpired(businessId: Int): Boolean {
        val updatedAt = storage.long(cacheTimestampKey(businessId)) ?: return true
        val ageMs = Clock.System.now().toEpochMilliseconds() - updatedAt
        return ageMs >= CACHE_TTL_MS
    }

    private fun requireBusinessId(): Int {
        return currentBusinessId ?: businessService.business.value?.businessId
        ?: throw IllegalStateException("No hay un negocio activo para impresoras")
    }

    companion object {
        private const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L
        private const val NARROW_TEST_PRINT_COLUMNS = 24

        fun cachePayloadKey(businessId: Int): String = "cache:printers:$businessId"
        fun cacheTimestampKey(businessId: Int): String = "cache:printers:$businessId:ts"
    }
}

private fun wrapTextForColumns(text: String, width: Int): List<String> {
    val safeWidth = width.coerceAtLeast(1)
    val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
    return normalized.split('\n').flatMap { line ->
        val content = line.trimEnd()
        if (content.length <= safeWidth) {
            listOf(content)
        } else {
            buildList {
                var remaining = content
                while (remaining.isNotEmpty()) {
                    if (remaining.length <= safeWidth) {
                        add(remaining)
                        remaining = ""
                    } else {
                        val breakpoint = remaining.take(safeWidth + 1).lastIndexOf(' ').takeIf { it > 0 } ?: safeWidth
                        add(remaining.substring(0, breakpoint).trimEnd())
                        remaining = remaining.substring(breakpoint).trimStart()
                    }
                }
            }
        }
    }
}
