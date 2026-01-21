package com.teco.ventago.features.quotes.domain

import com.teco.ventago.features.orders.domain.models.requests.Charge
import com.teco.ventago.features.orders.domain.models.requests.InvoiceCharge
import com.teco.ventago.features.orders.domain.models.requests.InvoiceDiscount
import com.teco.ventago.features.orders.domain.models.requests.ItemTotals
import com.teco.ventago.features.orders.domain.models.requests.NameValue
import com.teco.ventago.features.orders.domain.models.requests.OrderItem
import com.teco.ventago.features.orders.domain.models.requests.OrderItemDiscount
import com.teco.ventago.features.orders.domain.models.requests.OrderItemTax
import com.teco.ventago.features.pos.domain.models.CartLine
import com.teco.ventago.features.pos.domain.models.Discount
import com.teco.ventago.features.pos.ui.viewmodel.CartCalc
import com.teco.ventago.features.pos.ui.viewmodel.PosState
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.quotes.domain.models.requests.CreateQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.QuoteFinalCustomerInfo
import com.teco.ventago.features.quotes.domain.models.requests.QuoteTotalsRequest
import com.teco.ventago.utils.toDecimalString
import kotlinx.serialization.json.JsonElement

/**
 * Builds Create/Update quote requests from POS state, matching the JS buildCreateQuoteRequest logic.
 */
object QuoteRequestBuilder {
    private const val DEFAULT_QUOTE_BRANCH_CODE = "0000"

    fun build(
        state: PosState,
        quoteStyle: String = "style1",
        expiryDate: String? = null,
        additionalInfo: String? = null,
        quoteId: Long? = null
    ): CreateQuoteRequest {
        val itemsById: Map<Int, Item> = state.items.associateBy { it.itemId }

        val cartItems = state.cart.map { line ->
            val product = resolveProductForLine(itemsById, state.personalizedItems, line)

            val orderItemDiscounts = mutableListOf<OrderItemDiscount>()
            val discountPerUnit = line.discountPerUnit()
            if (discountPerUnit > 0) {
                orderItemDiscounts.add(OrderItemDiscount(amount = discountPerUnit.toDecimalString()))
            }

            val itemTaxes = mutableListOf<OrderItemTax>()
            if (state.taxExempt) {
                itemTaxes.add(
                    OrderItemTax(
                        code = "00",
                        type = "ITBMS",
                        description = "ITBMS",
                        rate = "0.00",
                        amount = "0.00"
                    )
                )
            } else {
                val taxPercent = product?.taxPercent ?: line.tax?.rateBps?.div(100) ?: 0
                val (itbmsCode, itbmsRate) = when (taxPercent) {
                    7 -> "01" to "0.07"
                    10 -> "02" to "0.10"
                    15 -> "03" to "0.15"
                    else -> "00" to "0.00"
                }
                val itbmsAmount = line.taxTotal(state.taxExempt)
                itemTaxes.add(
                    OrderItemTax(
                        code = itbmsCode,
                        type = "ITBMS",
                        description = "ITBMS",
                        rate = itbmsRate,
                        amount = itbmsAmount.toDecimalString()
                    )
                )

                product?.iscRate?.let { rate ->
                    if (rate > 0.0) {
                        val base = line.lineSubtotal().coerceAtLeast(0L)
                        val iscAmount = ((base * rate) / 100.0).toLong()
                        itemTaxes.add(
                            OrderItemTax(
                                code = "",
                                type = "ISC",
                                description = "ISC",
                                rate = rate.toString(),
                                amount = iscAmount.toDecimalString()
                            )
                        )
                    }
                }

                product?.otiTaxes?.forEach { oti ->
                    val base = line.lineSubtotal().coerceAtLeast(0L)
                    val otiAmount = ((base * oti.rate) / 1.0).toLong()
                    itemTaxes.add(
                        OrderItemTax(
                            code = oti.id,
                            type = "OTI",
                            description = "OTI",
                            rate = oti.rate.toString(),
                            amount = otiAmount.toDecimalString()
                        )
                    )
                }
            }

            val additionalCharges = mutableListOf<Charge>()
            if ((line.shippingCents ?: 0L) > 0L) {
                additionalCharges.add(Charge(description = "FACTURA", amount = (line.shippingCents ?: 0L).toDecimalString()))
            }
            if ((line.insuranceCents ?: 0L) > 0L) {
                additionalCharges.add(Charge(description = "SEGURO", amount = (line.insuranceCents ?: 0L).toDecimalString()))
            }

            val additionalInfoList = mutableListOf<NameValue>()
            product?.additionalInfo?.entries?.forEach { entry ->
                additionalInfoList.add(NameValue(name = entry.key, value = entry.value as JsonElement))
            }

            OrderItem(
                itemId = if (line.itemId > 0) line.itemId.toLong() else 0,
                code = product?.barcode ?: "0001",
                name = line.name,
                unitMeasure = product?.unitMeasureCode ?: "und",
                quantity = line.quantity,
                baseUnitPrice = line.unitPrice().toDecimalString(),
                overrideUnitPrice = line.overrideUnitPrice?.toDecimalString(),
                orderItemDiscounts = orderItemDiscounts,
                orderItemTaxes = itemTaxes,
                additionalCharges = additionalCharges,
                totals = ItemTotals(
                    beforeDiscounts = line.lineSubtotalBeforeDiscount().toDecimalString(),
                    afterDiscounts = line.lineSubtotal().toDecimalString(),
                    beforeTaxes = line.lineSubtotal().toDecimalString(),
                    taxes = line.taxTotal(state.taxExempt).toDecimalString(),
                    afterTaxes = (line.lineSubtotal() + line.taxTotal(state.taxExempt)).toDecimalString(),
                    total = line.total(state.taxExempt).toDecimalString()
                ),
                pharmaSale = if (product?.isPharma == true) {
                    com.teco.ventago.features.orders.domain.models.requests.PharmaSale(
                        pharmaBatchNumber = line.pharmaBatchNumber ?: "",
                        pharmaBatchQuantity = line.pharmaBatchQty ?: 0
                    )
                } else null,
                vehicleSale = null,
                additionalInfo = additionalInfoList,
                productType = product?.productType?.code
            )
        }

        val summary = CartCalc.summarize(state)
        val charges = mutableListOf<InvoiceCharge>()
        val hasItemFreight = state.cart.any { (it.shippingCents ?: 0L) > 0L }
        val hasItemInsurance = state.cart.any { (it.insuranceCents ?: 0L) > 0L }
        state.globalShippingCents?.let {
            if (it > 0 && !hasItemFreight) {
                charges.add(InvoiceCharge(type = "FACTURA", amount = it.toDecimalString()))
            }
        }
        state.globalInsuranceCents?.let {
            if (it > 0 && !hasItemInsurance) {
                charges.add(InvoiceCharge(type = "SEGURO", amount = it.toDecimalString()))
            }
        }
        state.globalOtherChargesCents?.let {
            if (it > 0) {
                charges.add(InvoiceCharge(type = "OTROS_GASTOS", amount = it.toDecimalString()))
            }
        }

        val discounts = mutableListOf<InvoiceDiscount>()
        if (summary.globalDiscount > 0) {
            discounts.add(InvoiceDiscount(description = "Descuento factura", amount = summary.globalDiscount.toDecimalString()))
        }

        val totals = QuoteTotalsRequest(
            quantityItems = state.cart.sumOf { it.quantity },
            charges = charges,
            discounts = discounts,
            subtotal = summary.subtotal.toDecimalString(),
            totalBeforeDiscounts = summary.subtotal.toDecimalString(),
            totalAfterDiscounts = (summary.subtotal - summary.lineDiscounts - summary.globalDiscount).toDecimalString(),
            totalBeforeTaxes = (summary.subtotal - summary.lineDiscounts - summary.globalDiscount).toDecimalString(),
            totalAfterTaxes = summary.totalBeforeTip.toDecimalString(),
            totalTaxes = summary.tax.toDecimalString(),
            invoiceTotal = summary.totalBeforeTip.toDecimalString()
        )

        val isFinalCustomer = requireNotNull(state.finalCustomer) { "Customer type not selected" }

        val finalCustomerInfo = if (isFinalCustomer) {
            QuoteFinalCustomerInfo(
                name = state.finalName,
                email = state.finalEmail,
                phone = state.finalPhone,
                idType = state.finalIdType,
                idNumber = state.finalIdNumber,
                country = state.finalPassportCountry
            )
        } else null

        val branchCode = state.branches.getOrNull(state.selectedBranchIndex)?.branchCode
            ?: DEFAULT_QUOTE_BRANCH_CODE

        return CreateQuoteRequest(
            branchCode = branchCode,
            customerId = if (isFinalCustomer) null else state.customer?.id,
            finalCustomer = isFinalCustomer,
            finalCustomerInfo = finalCustomerInfo,
            quoteStyle = quoteStyle,
            expiryDate = expiryDate,
            items = cartItems,
            totals = totals,
            includePaymentButton = false,
            additionalInfo = additionalInfo,
            quoteId = quoteId
        )
    }

    private fun resolveProductForLine(items: Map<Int, Item>, personalized: Map<String, Item>, line: CartLine): Item? {
        return if (line.itemId < 0) {
            personalized[line.lineId]
        } else {
            items[line.itemId]
        }
    }
}
