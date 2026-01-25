package com.teco.ventago.features.product.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class Item (
    val itemId: Int,
    val barcode: String?,
    val sku: String?,
    val name: String,
    val description: String,
    val img: String,
    val price: Double,
    val cost: Double?,
    var active: Boolean,
    var order: Int,
    val taxPercent: Int?,
    val productType: ProductType,

    // === New fields ===
    val unitMeasureCode: String = "und",
    val iscRate: Double? = null,
    val otiTaxes: List<OTITax>? = null,
    val isPharma: Boolean = false,
    val additionalInfo: JsonObject? = null,
){

    fun getImgUrl(): String? {
        if (img.isBlank()) {
            return null
        }

        val aux = img.replace("null", "")
        if (aux.isBlank()) {
            return null
        }
        return img
    }

    constructor(response: JsonObject) :this(
        itemId = response["idItem"]?.jsonPrimitive?.int ?: -1,
        barcode = response["barcode"]?.jsonPrimitive?.content,
        sku = response["sku"]?.jsonPrimitive?.content,
        name = response["name"]?.jsonPrimitive?.content ?: "",
        description = response["descripcion"]?.jsonPrimitive?.content ?: "",
        img = response["img"]?.jsonPrimitive?.content ?: "",
        price = response["price"]?.jsonPrimitive?.double ?: 0.0,
        cost = response["cost"]?.jsonPrimitive?.double,
        active = (response["active"]?.jsonPrimitive?.int ?: 0) == 1,
        order = response["order"]?.jsonPrimitive?.int ?: 0,
        taxPercent = response["tax_percent"]?.jsonPrimitive?.int,
        productType = ProductType.fromId(response["product_type"]?.jsonPrimitive?.int ?: 1),

        unitMeasureCode = response["unit_measure_code"]?.jsonPrimitive?.content
            ?: response["unitMeasureCode"]?.jsonPrimitive?.content ?: "und",
        iscRate = response["isc_rate"]?.jsonPrimitive?.doubleOrNull
            ?: response["iscRate"]?.jsonPrimitive?.doubleOrNull,
        otiTaxes = response["oti_taxes"]?.jsonArray?.map {
            val obj = it.jsonObject
            OTITax(
                id = obj["id"]?.jsonPrimitive?.content ?: "",
                rate = obj["rate"]?.jsonPrimitive?.double ?: 0.0
            )
        },
        isPharma = when {
            response["is_pharma"]?.jsonPrimitive != null ->
                (response["is_pharma"]!!.jsonPrimitive.intOrNull ?: 0) == 1
            response["isPharma"]?.jsonPrimitive != null ->
                (response["isPharma"]!!.jsonPrimitive.intOrNull ?: 0) == 1
            else -> false
        },
        additionalInfo = response["additional_info"] as? JsonObject
            ?: response["additionalInfo"] as? JsonObject
    )

    val asJSONObject: JsonObject?
        get() {
            var item: JsonObject?
            val data = mutableMapOf<String, JsonElement>()
            try {
                data["idItem"] = JsonPrimitive(itemId)
                data["barcode"] = JsonPrimitive(barcode ?: "")
                data["sku"] = JsonPrimitive(sku ?: "")
                data["name"] = JsonPrimitive(name)
                data["descripcion"] = JsonPrimitive(description)
                data["img"] = JsonPrimitive(img)
                data["allergens"] = JsonPrimitive("")
                data["price"] = JsonPrimitive(price)
                data["cost"] = JsonPrimitive(cost ?: 0.0)
                data["taxPercent"] = JsonPrimitive(taxPercent ?: 0)
                data["active"] = JsonPrimitive(if (active) 1 else 0)
                data["order"] = JsonPrimitive(order)
                data["product_type"] = JsonPrimitive(productType.typeId)
                data["unit_measure_code"] = JsonPrimitive(unitMeasureCode)
                iscRate?.let { data["isc_rate"] = JsonPrimitive(it) }
                otiTaxes?.let { taxes ->
                    data["oti_taxes"] = buildJsonArray {
                        taxes.forEach { tax ->
                            add(buildJsonObject {
                                put("id", tax.id)
                                put("rate", tax.rate)
                            })
                        }
                    }
                }
                data["is_pharma"] = JsonPrimitive(if (isPharma) 1 else 0)
                additionalInfo?.let { data["additional_info"] = it }
                item = JsonObject(data)
            } catch (e: Exception) {
                e.printStackTrace()
                item = null
            }
            return item
        }

    companion object {

        fun listFromMap(arr: JsonArray) : List<Item> {
            val list = mutableListOf<Item>()
            for (item in arr) {
                val map = Json.decodeFromJsonElement<Map<String, JsonElement>>(item)
                val element = fromMap(map)
                list.add(element)
            }
            return list
        }

        fun newItemFromJson(json: JsonObject): Item {
            return Item(
                itemId = json["id_item"]?.jsonPrimitive?.int ?: -1,
                barcode = json["barcode"]?.jsonPrimitive?.content,
                sku = json["sku"]?.jsonPrimitive?.content,
                name = json["name"]?.jsonPrimitive?.content ?: "",
                description = if ((json["descripcion"]?.jsonPrimitive?.content ?: "") != "")
                    json["descripcion"]?.jsonPrimitive?.content ?: ""
                else
                    json["desc"]?.jsonPrimitive?.content ?: "",
                img = json["img"]?.jsonPrimitive?.content ?: "",
                price = json["price"]?.jsonPrimitive?.double ?: 0.0,
                cost = json["cost"]?.jsonPrimitive?.double,
                active = (json["active"]?.jsonPrimitive?.int ?: 0) == 1,
                order = json["order"]?.jsonPrimitive?.int ?: 0,
                taxPercent = json["tax_percent"]?.jsonPrimitive?.int,
                productType = ProductType.fromId(json["product_type"]?.jsonPrimitive?.int ?: 1),
                unitMeasureCode = json["unit_measure_code"]?.jsonPrimitive?.content ?: "und",
                iscRate = json["isc_rate"]?.jsonPrimitive?.doubleOrNull,
                otiTaxes = json["oti_taxes"]?.jsonArray?.map {
                    val obj = it.jsonObject
                    OTITax(
                        id = obj["id"]?.jsonPrimitive?.content ?: "",
                        rate = obj["rate"]?.jsonPrimitive?.double ?: 0.0
                    )
                },
                isPharma = (json["is_pharma"]?.jsonPrimitive?.intOrNull ?: 0) == 1,
                additionalInfo = json["additional_info"] as? JsonObject
            )
        }

        private fun fromMap(map: Map<String, JsonElement>): Item {
            return Item(
                itemId = map["idItem"]?.jsonPrimitive?.int ?: -1,
                barcode = map["barcode"]?.jsonPrimitive?.content,
                sku = map["sku"]?.jsonPrimitive?.content,
                name = map["name"]?.jsonPrimitive?.content ?: "",
                description = map["descripcion"]?.jsonPrimitive?.content ?: "",
                img = map["img"]?.jsonPrimitive?.content ?: "",
                price = map["price"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                cost = map["cost"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                active = (map["active"]?.jsonPrimitive?.intOrNull ?: 0) == 1,
                order = map["order"]?.jsonPrimitive?.intOrNull ?: 0,
                taxPercent = map["tax_percent"]?.jsonPrimitive?.int,
                productType = ProductType.fromId(map["product_type"]?.jsonPrimitive?.int ?: 1),
                unitMeasureCode = map["unit_measure_code"]?.jsonPrimitive?.content
                    ?: map["unitMeasureCode"]?.jsonPrimitive?.content ?: "und",
                iscRate = map["isc_rate"]?.jsonPrimitive?.doubleOrNull
                    ?: map["iscRate"]?.jsonPrimitive?.doubleOrNull,
                otiTaxes = (map["oti_taxes"] as? JsonArray)?.map {
                    val obj = it.jsonObject
                    OTITax(
                        id = obj["id"]?.jsonPrimitive?.content ?: "",
                        rate = obj["rate"]?.jsonPrimitive?.double ?: 0.0
                    )
                },
                isPharma = when {
                    map["is_pharma"]?.jsonPrimitive != null ->
                        (map["is_pharma"]!!.jsonPrimitive.intOrNull ?: 0) == 1
                    map["isPharma"]?.jsonPrimitive != null ->
                        (map["isPharma"]!!.jsonPrimitive.intOrNull ?: 0) == 1
                    else -> false
                },
                additionalInfo = map["additional_info"] as? JsonObject
                    ?: map["additionalInfo"] as? JsonObject
            )
        }
    }
}


@Serializable
enum class AdditionalValueType { STRING, NUMBER, DATE }

/**
 * Canonical keys for Item.additional_info[].name
 */
@Serializable
enum class AdditionalInfoKey(
    val keyName: String,                 // value to send in JSON "name"
    val valueType: AdditionalValueType,  // helps UI validation
    val title: String                    // human label
)
{
    PANAMA_GOODS_SERVICES_CODE(
        keyName = "panama_goods_services_code",
        valueType = AdditionalValueType.STRING,
        title = "Código bienes/servicios (Panamá)"
    ),

    PANAMA_GOODS_SERVICES_UNIT_CODE(
        keyName = "panama_goods_services_unit_code",
        valueType = AdditionalValueType.STRING,
        title = "Unidad bienes/servicios (Panamá)"
    ),

    FABRICATION_DATE(
        keyName = "fabrication_date",
        valueType = AdditionalValueType.DATE, // "AAAA-MM-DD"
        title = "Fecha de fabricación"
    ),

    EXPIRATION_DATE(
        keyName = "expiration_date",
        valueType = AdditionalValueType.DATE, // "AAAA-MM-DD"
        title = "Fecha de expiración"
    ),

    ISSUER_ITEM_FE_ADDITIONAL_INFO(
        keyName = "issuer_item_fe_additional_info",
        valueType = AdditionalValueType.STRING,
        title = "Info adicional del emisor"
    ),

    ITEM_INSURANCE_PRICE(
        keyName = "item_insurance_price",
        valueType = AdditionalValueType.NUMBER,
        title = "Precio de seguro del ítem"
    );

    companion object {
        fun fromKey(keyName: String): AdditionalInfoKey? {
            return entries.find { it.keyName == keyName }
        }
    }

}

val AdditionalInfoCatalog = AdditionalInfoKey.entries

/** Quick lookup by key string (e.g., when editing existing items). */
val AdditionalInfoKeyIndex: Map<String, AdditionalInfoKey> =
    AdditionalInfoKey.entries.associateBy { it.keyName }
