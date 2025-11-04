package com.teco.ventago.features.product.domain.model

data class ItemTax(
    val name: String,
    val value: Int
) {
    companion object {
        val defaultTaxes = listOf(
            ItemTax("0% (Exonerado)", 0),
            ItemTax("7% (Regular)", 7),
            ItemTax("10% (Alcohol y derivados)", 10),
            ItemTax("15% (Cigarrillos)", 15),
        )
    }
}



