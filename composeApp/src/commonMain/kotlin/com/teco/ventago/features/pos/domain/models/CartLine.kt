package com.teco.ventago.features.pos.domain.models

import com.teco.ventago.utils.multiplyCentsByQuantity
import com.teco.ventago.utils.normalizeQuantity

typealias Money = Long

private const val BPS_DENOMINATOR = 10_000L
private const val BPS_HALF_DENOMINATOR = BPS_DENOMINATOR / 2

/**
 * Applies [bps] (basis points) to a cents amount and rounds half-up to the nearest cent.
 *
 * Example: 35.96 (3596 cents) at 10% (1000 bps) => 3.596 => 3.60 (360 cents).
 */
private fun applyBpsRounded(amountCents: Long, bps: Int): Long {
    if (amountCents == 0L || bps == 0) return 0L
    val numerator = amountCents * bps.toLong()
    // Round half-up at the cent level (works for both positive/negative numerators).
    val adjusted = if (numerator >= 0L) numerator + BPS_HALF_DENOMINATOR else numerator - BPS_HALF_DENOMINATOR
    return adjusted / BPS_DENOMINATOR
}

data class CartLine(
    val lineId: String,               // unique per line (UUID or itemId+timestamp)
    val itemId: Int,
    val name: String,

    val baseUnitPrice: Money,         // from catalog
    val quantity: Double = 1.0,
    val overrideUnitPrice: Money? = null, // custom unit price (replaces baseUnitPrice)

    val discount: Discount? = null,   // per-unit discount (stored as per-unit value)
    val tax: Tax? = null,

    val notes: String? = null,

    val shippingCents: Money? = null,     // acarreo por ítem
    val insuranceCents: Money? = null,    // seguro por ítem

    val pharmaBatchNumber: String? = null,
    val pharmaBatchQty: Int? = null,

    val costCents: Money? = null,         // cost from catalog (cents), used for margin display
    val locationId: Int? = null,
) {
    fun unitPrice(): Money = overrideUnitPrice ?: baseUnitPrice
    fun normalizedQuantity(): Double = normalizeQuantity(quantity)

    /**
     * Returns the subtotal BEFORE discounts (unitPrice × quantity).
     * This is used for calculating totals and taxes.
     */
    fun lineSubtotalBeforeDiscount(): Money = multiplyCentsByQuantity(unitPrice(), normalizedQuantity())

    /**
     * Calculates the discount amount PER UNIT (not total).
     * For percentage: discountPerUnit = unitPrice × (percent / 100)
     * For fixed: discountPerUnit = min(discountValue, unitPrice)
     * All values are rounded to 2 decimal places (cents).
     */
    fun discountPerUnit(): Money = when (val d = discount) {
        null -> 0L
        is Discount.Amount -> {
            // Fixed discount per unit: cap at unit price, round to 2 decimals
            val unitPrice = unitPrice()
            val discountPerUnit = d.value.coerceAtMost(unitPrice).coerceAtLeast(0L)
            discountPerUnit // Already in cents, no rounding needed
        }
        is Discount.Percent -> {
            // Percentage discount per unit: unitPrice × (percent / 100)
            val unitPrice = unitPrice()
            applyBpsRounded(unitPrice, d.bps)
        }
    }

    /**
     * Returns the total discount amount (discountPerUnit × quantity).
     * This is the total discount applied to the line.
     */
    fun discountAmount(): Money {
        val perUnit = discountPerUnit()
        return multiplyCentsByQuantity(perUnit, normalizedQuantity())
    }

    /**
     * Returns the subtotal AFTER discounts.
     * Calculated as: (unitPrice - discountPerUnit) × quantity
     * All values are rounded to 2 decimal places.
     */
    fun lineSubtotal(): Money {
        val unitPrice = unitPrice()
        val discountPerUnit = discountPerUnit()
        val discountedUnitPrice = (unitPrice - discountPerUnit).coerceAtLeast(0L)
        return multiplyCentsByQuantity(discountedUnitPrice, normalizedQuantity())
    }

    fun lineCharges(): Long = (shippingCents ?: 0L) + (insuranceCents ?: 0L)

    fun taxTotal(taxExempt: Boolean): Money {
        if (taxExempt || tax == null) return 0L
        // Base for taxes is the subtotal after discount (per-unit discount applied)
        val base = lineSubtotal().coerceAtLeast(0L)
        val rateBps = tax.rateBps // basis points: 7% = 700, 10% = 1000
        return applyBpsRounded(base, rateBps)
    }

    fun total(orderTaxExempt: Boolean): Money =
        (lineSubtotal() + taxTotal(orderTaxExempt) + lineCharges()).coerceAtLeast(0L)
}
