package com.teco.ventago.features.printers.domain.model

enum class PrinterDiscoveryStatus {
    IDLE,
    SCANNING,
    COMPLETED,
    ERROR,
    UNSUPPORTED,
}

data class PrinterDiscoveryState(
    val status: PrinterDiscoveryStatus = PrinterDiscoveryStatus.IDLE,
    val printers: List<DiscoveredPrinter> = emptyList(),
    val message: String? = null,
)

data class DiscoveredPrinterCandidate(
    val target: String? = null,
    val deviceName: String? = null,
    val ipAddress: String? = null,
    val macAddress: String? = null,
    val deviceType: Int? = null,
)

data class DiscoveredPrinter(
    val id: String,
    val displayName: String,
    val target: String,
    val host: String,
    val port: Int,
    val deviceType: Int,
    val ipAddress: String? = null,
    val macAddress: String? = null,
)

fun DiscoveredPrinterCandidate.toDiscoveredPrinter(defaultPort: Int = DEFAULT_DISCOVERY_PORT): DiscoveredPrinter? {
    val parsed = parseDiscoveryTarget(target, defaultPort)
    val parsedHost = parsed.host?.trim()?.takeUnless { it.isBlank() }?.takeUnless { it.isMacAddressLike() }
    val candidateIpAddress = ipAddress?.trim()?.takeUnless { it.isBlank() }?.takeUnless { it.isMacAddressLike() }
    val host = parsedHost ?: candidateIpAddress ?: return null

    val resolvedPort = parsed.port ?: defaultPort
    val resolvedScheme = parsed.scheme ?: "TCP"
    val normalizedTarget = buildDiscoveryTarget(
        scheme = resolvedScheme,
        host = host,
        port = resolvedPort,
        defaultPort = defaultPort,
    )

    val normalizedHost = host.normalizeDiscoveredHost()
    if (normalizedHost.isBlank()) return null

    val resolvedDisplayName = deviceName?.trim().takeUnless { it.isNullOrBlank() } ?: normalizedHost
    val resolvedIpAddress = candidateIpAddress
    val resolvedMacAddress = macAddress?.trim()?.takeUnless { it.isBlank() }

    return DiscoveredPrinter(
        id = discoveryIdentity(normalizedHost, normalizedTarget),
        displayName = resolvedDisplayName,
        target = normalizedTarget,
        host = normalizedHost,
        port = resolvedPort,
        deviceType = deviceType ?: -1,
        ipAddress = resolvedIpAddress,
        macAddress = resolvedMacAddress,
    )
}

fun discoveryDedupeKey(printer: DiscoveredPrinter): String {
    return printer.ipAddress?.trim()?.lowercase().takeUnless { it.isNullOrBlank() }
        ?: printer.host.trim().lowercase()
}

data class ParsedDiscoveryTarget(
    val scheme: String?,
    val host: String?,
    val port: Int?,
    val target: String?,
)

fun parseDiscoveryTarget(rawTarget: String?, defaultPort: Int = DEFAULT_DISCOVERY_PORT): ParsedDiscoveryTarget {
    if (rawTarget.isNullOrBlank()) {
        return ParsedDiscoveryTarget(
            scheme = null,
            host = null,
            port = null,
            target = null,
        )
    }

    val trimmed = rawTarget.trim()
    val explicitScheme = trimmed.substringBefore(':', "").uppercase().takeIf {
        it == "TCP" || it == "TCPS"
    }

    var payload = when {
        explicitScheme != null -> trimmed.substringAfter(':').trim()
        trimmed.startsWith("tcp://", ignoreCase = true) -> trimmed.removePrefix("tcp://")
        trimmed.startsWith("tcps://", ignoreCase = true) -> trimmed.removePrefix("tcps://")
        else -> trimmed
    }

    payload = payload.substringBefore('/').substringBefore('?').substringBefore('#').trim()

    if (payload.isBlank()) {
        return ParsedDiscoveryTarget(
            scheme = explicitScheme,
            host = null,
            port = null,
            target = trimmed,
        )
    }

    val host: String
    val port: Int?

    if (payload.startsWith('[') && payload.contains(']')) {
        host = payload.substringAfter('[').substringBefore(']').trim()
        val remainder = payload.substringAfter(']', "").trim()
        port = remainder.removePrefix(":").toIntOrNull()
    } else {
        val colonCount = payload.count { it == ':' }
        if (colonCount == 1) {
            host = payload.substringBefore(':').trim()
            port = payload.substringAfter(':').trim().toIntOrNull()
        } else {
            host = payload.trim()
            port = null
        }
    }

    val normalizedHost = host.normalizeDiscoveredHost()
    val normalizedPort = port ?: when (explicitScheme) {
        "TCP", "TCPS" -> defaultPort
        else -> null
    }
    val scheme = explicitScheme ?: "TCP"

    val normalizedTarget = if (normalizedHost.isBlank()) {
        trimmed
    } else {
        buildDiscoveryTarget(
            scheme = scheme,
            host = normalizedHost,
            port = normalizedPort,
            defaultPort = defaultPort,
        )
    }

    return ParsedDiscoveryTarget(
        scheme = scheme,
        host = normalizedHost.takeUnless { it.isBlank() },
        port = normalizedPort,
        target = normalizedTarget,
    )
}

private fun buildDiscoveryTarget(
    scheme: String,
    host: String,
    port: Int?,
    defaultPort: Int,
): String {
    return if (port == null || port == defaultPort) {
        "$scheme:$host"
    } else {
        "$scheme:$host:$port"
    }
}

private fun String.normalizeDiscoveredHost(): String {
    val candidate = trim().removeSuffix(":").trim()
    if (candidate.isBlank()) return ""

    if (candidate.startsWith('[') && candidate.contains(']')) {
        return candidate.substringAfter('[').substringBefore(']').trim()
    }

    return candidate
}

private fun String.isMacAddressLike(): Boolean {
    val value = trim()
    val macRegex = Regex("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$")
    return macRegex.matches(value)
}

private fun discoveryIdentity(host: String, target: String): String {
    return "$host|${target.lowercase()}"
}

const val DEFAULT_DISCOVERY_PORT = 443
