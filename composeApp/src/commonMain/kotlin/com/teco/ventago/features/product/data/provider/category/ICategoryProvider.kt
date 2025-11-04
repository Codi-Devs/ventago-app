package com.teco.ventago.features.product.data.provider.category

import com.teco.ventago.features.product.domain.model.Category
import com.teco.ventago.utils.ApiResponse


interface ICategoryProvider {
    suspend fun setActive(categoryId: Int, active: Boolean): ApiResponse
    suspend fun changeCategoryOrder(categories: List<Category>): ApiResponse
    suspend fun editCategory(category: Category): ApiResponse
    suspend fun addCategory(category: Category, menuId: Int): ApiResponse
    suspend fun removeCategory(categoryId: Int): ApiResponse

}