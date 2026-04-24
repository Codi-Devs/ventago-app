package com.teco.ventago.features.orders.domain

import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.OrderPaymentLinkDto
import kotlinx.datetime.Instant

object PaymentLinkResolver {

    private val terminalStatuses = setOf("completed", "expired", "cancelled", "canceled")
    private val pendingStatuses = setOf(
        "pending",
        "pending_review",
        "requires_action",
        "requires_payment_method",
        "processing",
        "created"
    )

    data class ResolvedLink(
        val url: String,
        val status: String?,
        val source: String,
        val createdAt: String? = null,
    ) {
        private val normalizedStatus: String
            get() = status.orEmpty()
                .trim()
                .lowercase()
                .replace('-', '_')
                .replace(' ', '_')

        val isTerminal: Boolean
            get() = normalizedStatus in terminalStatuses

        val isPending: Boolean
            get() = normalizedStatus in pendingStatuses

        val isActive: Boolean
            get() = !isTerminal && !isPending
    }

    fun resolveCurrent(order: Order): ResolvedLink? {
        val paymentLinks = paymentLinksCandidates(order)
        val links = linksCandidates(order)
        val direct = directLegacyCandidate(order)

        // For action/display parity, prefer an active link if one exists.
        paymentLinks.mostRecentMatching { it.isActive }?.let { return it }
        if (direct?.isActive == true) return direct
        links.mostRecentMatching { it.isActive }?.let { return it }

        // Otherwise fallback to most-recent pending, then terminal.
        paymentLinks.mostRecentMatching { it.isPending }?.let { return it }
        links.mostRecentMatching { it.isPending }?.let { return it }
        paymentLinks.mostRecent()?.let { return it }
        links.mostRecent()?.let { return it }

        return direct
    }

    fun hasPendingLink(order: Order): Boolean {
        return paymentLinksCandidates(order).any { it.isPending } ||
            linksCandidates(order).any { it.isPending }
    }

    fun hasActiveLink(order: Order): Boolean {
        return paymentLinksCandidates(order).any { it.isActive } ||
            directLegacyCandidate(order)?.isActive == true ||
            linksCandidates(order).any { it.isActive }
    }

    fun hasOpenLink(order: Order): Boolean {
        return paymentLinksCandidates(order).any { !it.isTerminal } ||
            directLegacyCandidate(order)?.let { !it.isTerminal } == true ||
            linksCandidates(order).any { !it.isTerminal }
    }

    private fun paymentLinksCandidates(order: Order): List<ResolvedLink> {
        return order.paymentLinks
            .mapNotNull { dto -> dto.toResolved(source = "payment_links", preferLinkField = true) }
    }

    private fun linksCandidates(order: Order): List<ResolvedLink> {
        return order.links
            .mapNotNull { dto -> dto.toResolved(source = "links", preferLinkField = false) }
    }

    private fun directLegacyCandidate(order: Order): ResolvedLink? {
        return order.paymentLink
            ?.takeIf { it.isNotBlank() }
            ?.let { ResolvedLink(url = it, status = "active", source = "payment_link") }
    }

    private fun OrderPaymentLinkDto.toResolved(
        source: String,
        preferLinkField: Boolean
    ): ResolvedLink? {
        val candidate = if (preferLinkField) {
            link?.ifBlank { null } ?: url?.ifBlank { null }
        } else {
            url?.ifBlank { null } ?: link?.ifBlank { null }
        }
        return candidate?.let { safeUrl ->
            ResolvedLink(
                url = safeUrl,
                status = status,
                source = source,
                createdAt = createdAt
            )
        }
    }

    private fun List<ResolvedLink>.mostRecent(): ResolvedLink? {
        return this
            .sortedByDescending { candidate ->
                parseCreatedAtEpochSeconds(candidate.createdAt)
            }
            .firstOrNull()
    }

    private fun List<ResolvedLink>.mostRecentMatching(predicate: (ResolvedLink) -> Boolean): ResolvedLink? {
        return this
            .asSequence()
            .filter(predicate)
            .toList()
            .mostRecent()
    }

    private fun parseCreatedAtEpochSeconds(createdAt: String?): Long {
        if (createdAt.isNullOrBlank()) return Long.MIN_VALUE
        return runCatching { Instant.parse(createdAt).epochSeconds }
            .getOrDefault(Long.MIN_VALUE)
    }
}
