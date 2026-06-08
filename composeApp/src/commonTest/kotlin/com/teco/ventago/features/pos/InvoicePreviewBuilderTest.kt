package com.teco.ventago.features.pos

import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.business.domain.model.BusinessAddress
import com.teco.ventago.features.business.domain.model.BusinessSocialNetwork
import com.teco.ventago.features.business.domain.model.Currency
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.pos.domain.models.CartLine
import com.teco.ventago.features.pos.domain.models.Discount
import com.teco.ventago.features.pos.domain.models.Tax
import com.teco.ventago.design_system.molecules.pos.GlobalDiscountMode
import com.teco.ventago.features.pos.ui.invoice_preview.InvoicePreviewBuilder
import com.teco.ventago.features.pos.ui.invoice_preview.resolveDocumentLogo
import com.teco.ventago.features.pos.ui.viewmodel.InstallmentUI
import com.teco.ventago.features.pos.ui.viewmodel.PosState
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.OTITax
import com.teco.ventago.features.product.domain.model.ProductType
import com.teco.ventago.features.invoicing.domain.models.BottomNoteSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonObject

class InvoicePreviewBuilderTest {

    @Test
    fun finalConsumerUsesFallbackNameAndCfIdentification() {
        val preview = InvoicePreviewBuilder.build(
            state = baseState(
                finalCustomer = true,
                finalName = null,
                finalIdNumber = null,
            ),
            business = business(),
            issuedAt = LocalDateTime(2026, 5, 5, 12, 46),
        )

        assertEquals("Consumidor Final", preview.receptor.type)
        assertEquals("CONSUMIDOR FINAL", preview.receptor.name)
        assertEquals("CF", preview.receptor.identification)
        assertEquals("05-05-2026 12:46", preview.meta.issuedAt)
    }

    @Test
    fun registeredCustomerUsesSelectedNameAndRuc() {
        val preview = InvoicePreviewBuilder.build(
            state = baseState(
                finalCustomer = false,
                customer = CustomerListItem(
                    id = 12,
                    name = "Oscar Ernesto",
                    email = "oscar@example.com",
                    ruc = "8-888-8456",
                    status = 1,
                    invoiceCustomer = 1,
                    updatedAt = 0,
                ),
            ),
            business = business(),
        )

        assertEquals("Contribuyente", preview.receptor.type)
        assertEquals("Oscar Ernesto", preview.receptor.name)
        assertEquals("8-888-8456", preview.receptor.identification)
    }

    @Test
    fun groupsItbmsByRateWithoutDoubleCountingExemptBase() {
        val taxableLine = CartLine(
            lineId = "line-1",
            itemId = 1,
            name = "Correo empresarial",
            baseUnitPrice = 1000,
            quantity = 2.0,
            tax = Tax(id = 7, name = "ITBMS", rateBps = 700),
        )
        val exemptLine = CartLine(
            lineId = "line-2",
            itemId = 2,
            name = "Servicio exento",
            baseUnitPrice = 500,
            quantity = 1.0,
            tax = null,
        )

        val preview = InvoicePreviewBuilder.build(
            state = baseState(
                items = listOf(
                    item(id = 1, name = "Correo empresarial", taxPercent = 7),
                    item(id = 2, name = "Servicio exento", taxPercent = 0),
                ),
                cart = listOf(taxableLine, exemptLine),
            ),
            business = business(),
        )

        assertEquals(2, preview.itbmsBreakdown.size)
        assertEquals("Exento", preview.itbmsBreakdown[0].rateLabel)
        assertEquals(500, preview.itbmsBreakdown[0].baseCents)
        assertEquals(0, preview.itbmsBreakdown[0].taxCents)
        assertEquals("7%", preview.itbmsBreakdown[1].rateLabel)
        assertEquals(2000, preview.itbmsBreakdown[1].baseCents)
        assertEquals(140, preview.itbmsBreakdown[1].taxCents)
        assertEquals(500, preview.totals.exemptItbmsCents)
        assertEquals(2000, preview.totals.taxableItbmsCents)
        assertEquals(140, preview.totals.itbmsCents)
        assertEquals(140, preview.totals.totalTaxCents)
    }

    @Test
    fun rendersManualPaymentsAndInstallments() {
        val preview = InvoicePreviewBuilder.build(
            state = baseState(
                charged = mapOf(2 to 500, 8 to 700),
                installments = listOf(InstallmentUI(amountCents = 300, dueDateIso = "2026-06-05")),
            ),
            business = business(),
        )

        assertEquals(3, preview.payments.size)
        assertEquals("Efectivo", preview.payments[0].label)
        assertEquals(500, preview.payments[0].amountCents)
        assertEquals("Transferencia bancaria", preview.payments[1].label)
        assertEquals(700, preview.payments[1].amountCents)
        assertEquals("Credito/Plazo 1 - 2026-06-05", preview.payments[2].label)
        assertEquals(300, preview.payments[2].amountCents)
    }

    @Test
    fun includesRetentionAppliedOverItbmsAmount() {
        val preview = InvoicePreviewBuilder.build(
            state = baseState(
                cart = listOf(
                    CartLine(
                        lineId = "line-1",
                        itemId = 1,
                        name = "Correo empresarial",
                        baseUnitPrice = 857,
                        quantity = 1.0,
                        tax = Tax(id = 7, name = "ITBMS", rateBps = 700),
                    )
                ),
                retentionCodeIndex = 2,
            ),
            business = business(),
        )

        assertEquals(60, preview.totals.itbmsCents)
        assertEquals("Pago por venta de bienes/servicios al estado 50%", preview.retention?.label)
        assertEquals(30, preview.retention?.amountCents)
        assertEquals(917, preview.totals.totalCents)

        val withoutRetention = InvoicePreviewBuilder.build(
            state = baseState(),
            business = business(),
        )

        assertEquals(null, withoutRetention.retention)
    }

    @Test
    fun includesDiscountsAndGlobalChargesOnlyWhenPositive() {
        val preview = InvoicePreviewBuilder.build(
            state = baseState(
                cart = listOf(
                    CartLine(
                        lineId = "line-1",
                        itemId = 1,
                        name = "Servicio",
                        baseUnitPrice = 2000,
                        quantity = 1.0,
                        discount = Discount.Amount(100),
                        tax = Tax(id = 7, name = "ITBMS", rateBps = 700),
                    )
                ),
                globalShippingCents = 250,
                globalInsuranceCents = 150,
                globalOtherChargesCents = 75,
                globalDiscountMode = GlobalDiscountMode.FIXED,
                globalDiscountFixedCents = 200,
            ),
            business = business(),
        )

        assertEquals(200, preview.totals.discountCents)
        assertEquals(250, preview.totals.freightCents)
        assertEquals(150, preview.totals.insuranceCents)
        assertEquals(75, preview.totals.otherChargesCents)
        assertEquals(2308, preview.totals.totalCents)
    }

    @Test
    fun branchLogoTakesPriorityOverBusinessLogoInPreview() {
        val preview = InvoicePreviewBuilder.build(
            state = baseState(
                branches = listOf(branch(logoUrl = "https://cdn.example.com/branch-logo.jpg")),
            ),
            business = business(logo = "https://cdn.example.com/business-logo.jpg"),
        )

        assertEquals("https://cdn.example.com/branch-logo.jpg", preview.issuer.logoUrl)
        assertEquals("VentaGo Centro", preview.branch?.tradeName)
    }

    @Test
    fun documentLogoFallsBackToBusinessLogoThenEmpty() {
        assertEquals(
            "https://cdn.example.com/business-logo.jpg",
            resolveDocumentLogo(
                branch = branch(logoUrl = null),
                business = business(logo = "https://cdn.example.com/business-logo.jpg"),
            )
        )
        assertEquals(
            "",
            resolveDocumentLogo(
                branch = branch(logoUrl = "null"),
                business = business(logo = "null"),
            )
        )
    }

    @Test
    fun includesBottomNoteOnlyWhenSelectedAndConfigured() {
        val preview = InvoicePreviewBuilder.build(
            state = baseState(
                bottomNoteSettings = BottomNoteSettings(
                    title = "Información de pago",
                    body = "<ol><li><strong>Notas</strong>:</li><li>Enviar comprobante.</li></ol><blockquote>Gracias</blockquote>",
                    includeOnInvoice = true,
                ),
                includeBottomNote = true,
            ),
            business = business(),
        )

        assertEquals("Información de pago", preview.bottomNote?.title)
        assertEquals("- Notas:\n- Enviar comprobante.\nGracias", preview.bottomNote?.body)

        val excludedPreview = InvoicePreviewBuilder.build(
            state = baseState(
                bottomNoteSettings = BottomNoteSettings(
                    title = "Información de pago",
                    body = "<p>Enviar comprobante.</p>",
                    includeOnInvoice = true,
                ),
                includeBottomNote = false,
            ),
            business = business(),
        )

        assertEquals(null, excludedPreview.bottomNote)
    }

    private fun baseState(
        finalCustomer: Boolean? = true,
        finalName: String? = null,
        finalIdNumber: String? = null,
        customer: CustomerListItem? = null,
        items: List<Item> = listOf(item()),
        cart: List<CartLine> = listOf(
            CartLine(
                lineId = "line-1",
                itemId = 1,
                name = "Correo empresarial",
                baseUnitPrice = 1000,
                quantity = 1.0,
                tax = Tax(id = 7, name = "ITBMS", rateBps = 700),
            )
        ),
        charged: Map<Int, Long> = emptyMap(),
        installments: List<InstallmentUI> = emptyList(),
        branches: List<Branch> = emptyList(),
        globalShippingCents: Long? = null,
        globalInsuranceCents: Long? = null,
        globalOtherChargesCents: Long? = null,
        globalDiscountMode: GlobalDiscountMode = GlobalDiscountMode.NONE,
        globalDiscountFixedCents: Long = 0L,
        bottomNoteSettings: BottomNoteSettings? = null,
        includeBottomNote: Boolean? = null,
        retentionCodeIndex: Int = 0,
        retentionAmount: String = "",
    ): PosState {
        return PosState(
            items = items,
            cart = cart,
            finalCustomer = finalCustomer,
            finalName = finalName,
            finalIdNumber = finalIdNumber,
            customer = customer,
            branches = branches,
            billingPoints = listOf(FiscalBillingPoint("001", "Principal", 1)),
            charged = charged,
            installments = installments,
            globalShippingCents = globalShippingCents,
            globalInsuranceCents = globalInsuranceCents,
            globalOtherChargesCents = globalOtherChargesCents,
            globalDiscountMode = globalDiscountMode,
            globalDiscountFixedCents = globalDiscountFixedCents,
            bottomNoteSettings = bottomNoteSettings,
            includeBottomNote = includeBottomNote,
            retentionCodeIndex = retentionCodeIndex,
            retentionAmount = retentionAmount,
        )
    }

    private fun branch(logoUrl: String?): Branch {
        return Branch(
            branchCode = "0000",
            name = "Sucursal Principal",
            addressLine = "Calle 50",
            locationCode = "",
            longitude = "",
            latitude = "",
            status = 1,
            fiscalBillingPoints = emptyList(),
            tradeName = "VentaGo Centro",
            logoUrl = logoUrl,
        )
    }

    private fun item(
        id: Int = 1,
        name: String = "Correo empresarial",
        taxPercent: Int? = 7,
        iscRate: Double? = null,
        otiTaxes: List<OTITax>? = null,
    ): Item {
        return Item(
            itemId = id,
            barcode = "000$id",
            sku = null,
            name = name,
            description = "Descripcion $name",
            img = "",
            price = 10.0,
            cost = null,
            active = true,
            order = 0,
            taxPercent = taxPercent,
            productType = ProductType.SERVICE,
            unitMeasureCode = "UND",
            iscRate = iscRate,
            otiTaxes = otiTaxes,
        )
    }

    private fun business(logo: String = ""): Business {
        return Business(
            businessId = 1,
            name = "TECO MARK, S.A.",
            description = "",
            active = true,
            currency = Currency(140, "US Dollar", "USD", "$"),
            logo = logo,
            socialNetwork = BusinessSocialNetwork(JsonObject(emptyMap())),
            phone = "",
            address = BusinessAddress("", "Panama", 0.0, 0.0),
            domain = "",
            isFull = true,
            ruc = "155746999-2-2024",
            web = null,
            businessEmail = null,
        )
    }
}
