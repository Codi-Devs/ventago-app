package com.teco.ventago.features.settings.ui.logo


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.teco.ventago.core.camera.PermissionCallback
import com.teco.ventago.core.camera.PermissionStatus
import com.teco.ventago.core.camera.PermissionType
import com.teco.ventago.core.camera.SharedImage
import com.teco.ventago.core.camera.createPermissionsManager
import com.teco.ventago.core.camera.rememberCameraManager
import com.teco.ventago.core.camera.rememberGalleryManager
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.DottedButton
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.organism.BusinessImage
import com.teco.ventago.design_system.organism.LoadingBottomSheet
import com.teco.ventago.features.settings.ui.logo.viewmodel.ChangeImageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.action_settings
import ventago.composeapp.generated.resources.are_you_sure
import ventago.composeapp.generated.resources.cancel
import ventago.composeapp.generated.resources.goback
import ventago.composeapp.generated.resources.permission_profile_image
import ventago.composeapp.generated.resources.permission_required
import ventago.composeapp.generated.resources.save_address
import ventago.composeapp.generated.resources.select_photo_from_camera
import ventago.composeapp.generated.resources.select_photo_from_gallery
import ventago.composeapp.generated.resources.stay
import ventago.composeapp.generated.resources.unsaved_changes
import ventago.composeapp.generated.resources.update


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeBusinessImageContent(
    viewModel: ChangeImageViewModel,
    navigateBack: () -> Unit
) {
    val showAlertDialog = remember { mutableStateOf(false) }
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })
    val scope = rememberCoroutineScope()

    val uploadImageSheetState = rememberModalBottomSheetState()
    var showUploadImageSheet by remember { mutableStateOf(false) }

    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var sharedImage by remember { mutableStateOf<SharedImage?>(null) }

    var launchCamera by remember { mutableStateOf(value = false) }
    var launchGallery by remember { mutableStateOf(value = false) }
    var launchSetting by remember { mutableStateOf(value = false) }
    var permissionRationalDialog by remember { mutableStateOf(value = false) }
    val permissionsManager = createPermissionsManager(object : PermissionCallback {
        override fun onPermissionStatus(
            permissionType: PermissionType,
            status: PermissionStatus
        ) {
            when (status) {
                PermissionStatus.GRANTED -> {
                    when (permissionType) {
                        PermissionType.CAMERA -> launchCamera = true
                        PermissionType.GALLERY -> launchGallery = true
                    }
                }

                else -> {
                    permissionRationalDialog = true
                }
            }
        }
    })

    val cameraManager = rememberCameraManager {
        scope.launch {
            sharedImage = it
            imageBitmap = withContext(Dispatchers.Default) {
                it?.toImageBitmap()
            }
            viewModel.state.enableButton.value = true
        }
    }

    val galleryManager = rememberGalleryManager {
        scope.launch {
            sharedImage = it
            imageBitmap = withContext(Dispatchers.Default) {
                it?.toImageBitmap()
            }
            viewModel.state.enableButton.value = true
        }
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

    if (permissionRationalDialog) {
        DMAlertDialog(
            title = stringResource(Res.string.permission_required),
            message = stringResource(Res.string.permission_profile_image),
            confirmText = stringResource(Res.string.action_settings),
            dismissText = stringResource(Res.string.cancel),
            onConfirm = {
                permissionRationalDialog = false
                launchSetting = true

            },
            onDismiss = {
                permissionRationalDialog = false
            },
            show = permissionRationalDialog
        )

    }

    Column(
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .fillMaxSize(),
    ) {

        BusinessImage(
            viewModel.state.imgUrl.value,
            imageBitmap,
        ) {
            showUploadImageSheet = true
        }

        Spacer(modifier = Modifier.weight(1f))

        ButtonM(
            onClick = {
                viewModel.updateBusinessLogo(sharedImage)
            },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            enabled = viewModel.state.enableButton.value
        ) {
            Text(stringResource(Res.string.update))
        }

        DMAlertDialog(
            title = stringResource(Res.string.are_you_sure),
            message = stringResource(Res.string.unsaved_changes),
            show = remember { showAlertDialog }.value,
            onDismiss = {
                showAlertDialog.value = false
                navigateBack()
            },
            onConfirm = {
                showAlertDialog.value = false
            },
            confirmText = stringResource(Res.string.stay),
            dismissText = stringResource(Res.string.goback)
        )
    }

    if (viewModel.state.loadingState.value.isLoading()) {
        LoadingBottomSheet(
            loadingState = viewModel.state.loadingState,
            sheetState = loadingSheetState
        ) {
            viewModel.loadingDone()
        }
    }

    if (showUploadImageSheet) {
        ModalBottomSheet(
            containerColor = MaterialTheme.colorScheme.background,
            sheetState = uploadImageSheetState,
            onDismissRequest = { showUploadImageSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButtonS(
                    label = stringResource(Res.string.select_photo_from_camera),
                    prefixIcon = rememberVectorPainter(Icons.Outlined.CameraAlt)
                ) {
                    launchCamera = true
                    scope.launch { uploadImageSheetState.hide() }.invokeOnCompletion {
                        showUploadImageSheet = false
                    }
                }

                TextButtonS(
                    label = stringResource(Res.string.select_photo_from_gallery),
                    prefixIcon = rememberVectorPainter(Icons.Outlined.Image)
                ) {
                    launchGallery = true
                    scope.launch { uploadImageSheetState.hide() }.invokeOnCompletion {
                        showUploadImageSheet = false
                    }
                }
            }
        }
    }
}