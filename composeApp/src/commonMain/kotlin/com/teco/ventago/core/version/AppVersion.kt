package com.teco.ventago.core.version

data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<AppVersion> {
    val isEnabled: Boolean get() = this > ZERO

    override fun compareTo(other: AppVersion): Int {
        major.compareTo(other.major).let { if (it != 0) return it }
        minor.compareTo(other.minor).let { if (it != 0) return it }
        return patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        val ZERO = AppVersion(0, 0, 0)
    }
}

fun parseMarketingVersion(raw: String?): AppVersion? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    val core = trimmed.substringBefore('-').substringBefore('+')
    val parts = core.split('.')
    if (parts.isEmpty() || parts.size > 3) return null
    val numbers = parts.map { part ->
        if (part.isEmpty() || part.any { !it.isDigit() }) return null
        part.toIntOrNull() ?: return null
    }
    if (numbers.any { it < 0 }) return null
    if (parts.size == 1 && numbers[0] != 0) return null
    return AppVersion(
        major = numbers.getOrElse(0) { 0 },
        minor = numbers.getOrElse(1) { 0 },
        patch = numbers.getOrElse(2) { 0 },
    )
}
