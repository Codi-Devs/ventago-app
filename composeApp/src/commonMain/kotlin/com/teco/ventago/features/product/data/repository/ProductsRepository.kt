package com.teco.ventago.features.product.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.product.data.provider.category.ICategoryProvider
import com.teco.ventago.features.product.data.provider.item.IItemProvider
import com.teco.ventago.features.product.data.provider.product.IProductProvider
import com.teco.ventago.features.product.domain.model.Category
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.Products
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ProductsRepository(
    private val productProvider: IProductProvider,
    private val categoryProvider: ICategoryProvider,
    private val itemProvider: IItemProvider,
    private val logger: ILoggerService
): IProductsRepository {
    override suspend fun setCategoryActive(categoryId: Int, active: Boolean): Boolean {
        return try {
            val response = categoryProvider.setActive(categoryId, active)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            response.successful
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                LogLevel.ERROR,
                "setCategoryActive",
                "Error setting category active. Error: ${e.message ?: "UNKNOWN"}"
            )
            )
            false
        }
    }

    override suspend fun changeCategoryOrder(categories: List<Category>): Boolean {
        return try {
            val response = categoryProvider.changeCategoryOrder(categories)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            response.successful
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "changeCategoryOrder", "Error changing category order. Error: ${e.message ?: "UNKNOWN" }"))
            false
        }
    }

    override suspend fun editCategory(category: Category): Boolean {
        return try {
            val response = categoryProvider.editCategory(category)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            response.successful
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "editCategory", "Error editing category. Error: ${e.message ?: "UNKNOWN" }"))
            false
        }
    }

    override suspend fun addCategory(category: Category, menuId: Int): Category {
        return try {
            val response = categoryProvider.addCategory(category, menuId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            Category.newCategoryFromJson(response.data!!.jsonObject)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "addCategory", "Error adding category. Error: ${e.message ?: "UNKNOWN" }"))
            throw e
        }
    }

    override suspend fun removeCategory(categoryId: Int): Boolean {
        return try {
            val response = categoryProvider.removeCategory(categoryId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            response.successful
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "removeCategory", "Error removing category. Error: ${e.message ?: "UNKNOWN" }"))
            false
        }
    }

    override suspend fun addItem(item: Item, categoryId: Int): Item {
        return try {
            val response = itemProvider.addItem(item, categoryId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            Item.newItemFromJson(response.data!!.jsonObject)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "addItem", "Error adding item. Error: ${e.message ?: "UNKNOWN" }"))
            throw e
        }
    }

    override suspend fun editItem(item: Item): Boolean {
        return try {
            val response = itemProvider.editItem(item)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            response.successful
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "editItem", "Error editing item. Error: ${e.message ?: "UNKNOWN" }"))
            false
        }
    }

    override suspend fun removeItem(itemId: Int): Boolean {
        return try {
            val response = itemProvider.removeItem(itemId)
            println("ASDASD: Response from backend ${response.toJson()}")
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            response.successful
        } catch (e: Exception) {
            println("ASDASD: Error removing item ${e.message}")
            logger.sendLog(Log(LogLevel.ERROR, "removeItem", "Error removing item. Error: ${e.message ?: "UNKNOWN" }"))
            false
        }
    }

    override suspend fun changeItemOrder(items: List<Item>): Boolean {
        return try {
            val response = itemProvider.changeItemOrder(items)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            response.successful
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "changeItemOrder", "Error changing item order. Error: ${e.message ?: "UNKNOWN" }"))
            false
        }
    }

    override suspend fun getProductsByBusinessId(businessId: Int): Products {
        return try {
            val response = productProvider.getMenuByBusinessId(businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            Products(response.data!!.jsonObject)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "getMenuByBusinessId", "Error getting menu by business id. Error: ${e.message ?: "UNKNOWN" }"))
            throw e
        }
    }

    override suspend fun getProductIdByBusinessId(businessId: Int): Int {
        return try {
            val response = productProvider.getMenuIdByBusinessId(businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            response.data?.jsonPrimitive?.int ?: -1
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "getMenuIdByBusinessId", "Error getting menu id by business id. Error: ${e.message ?: "UNKNOWN" }"))
            throw e
        }
    }

}