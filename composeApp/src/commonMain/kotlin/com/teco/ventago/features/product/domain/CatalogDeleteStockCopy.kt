package com.teco.ventago.features.product.domain

object CatalogDeleteStockCopy {
    fun product(name: String, stock: String): String {
        val productName = name.trim().ifBlank { "este producto" }
        return "No puedes borrar «$productName» porque tiene $stock en stock. Registra una salida de inventario hasta dejarlo en 0 y después podrás borrarlo."
    }

    fun category(categoryName: String, products: List<Pair<String, String>>): String? {
        if (products.isEmpty()) return null
        val name = categoryName.trim().ifBlank { "esta categoría" }
        if (products.size == 1) {
            val (productName, stock) = products.first()
            val label = productName.trim().ifBlank { "un producto" }
            return "No puedes borrar la categoría «$name» porque «$label» tiene $stock en stock. Registra una salida de inventario hasta dejarlo en 0 y después podrás borrar la categoría."
        }
        val shown = products.take(3).joinToString(", ") { (productName, stock) ->
            val label = productName.trim().ifBlank { "un producto" }
            "«$label» ($stock)"
        }
        val extra = if (products.size > 3) " y ${products.size - 3} más" else ""
        return "No puedes borrar la categoría «$name» porque estos productos todavía tienen stock: $shown$extra. Registra una salida de inventario hasta dejarlos en 0 y después podrás borrar la categoría."
    }
}
