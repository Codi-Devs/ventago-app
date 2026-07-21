package com.teco.ventago.core.cache

import com.teco.ventago.core.Paged
import com.teco.ventago.core.cache.room.CacheDatabase
import com.teco.ventago.core.cache.room.models.UserCache
import com.teco.ventago.core.cache.room.models.toBusinessIdsList
import com.teco.ventago.core.cache.room.models.toCache
import com.teco.ventago.core.cache.room.models.toObject
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.features.auth.domain.model.User
import com.teco.ventago.features.auth.domain.model.response.BusinessIds
import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.financialProfile.domain.model.BusinessFinancialProfile
import com.teco.ventago.features.product.domain.model.Products
import kotlin.reflect.KClass

class RoomCache (private val cacheDatabase: CacheDatabase,  private val changesManager: IChangesManager): ICacheService {
    override suspend fun <T : Any> getCache(klass: KClass<T>): T? {
        return when (klass) {
            User::class -> {
                val userCache = cacheDatabase.getUserCacheDao().getUser()
                userCache?.let {
                    User(
                        it.uid,
                        it.email,
                        it.name,
                        it.premium,
                        it.active,
                        it.missingBusiness,
                        it.userId,
                        it.businessIds.businessIds.map { businessIdsCache ->
                            BusinessIds(
                                businessIdsCache.businessId,
                                businessIdsCache.menuId
                            )
                        }
                    )
                }?: return null

            }
            Business::class -> cacheDatabase.getBusinessCacheDao().getBusiness()?.toObject()
            BusinessFinancialProfile::class -> cacheDatabase.getFinancialProfileCacheDao().getProfile()?.toObject()
            Products::class -> cacheDatabase.getProductsCacheDao().getProducts()?.toObject()
            else -> null
        } as T?
    }

    override suspend fun <T : Any> getCache(key: String): T? {
        return when (key) {
            CacheUtils.CUSTOMERS -> cacheDatabase.getCustomerCacheDao().getCustomers()?.toObject()
            CacheUtils.BRANCHES -> cacheDatabase.getBranchesCacheDao().getBranches()?.toObject()
            else -> null
        } as T?
    }

    override suspend fun <T> saveCache(data: T) {
        when (data) {
            is User -> {
                cacheDatabase.getUserCacheDao().deleteUser()
                cacheDatabase.getUserCacheDao().insert(UserCache(
                    0,
                    data.uid,
                    data.email,
                    data.name,
                    data.premium,
                    data.active,
                    data.missingBusiness,
                    data.userId,
                    data.businessIds.toBusinessIdsList()
                ))
            }
            is Business -> {
                cacheDatabase.getBusinessCacheDao().deleteBusiness()
                data.toCache()?.let { cacheDatabase.getBusinessCacheDao().insert(it) }
                changesManager.businessChanged()
            }
            is BusinessFinancialProfile -> {
                cacheDatabase.getFinancialProfileCacheDao().deleteProfile()
                data.toCache()?.let { cacheDatabase.getFinancialProfileCacheDao().insert(it) }
            }
            is Products -> {
                cacheDatabase.getProductsCacheDao().deleteProducts()
                data.toCache()?.let { cacheDatabase.getProductsCacheDao().insert(it) }
                changesManager.productsChanged()
            }
        }
    }

    override suspend fun <T> saveCache(key: String, data: T) {
        when (key) {
            CacheUtils.CUSTOMERS -> cacheDatabase.getCustomerCacheDao().insert((data as Paged<CustomerListItem>).toCache())
            CacheUtils.BRANCHES -> cacheDatabase.getBranchesCacheDao().insert((data as List<Branch>).toCache())
        }
    }

    override suspend fun clearCache(id: String) {
        when (id) {
            CacheUtils.USER -> cacheDatabase.getUserCacheDao().deleteUser()
            CacheUtils.BUSINESS -> cacheDatabase.getBusinessCacheDao().deleteBusiness()
            CacheUtils.FINANCIAL_PROFILE -> cacheDatabase.getFinancialProfileCacheDao().deleteProfile()
            CacheUtils.PRODUCTS -> cacheDatabase.getProductsCacheDao().deleteProducts()
            CacheUtils.CUSTOMERS -> cacheDatabase.getCustomerCacheDao().deleteCustomers()
            CacheUtils.CUSTOMERS -> cacheDatabase.getBranchesCacheDao().deleteBranches()
        }
    }

    override suspend fun clearAllCache() {
        cacheDatabase.getUserCacheDao().deleteUser()
        cacheDatabase.getBusinessCacheDao().deleteBusiness()
        cacheDatabase.getProductsCacheDao().deleteProducts()
        cacheDatabase.getCustomerCacheDao().deleteCustomers()
        cacheDatabase.getFinancialProfileCacheDao().deleteProfile()
        cacheDatabase.getBranchesCacheDao().deleteBranches()
    }
}
