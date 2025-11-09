package com.teco.ventago.features.product.data.repository

import com.teco.ventago.features.product.domain.model.Category
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.Products


interface IProductsRepository {
    suspend fun setCategoryActive(categoryId: Int, active: Boolean): Boolean
    suspend fun changeCategoryOrder(categories: List<Category>): Boolean
    suspend fun editCategory(category: Category): Boolean
    suspend fun addCategory(category: Category, menuId: Int): Category
    suspend fun removeCategory(categoryId: Int): Boolean
    suspend fun addItem(item: Item, categoryId: Int): Item
    suspend fun editItem(item: Item, categoryId: Int): Boolean
    suspend fun removeItem(itemId: Int): Boolean
    suspend fun changeItemOrder(items: List<Item>): Boolean
    suspend fun getProductsByBusinessId(businessId: Int): Products
    suspend fun getProductIdByBusinessId(businessId: Int): Int
}