package com.teco.ventago.features.product.ui.category.add.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.product.domain.ProductService
import com.teco.ventago.features.product.domain.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
//import utils.AnalyticsHelper

class ModifyCategoryViewModel(private val productService: ProductService): ViewModel()  {
    val state = AddCategoryState()
    var selectedCategory: Category? = null

    init {
        val selectedCategoryId = productService.selectedCategoryId
        if (selectedCategoryId == null) {
            state.goBack.value = true
        }

        viewModelScope.launch {
            productService.getMenu().onEach { menu ->
                menu?.let { _ ->
                    menu.categories.firstOrNull{ it.id == selectedCategoryId }?.let {
                        selectedCategory = it
                        state.name.value = it.name
                        state.description.value = it.desc
                    } ?: run {
                        // TODO Add logs
                        state.goBack.value = true
                    }
                } ?: run {
                    // TODO Add logs
                    state.goBack.value = true
                }
            }.launchIn(this)
        }
    }

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

    fun saveCategory() {
        val name = state.name.value
        val desc = state.description.value
        if (name.isBlank()) {
            state.wrongName.value = true
            return
        }

//        AnalyticsHelper.logEvent("category_modified", AnalyticsHelper.getAnalyticsBundle().apply {
//            putString("category_name", name)
//        })

        selectedCategory?.let { category ->
            state.showLoading("Editando categoría")
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val res = productService.editCategory(category.copy(name = name, desc = desc))
                    if (res) {
                        state.showSuccess()
                        delay(600)
                        state.goBack.value = true
                    } else {
                        // TODO Add logs
                        state.showError()
                    }
                } catch (e: Exception) {
                    // TODO Add logs
                    state.showError()
                }
            }
        }

    }

}