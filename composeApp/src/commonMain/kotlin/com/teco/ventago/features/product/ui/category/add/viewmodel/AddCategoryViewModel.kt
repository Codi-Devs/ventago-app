package com.teco.ventago.features.product.ui.category.add.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.product.domain.ProductService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
//import utils.AnalyticsHelper

class AddCategoryViewModel(
    private val authService: IAuthService,
    private val businessService: BusinessService,
    private val productService: ProductService,
): ViewModel() {

    val state = AddCategoryState()

    fun loadingDone() {
        state.loadingState.value = state.loadingState.value.copy(state = LoadingState.HIDDEN)
    }

    fun onNameChanged(name: String) {
        state.name.value = name
        state.wrongName.value = false
    }

    fun onDescriptionChanged(desc: String) {
        state.description.value = desc
    }

    fun createCategory() {
        val name = state.name.value
        val desc = state.description.value
        if (name.isBlank()) {
            state.wrongName.value = true
            return
        }

//        AnalyticsHelper.logEvent("category_created", AnalyticsHelper.getAnalyticsBundle().apply {
//            putString("category_name", name)
//        })

        if (!productService.canAddCategory()){
            state.nonVipLimitReached.value = true
            return
        }

        state.showLoading("Creando categoría")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = productService.addCategory(name, desc)
                if (res.id > 0) {
                    state.showSuccess()
                } else {
                    state.showError()
                }
            } catch (e: Exception) {
                // TODO Add logs
                state.showError()
            }
        }
    }
}