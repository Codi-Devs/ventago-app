package com.teco.ventago.features.printers.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PrinterConfig(
    val id: Int? = null,
    @SerialName("business_id") val businessId: Int? = null,
    @SerialName("branch_code") val branchCode: String,
    @SerialName("billing_point_code") val billingPointCode: String,
    @SerialName("printer_brand") val printerBrand: String = "epson",
    @SerialName("printer_model") val printerModel: String,
    @SerialName("integration_type") val integrationType: String = "epson_epos",
    val host: String,
    val port: Int = 443,
    @SerialName("device_id") val deviceId: String = "local_printer",
    @SerialName("paper_width_mm") val paperWidthMm: Int = 80,
    @SerialName("supports_cutter") val supportsCutter: Boolean = true,
    @SerialName("timeout_ms") val timeoutMs: Int = 30_000,
    @SerialName("retry_count") val retryCount: Int = 1,
    @SerialName("print_by_default") val printByDefault: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("last_test_status") val lastTestStatus: String = "not_tested",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun hasEndpoint(): Boolean = normalizedHost().isNotBlank()

    fun isInternalDevice(): Boolean =
        integrationType.trim().equals(PRINTER_INTEGRATION_H10P_INTERNAL, ignoreCase = true)

    fun isAvailableForPrint(): Boolean = isActive && (isInternalDevice() || hasEndpoint())

    fun connectionTarget(): String = "TCP:${normalizedHost()}"

    fun connectionTargets(): List<String> {
        val normalizedHost = normalizedHost()
        if (normalizedHost.isBlank()) {
            return emptyList()
        }

        val effectivePort = normalizedPort()
        return when {
            effectivePort == 443 -> listOf(
                "TCP:$normalizedHost",
                "TCPS:$normalizedHost"
            )
            effectivePort != null && effectivePort != DEFAULT_RAW_TCP_PORT -> listOf(
                "TCP:$normalizedHost:$effectivePort",
                "TCP:$normalizedHost"
            )
            else -> listOf("TCP:$normalizedHost")
        }.distinct()
    }

    fun requiresRetestComparedTo(previous: PrinterConfig?): Boolean {
        if (previous == null) return true
        return previous.isInternalDevice() != isInternalDevice() ||
            previous.normalizedHost() != normalizedHost() ||
            previous.normalizedPort() != normalizedPort() ||
            previous.printerModel.trim() != printerModel.trim() ||
            previous.integrationType.trim() != integrationType.trim()
    }

    private fun normalizedHost(): String = host.normalizePrinterHost()

    private fun normalizedPort(): Int? {
        val hostPort = host.extractInlinePort()
        return when {
            port > 0 && port != DEFAULT_PRINTER_PORT -> port
            hostPort != null -> hostPort
            port > 0 -> port
            else -> null
        }
    }
}

private const val DEFAULT_PRINTER_PORT = 443
private const val DEFAULT_RAW_TCP_PORT = 9100
const val PRINTER_INTEGRATION_EPSON_EPOS = "epson_epos"
const val PRINTER_INTEGRATION_H10P_INTERNAL = "h10p_internal"

private fun String.normalizePrinterHost(): String {
    if (isBlank()) return ""

    var candidate = trim()
    candidate = candidate.removePrefix("tcp://")
    candidate = candidate.removePrefix("tcps://")
    candidate = candidate.removePrefix("http://")
    candidate = candidate.removePrefix("https://")

    if (candidate.startsWith("TCP:", ignoreCase = true)) {
        candidate = candidate.substringAfter(':')
    } else if (candidate.startsWith("TCPS:", ignoreCase = true)) {
        candidate = candidate.substringAfter(':')
    }

    candidate = candidate.substringBefore('/')
    candidate = candidate.substringBefore('?')
    candidate = candidate.substringBefore('#')
    candidate = candidate.trim()

    if (candidate.startsWith('[') && candidate.contains(']')) {
        return candidate.substringAfter('[').substringBefore(']')
    }

    val colonCount = candidate.count { it == ':' }
    if (colonCount == 1) {
        val hostPart = candidate.substringBefore(':').trim()
        if (hostPart.isNotBlank()) {
            return hostPart
        }
    }

    return candidate.removeSuffix(":").trim()
}

private fun String.extractInlinePort(): Int? {
    var candidate = trim()
    candidate = candidate.removePrefix("tcp://")
    candidate = candidate.removePrefix("tcps://")
    candidate = candidate.removePrefix("http://")
    candidate = candidate.removePrefix("https://")

    if (candidate.startsWith("TCP:", ignoreCase = true)) {
        candidate = candidate.substringAfter(':')
    } else if (candidate.startsWith("TCPS:", ignoreCase = true)) {
        candidate = candidate.substringAfter(':')
    }

    candidate = candidate.substringBefore('/')
    candidate = candidate.substringBefore('?')
    candidate = candidate.substringBefore('#')
    candidate = candidate.trim()

    if (candidate.startsWith('[') && candidate.contains(']')) {
        val remainder = candidate.substringAfter(']').trim()
        return remainder.removePrefix(":").toIntOrNull()
    }

    val colonCount = candidate.count { it == ':' }
    if (colonCount != 1) return null

    return candidate.substringAfter(':').trim().toIntOrNull()
}

@Serializable
data class CreatePrinterRequest(
    @SerialName("branch_code") val branchCode: String,
    @SerialName("billing_point_code") val billingPointCode: String,
    @SerialName("printer_brand") val printerBrand: String = "epson",
    @SerialName("printer_model") val printerModel: String,
    @SerialName("integration_type") val integrationType: String = "epson_epos",
    val host: String,
    val port: Int = 443,
    @SerialName("device_id") val deviceId: String = "local_printer",
    @SerialName("paper_width_mm") val paperWidthMm: Int = 80,
    @SerialName("supports_cutter") val supportsCutter: Boolean = true,
    @SerialName("timeout_ms") val timeoutMs: Int = 30_000,
    @SerialName("retry_count") val retryCount: Int = 1,
    @SerialName("print_by_default") val printByDefault: Boolean = true,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("last_test_status") val lastTestStatus: String = "not_tested",
)

@Serializable
data class UpdatePrinterRequest(
    @SerialName("printer_brand") val printerBrand: String? = null,
    @SerialName("printer_model") val printerModel: String? = null,
    @SerialName("integration_type") val integrationType: String? = null,
    val host: String? = null,
    val port: Int? = null,
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("paper_width_mm") val paperWidthMm: Int? = null,
    @SerialName("supports_cutter") val supportsCutter: Boolean? = null,
    @SerialName("timeout_ms") val timeoutMs: Int? = null,
    @SerialName("retry_count") val retryCount: Int? = null,
    @SerialName("print_by_default") val printByDefault: Boolean? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("last_test_status") val lastTestStatus: String? = null,
)

data class PrinterListFilters(
    val branchCode: String? = null,
    val billingPointCode: String? = null,
    val isActive: Boolean? = null,
    val printByDefault: Boolean? = null,
)

data class PrinterSelectionOption(
    val printerConfig: PrinterConfig,
    val displayLabel: String,
)

data class ReprintTicketState(
    val businessId: Int,
    val orderId: Int,
    val orderNumber: String,
    val ticketLayout: TicketLayout? = null,
)
