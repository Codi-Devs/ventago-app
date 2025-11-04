package com.teco.ventago.features.settings.ui.logo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.camera.SharedImage
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.settings.domain.SettingsService
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.utils.randomUUID
import com.teco.ventago.utils.uploadImageToBunnyCdn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangeImageViewModel(
    private val businessService: BusinessService,
    private val settingsService: SettingsService,
    private val logger: ILoggerService
) : ViewModel() {

    var business = businessService.business.value
    val state = ChangeImageState()

    init {
        viewModelScope.launch {
            businessService.business.collect { businessData ->
                businessData?.let {
                    business = it
                    if (it.logo.isNotEmpty() && it.logo != "null") {
                        state.imgUrl.value = it.logo
                    }
                }
            }
        }
    }

    fun loadingDone() {
        state.loadingState.value = state.loadingState.value.copy(state = LoadingState.HIDDEN)
    }


    fun updateBusinessLogo(image: SharedImage?) {
        state.showLoading("")
        business?.let { newBusiness ->
            viewModelScope.launch {
                try {
                    var imageUrl = ""
                    if (image != null) {
                        val imageData = withContext(Dispatchers.Default) {
                            image.toByteArray()
                        }
                        if (imageData != null) {
                            withContext(Dispatchers.IO)  {
                                imageUrl = uploadImageToBunnyCdn(
                                    imageData = imageData,
                                    "business_${newBusiness.businessId}.jpg",
                                    newBusiness.businessId.toString()
                                ) ?: ""
                            }
                        }
                    }

                    val res = settingsService.updateBusinessLogo(newBusiness.businessId, imageUrl)
                    business = businessService.business.value
                    business?.logo = imageUrl
                    if (res) {
                        state.imgUrl.value = imageUrl
                        state.enableButton.value = false
                        state.pendingChanges.value = false
                        state.showSuccess()
                    } else {
                        state.showError()
                    }
                } catch (e: Exception) {
                    logger.sendLog(
                        Log(
                            LogLevel.ERROR,
                            "updateBusinessLogo",
                            "Error updateing business logo. Error: ${e.message ?: "UNKNOWN"}"

                        )
                    )
                    state.showError()
                }

            }
        }
    }
}