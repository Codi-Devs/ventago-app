package com.teco.ventago.features.orders.domain.models.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CreateOrderRequest(
    val invoice: Invoice,
    val branch: Branch,
    @SerialName("customer_id") val customerId: Long? = null,
    @SerialName("final_customer") val finalCustomer: Boolean? = null,
    @SerialName("final_customer_info") val finalCustomerInfo: FinalCustomerInfo? = null,
    @SerialName("third_parties") val thirdParties: List<ThirdParty> = emptyList(),
    @SerialName("items") val orderItems: List<OrderItem> = emptyList(),
    val totals: CreateOrderTotals,
    val payments: List<CreateOrderPayment> = emptyList(),
    val references: List<References>? = null,
    @SerialName("order_reference") val orderReference: OrderReference? = null,
    val retentions: Retentions? = null,
    val exportation: Exportation? = null,
    val logistics: Logistics? = null,
    @SerialName("delivery_location") val deliveryLocation: DeliveryLocation? = null,
    @SerialName("commercial_addenda") val commercialAddenda: CommercialAddenda? = null,
    val links: PaymentLinksBlock? = null,              // exclusive with payments
    val formats: List<String> = emptyList(),

    @SerialName("save_as") val saveAs: String = "confirmed"
)

// ================= Invoice / Branch =================

@Serializable
data class Invoice(
    val type: String,
    @SerialName("delivery_date") val deliveryDate: String? = null,
    @SerialName("operation_nature") val operationNature: String,
    @SerialName("operation_destination") val operationDestination: String,
    @SerialName("issuer_fe_additional_info") val issuerFeAdditionalInfo: String? = null,
    @SerialName("issued_datetime") val issuedDatetime: String? = null
)

@Serializable
data class Branch(
    val code: String,
    @SerialName("billing_point") val billingPoint: String
)

// =============== Final customer (optional) ===============

@Serializable
data class FinalCustomerInfo(
    val name: String? = null,
    @SerialName("identification_type") val identificationType: String? = null,
    @SerialName("identification_number") val identificationNumber: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    val email: String? = null
)

// ================= Third Parties =================

@Serializable
data class ThirdParty( // NO
    @SerialName("tax_id") val taxId: String,
    val dv: String? = null,
    @SerialName("tax_id_type") val taxIdType: String
)

// ================= Items =================

@Serializable
data class OrderItem(
    @SerialName("item_id") val itemId: Long,
    val code: String,
    val name: String,
    @SerialName("unit_measure") val unitMeasure: String,
    val quantity: Int,
    @SerialName("base_unit_price") val baseUnitPrice: String,
    @SerialName("override_unit_price") val overrideUnitPrice: String? = null,
    @SerialName("discounts") val orderItemDiscounts: List<OrderItemDiscount> = emptyList(),
    @SerialName("taxes")val orderItemTaxes: List<OrderItemTax> = emptyList(),
    @SerialName("additional_charges") val additionalCharges: List<Charge> = emptyList(),
    val totals: ItemTotals? = null,
    @SerialName("pharma_sale") val pharmaSale: PharmaSale? = null,
    @SerialName("vehicle_sale") val vehicleSale: VehicleSale? = null,
    @SerialName("additional_info") val additionalInfo: List<NameValue> = emptyList(),
    @SerialName("product_type") val productType: String? = null,

)

@Serializable
data class OrderItemDiscount(
    val amount: String
)

@Serializable
data class OrderItemTax(
    val code: String,
    val type: String,
    val description: String,
    val rate: String,
    val amount: String
)

@Serializable
data class Charge(
    val amount: String,
    val description: String
)

@Serializable
data class ItemTotals(
    @SerialName("before_discounts") val beforeDiscounts: String,
    @SerialName("after_discounts") val afterDiscounts: String,
    @SerialName("before_taxes") val beforeTaxes: String,
    val taxes: String,
    @SerialName("after_taxes") val afterTaxes: String,
    val total: String
)

@Serializable
data class PharmaSale(
    @SerialName("pharma_batch_number") val pharmaBatchNumber: String,
    @SerialName("pharma_batch_quantity") val pharmaBatchQuantity: Int
)

@Serializable
data class VehicleSale(
    @SerialName("sale_mode") val saleMode: String? = null,
    @SerialName("sale_mode_other") val saleModeOther: String? = null,
    @SerialName("chassis_number") val chassisNumber: String? = null,
    @SerialName("color_code") val colorCode: String? = null,
    @SerialName("color_description") val colorDescription: String? = null,
    @SerialName("engine_power_cv") val enginePowerCv: Int? = null,
    @SerialName("engine_capacity") val engineCapacity: Int? = null,
    @SerialName("net_weight") val netWeight: Int? = null,
    @SerialName("gross_weight") val grossWeight: Int? = null,
    @SerialName("fuel_type") val fuelType: String? = null,
    @SerialName("fuel_type_other") val fuelTypeOther: String? = null,
    @SerialName("engine_number") val engineNumber: String? = null,
    @SerialName("max_traction_capacity") val maxTractionCapacity: Int? = null,
    @SerialName("wheelbase_distance") val wheelbaseDistance: Double? = null,
    @SerialName("model_year") val modelYear: Int? = null,
    @SerialName("manufacture_year") val manufactureYear: Int? = null,
    @SerialName("paint_type") val paintType: String? = null,
    @SerialName("paint_type_other") val paintTypeOther: String? = null,
    val type: String? = null,
    val usage: String? = null,
    val condition: String? = null,
    @SerialName("max_passenger_capacity") val maxPassengerCapacity: Int? = null
)

@Serializable
data class NameValue(
    val name: String,
    val value: JsonElement // allows string/number as in your examples
)

// ================= Totals =================

@Serializable
data class CreateOrderTotals(
    @SerialName("quantity_items") val quantityItems: Int,
    val charges: List<InvoiceCharge> = emptyList(),
    val discounts: List<InvoiceDiscount> = emptyList(),
    val subtotal: String,
    @SerialName("total_before_discounts") val totalBeforeDiscounts: String,
    @SerialName("total_after_discounts") val totalAfterDiscounts: String,
    @SerialName("total_before_taxes") val totalBeforeTaxes: String,
    @SerialName("total_after_taxes") val totalAfterTaxes: String,
    @SerialName("total_taxes") val totalTaxes: String,
    @SerialName("invoice_total") val invoiceTotal: String
)

@Serializable
data class InvoiceCharge(
    val amount: String,
    val type: String
)

@Serializable
data class InvoiceDiscount(
    val description: String,
    val amount: String
)

// ================= Payments =================

@Serializable
data class CreateOrderPayment(
    val type: Int,
    val description: String? = null,
    val amount: String,
    @SerialName("due_date") val dueDate: String? = null // ISO string
)

// ================= References / Retentions / OrderRef =================

@Serializable
data class References(
    @SerialName("legal_name") val legalName: String,
    @SerialName("issue_datetime") val issueDatetime: String,
    @SerialName("reference_number") val referenceNumber: ReferenceNumber
)

@Serializable
data class ReferenceNumber(
    val type: String,   // "cufe" | "paper" | "fiscal_document"
    val number: String
)

@Serializable
data class Retentions(
    val code: String,
    @SerialName("amount") val rate: String
)

@Serializable
data class OrderReference( // NO
    @SerialName("purchase_order_number") val purchaseOrderNumber: String? = null,
    @SerialName("acceptance_number") val acceptanceNumber: String? = null,
    @SerialName("customer_receiver_code") val customerReceiverCode: String? = null,
    @SerialName("issuer_system_code") val issuerSystemCode: String? = null,
    @SerialName("issuer_global_purchase_order_info") val issuerGlobalPurchaseOrderInfo: String? = null
)

// ================= Export / Logistics / Delivery =================

@Serializable
data class Exportation(
    val incoterm: String? = null,
    val currency: String? = null,
    @SerialName("port_of_loading") val portOfLoading: String? = null
)

@Serializable
data class Logistics(
    @SerialName("total_packages_number") val totalPackagesNumber: Int? = null,
    @SerialName("total_cargo_weight") val totalCargoWeight: Double? = null,
    @SerialName("total_weight_unit") val totalWeightUnit: String? = null,
    @SerialName("cargo_vehicle_license") val cargoVehicleLicense: String? = null,
    @SerialName("carrier_legal_name") val carrierLegalName: String? = null,
    @SerialName("carrier_taxpayer_type") val carrierTaxpayerType: String? = null,
    @SerialName("carrier_tax_id") val carrierTaxId: String? = null,
    @SerialName("carrier_tax_dv") val carrierTaxDv: String? = null,
    @SerialName("issuer_logistics_additional_info") val issuerLogisticsAdditionalInfo: String? = null
)

@Serializable
data class DeliveryLocation(
    @SerialName("receiver_taxpayer_type") val receiverTaxpayerType: String? = null,
    @SerialName("receiver_tax_id") val receiverTaxId: String? = null,
    @SerialName("receiver_tax_dv") val receiverTaxDv: String? = null,
    @SerialName("location_code") val locationCode: String? = null,
    @SerialName("receiver_legal_name") val receiverLegalName: String? = null,
    @SerialName("contact_phone") val contactPhone: String? = null,
    @SerialName("alternate_contact_phone") val alternateContactPhone: String? = null
)

// ================= Addenda =================

@Serializable
data class CommercialAddenda(
    val code: String,
    val type: String,
    val version: String,
    @SerialName("validate_internal_reference") val validateInternalReference: String,
    @SerialName("invoice_notes") val invoiceNotes: AddendaNotes,
    val items: List<AddendaItem> = emptyList()
)

@Serializable
data class AddendaNotes(
    val observations: String,
    @SerialName("amount_in_words") val amountInWords: String,
    @SerialName("custom_fields") val customFields: Map<String, JsonElement>? = null
)

@Serializable
data class AddendaItem(
    @SerialName("line_number") val lineNumber: Int,
    @SerialName("extra_description") val extraDescription: String,
    @SerialName("ean_code") val eanCode: String,
    @SerialName("extra_category") val extraCategory: String,
    val texts: List<String> = emptyList()
)

// ================= Payment Links (exclusive with payments) =================

@Serializable
data class PaymentLinksBlock(
    val create: Boolean,
    @SerialName("expire_in_minutes") val expireInMinutes: Int,
    val note: String? = null,
    val method: String // "LINK" | "YAPPY QR"
)
