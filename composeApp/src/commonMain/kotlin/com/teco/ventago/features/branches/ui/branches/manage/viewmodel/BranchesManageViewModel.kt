package com.teco.ventago.features.branches.ui.branches.manage.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.camera.SharedImage
import com.teco.ventago.core.file.SharedFile
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.business.domain.BusinessService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class BranchesManageViewModel(
    private val branchService: BranchService,
    private val businessService: BusinessService,
) : BaseViewModel<BranchesManageState, BranchesManageStateUiEvent>(BranchesManageState()) {

    init {
        branchService.observe()
            .onStart {
                updateState {
                    copy(
                        branches = emptyList()
                    )
                }
            }
            .onEach { branches ->
                updateState {
                    copy(
                        branches =  branches
                    )
                }
            }.catch {
                // TODO Add logs
            }.launchIn(viewModelScope)
    }

    fun uploadLogo(branchCode: String, image: SharedImage?) {
        val businessId = businessService.business.value?.businessId
        val imageData = image?.toByteArray()

        if (businessId == null || businessId <= 0) {
            emitMessage("No se pudo determinar el negocio para actualizar el logo.")
            return
        }

        if (imageData == null || imageData.isEmpty()) {
            emitMessage("El logo debe ser PNG o JPG y pesar menos de ${MAX_LOGO_SIZE_MB} MB.")
            return
        }

        if (imageData.size > MAX_LOGO_SIZE_BYTES) {
            emitMessage("El logo debe ser PNG o JPG y pesar menos de ${MAX_LOGO_SIZE_MB} MB.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            updateState { copy(uploadingLogoBranchCode = branchCode) }
            try {
                val ok = branchService.uploadBranchLogo(
                    businessId = businessId,
                    branchCode = branchCode,
                    logo = SharedFile(
                        bytes = imageData,
                        fileName = "branch_${branchCode}_logo.jpg",
                        contentType = "image/jpeg",
                    )
                )
                emitEvent(
                    BranchesManageStateUiEvent.Message(
                        if (ok) "Logo de sucursal actualizado" else "No se pudo actualizar el logo"
                    )
                )
            } catch (_: Throwable) {
                emitEvent(BranchesManageStateUiEvent.Message("No se pudo actualizar el logo"))
            } finally {
                updateState { copy(uploadingLogoBranchCode = null) }
            }
        }
    }

    fun deleteLogo(branchCode: String) {
        val businessId = businessService.business.value?.businessId

        if (businessId == null || businessId <= 0) {
            emitMessage("No se pudo determinar el negocio para eliminar el logo.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            updateState { copy(deletingLogoBranchCode = branchCode) }
            try {
                val ok = branchService.deleteBranchLogo(
                    businessId = businessId,
                    branchCode = branchCode,
                )
                emitEvent(
                    BranchesManageStateUiEvent.Message(
                        if (ok) "Logo de sucursal eliminado" else "No se pudo eliminar el logo"
                    )
                )
            } catch (_: Throwable) {
                emitEvent(BranchesManageStateUiEvent.Message("No se pudo eliminar el logo"))
            } finally {
                updateState { copy(deletingLogoBranchCode = null) }
            }
        }
    }

    private fun emitMessage(text: String) {
        viewModelScope.launch {
            emitEvent(BranchesManageStateUiEvent.Message(text))
        }
    }

    private companion object {
        const val MAX_LOGO_SIZE_MB = 2
        const val MAX_LOGO_SIZE_BYTES = MAX_LOGO_SIZE_MB * 1024 * 1024
    }
}
