package com.teco.ventago.features.branches.ui.branches.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.teco.ventago.core.SnackbarService
import com.teco.ventago.core.camera.PermissionCallback
import com.teco.ventago.core.camera.PermissionStatus
import com.teco.ventago.core.camera.PermissionType
import com.teco.ventago.core.camera.createPermissionsManager
import com.teco.ventago.core.camera.rememberCameraManager
import com.teco.ventago.core.camera.rememberGalleryManager
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.molecules.BranchItem
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.branches.ui.branches.manage.viewmodel.BranchesManageViewModel
import com.teco.ventago.features.branches.ui.branches.manage.viewmodel.BranchesManageStateUiEvent
import com.teco.ventago.navigation.BillingPointManageRoute
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchesManageScreen(
    viewModel: BranchesManageViewModel = koinViewModel<BranchesManageViewModel>(),
    navController: NavHostController
) {

    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uploadImageSheetState = rememberModalBottomSheetState()
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarService: SnackbarService = koinInject()
    val uriHandler = LocalUriHandler.current

    var uploadLogoSheetBranchCode by remember { mutableStateOf<String?>(null) }
    var uploadLogoTargetBranchCode by remember { mutableStateOf<String?>(null) }
    var deleteLogoBranchCode by remember { mutableStateOf<String?>(null) }
    var launchCamera by remember { mutableStateOf(false) }
    var launchGallery by remember { mutableStateOf(false) }
    var launchSetting by remember { mutableStateOf(false) }
    var permissionRationalDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is BranchesManageStateUiEvent.Message -> snackbarService.show(event.text)
                BranchesManageStateUiEvent.GoBack -> Unit
            }
        }
    }

    val permissionsManager = createPermissionsManager(object : PermissionCallback {
        override fun onPermissionStatus(permissionType: PermissionType, status: PermissionStatus) {
            when (status) {
                PermissionStatus.GRANTED -> when (permissionType) {
                    PermissionType.CAMERA -> launchCamera = true
                    PermissionType.GALLERY -> launchGallery = true
                }
                else -> permissionRationalDialog = true
            }
        }
    })

    val cameraManager = rememberCameraManager { image ->
        uploadLogoTargetBranchCode?.let { branchCode ->
            viewModel.uploadLogo(branchCode, image)
        }
        uploadLogoTargetBranchCode = null
    }

    val galleryManager = rememberGalleryManager { image ->
        uploadLogoTargetBranchCode?.let { branchCode ->
            viewModel.uploadLogo(branchCode, image)
        }
        uploadLogoTargetBranchCode = null
    }

    if (launchGallery) {
        if (permissionsManager.isPermissionGranted(PermissionType.GALLERY)) {
            galleryManager.launch()
        } else {
            permissionsManager.askPermission(PermissionType.GALLERY)
        }
        launchGallery = false
    }

    if (launchCamera) {
        if (permissionsManager.isPermissionGranted(PermissionType.CAMERA)) {
            cameraManager.launch()
        } else {
            permissionsManager.askPermission(PermissionType.CAMERA)
        }
        launchCamera = false
    }

    if (launchSetting) {
        permissionsManager.launchSettings()
        launchSetting = false
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = vanishedBackgroundColor(), shape = RoundedCornerShape(12.dp)
                )
                .padding(top = 8.dp, bottom = 8.dp),
            state = lazyListState,
        ) {
            items(uiState.branches, key = { it.branchCode }) { branch ->
                val isLogoLoading = uiState.uploadingLogoBranchCode == branch.branchCode ||
                    uiState.deletingLogoBranchCode == branch.branchCode

                BranchItem(
                    modifier = Modifier.padding(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp
                    ),
                    branch = branch,
                    isLogoLoading = isLogoLoading,
                    onClick = {
                        navController.navigate(BillingPointManageRoute(branch.branchCode))
                    },
                    onUploadLogoClick = {
                        uploadLogoTargetBranchCode = branch.branchCode
                        uploadLogoSheetBranchCode = branch.branchCode
                    },
                    onViewLogoClick = {
                        val logoUrl = branch.logoUrl.orEmpty()
                        if (logoUrl.isNotBlank() && !logoUrl.equals("null", ignoreCase = true)) {
                            uriHandler.openUri(logoUrl)
                        } else {
                            scope.launch { snackbarService.show("Sin logo configurado") }
                        }
                    },
                    onDeleteLogoClick = {
                        deleteLogoBranchCode = branch.branchCode
                    },
                )
            }

        }

        if (uiState.loadingBottomSheet.isLoading()) {
            LoadingSheet(
                state = uiState.loadingBottomSheet,
                sheetState = loadingSheetState
            ) {
                viewModel.hideLoading()
            }
        }
    }

    if (uploadLogoSheetBranchCode != null) {
        ModalBottomSheet(
            containerColor = cardContainerColor(),
            sheetState = uploadImageSheetState,
            onDismissRequest = {
                uploadLogoSheetBranchCode = null
                uploadLogoTargetBranchCode = null
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Seleccionar logo de sucursal")
                TextButtonS(
                    label = "Tomar foto",
                    prefixIcon = rememberVectorPainter(Icons.Outlined.CameraAlt),
                ) {
                    launchCamera = true
                    scope.launch { uploadImageSheetState.hide() }.invokeOnCompletion {
                        uploadLogoSheetBranchCode = null
                    }
                }

                TextButtonS(
                    label = "Elegir de galería",
                    prefixIcon = rememberVectorPainter(Icons.Outlined.Image),
                ) {
                    launchGallery = true
                    scope.launch { uploadImageSheetState.hide() }.invokeOnCompletion {
                        uploadLogoSheetBranchCode = null
                    }
                }
            }
        }
    }

    deleteLogoBranchCode?.let { branchCode ->
        DMAlertDialog(
            title = "Eliminar logo",
            message = "¿Deseas eliminar el logo de esta sucursal?",
            show = true,
            onDismiss = { deleteLogoBranchCode = null },
            onConfirm = {
                deleteLogoBranchCode = null
                viewModel.deleteLogo(branchCode)
            },
            confirmText = "Eliminar",
            dismissText = "Cancelar",
        )
    }

    DMAlertDialog(
        title = "Permiso requerido",
        message = "Para seleccionar el logo de la sucursal, concede el permiso desde ajustes.",
        show = permissionRationalDialog,
        onDismiss = { permissionRationalDialog = false },
        onConfirm = {
            permissionRationalDialog = false
            launchSetting = true
        },
        confirmText = "Ajustes",
        dismissText = "Cancelar",
    )
}
