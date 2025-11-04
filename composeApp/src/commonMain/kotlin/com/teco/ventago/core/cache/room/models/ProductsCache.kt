package com.teco.ventago.core.cache.room.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.teco.ventago.features.product.domain.model.Products
import kotlinx.serialization.json.Json

@Entity
data class ProductsCache (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 1L,
    var productsJson: String,
)

fun Products.toCache(): ProductsCache? {
    val jsonObject = this.asJSONObject
    return jsonObject?.let {
        ProductsCache(
            id = 1L,
            productsJson = jsonObject.toString()
        )
    }
}

fun ProductsCache.toObject(): Products {
    return Products(Json.decodeFromString(this.productsJson))
}