package com.teco.ventago.features.product.domain.model

enum class ProductType(val typeId: Int, val description: String, val code: String) {
    GOOD(1, "Bien", "good"),
    SERVICE(2, "Servicio", "service");

    companion object {
        fun fromId(typeId: Int): ProductType {
            return when (typeId) {
                1 -> GOOD
                2 -> SERVICE
                else -> SERVICE
            }
        }

        fun toList(): List<ProductType> {
            return listOf(GOOD, SERVICE)
        }
    }
}