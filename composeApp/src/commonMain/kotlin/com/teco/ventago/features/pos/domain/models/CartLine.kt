package com.teco.ventago.features.pos.domain.models

typealias Money = Long

data class CartLine(
    val lineId: String,               // unique per line (UUID or itemId+timestamp)
    val itemId: Int,
    val name: String,

    val baseUnitPrice: Money,         // from catalog
    val quantity: Int = 1,
    val overrideUnitPrice: Money? = null, // custom unit price (replaces baseUnitPrice)

    val discount: Discount? = null,   // per-line discount applied on (unitPrice * qty)
    val tax: Tax? = null,

    val notes: String? = null,

    val shippingCents: Money? = null,     // acarreo por ítem
    val insuranceCents: Money? = null,    // seguro por ítem

    val pharmaBatchNumber: String? = null,
    val pharmaBatchQty: Int? = null,
) {
    fun unitPrice(): Money = overrideUnitPrice ?: baseUnitPrice

    fun lineSubtotal(): Money = unitPrice() * quantity

    fun discountAmount(): Money = when (val d = discount) {
        null -> 0L
        is Discount.Amount  -> d.value.coerceAtMost(lineSubtotal()).coerceAtLeast(0L)
        is Discount.Percent -> (lineSubtotal() * d.bps / 10_000L)
    }

    fun lineCharges(): Long = (shippingCents ?: 0L) + (insuranceCents ?: 0L)

    fun taxTotal(taxExempt: Boolean): Money {
        if (taxExempt || tax == null) return 0L
        val base = (lineSubtotal() - discountAmount()).coerceAtLeast(0L)
        val rateBps = tax.rateBps // e.g., 700 for 7.00% if your model stores basis points
        return (base * rateBps) / 10_000L
    }


    fun total(orderTaxExempt: Boolean): Money =
        (lineSubtotal() - discountAmount() + taxTotal(orderTaxExempt) + lineCharges()).coerceAtLeast(0L)
}