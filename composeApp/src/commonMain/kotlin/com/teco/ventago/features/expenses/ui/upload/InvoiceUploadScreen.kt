package com.teco.ventago.features.expenses.ui.upload

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teco.ventago.core.SnackbarService
import com.teco.ventago.core.camera.PermissionCallback
import com.teco.ventago.core.camera.PermissionStatus
import com.teco.ventago.core.camera.PermissionType
import com.teco.ventago.core.camera.createPermissionsManager
import com.teco.ventago.core.camera.rememberGalleryManager
import com.teco.ventago.core.file.SharedFile
import com.teco.ventago.core.file.rememberDocumentPickerManager
import com.teco.ventago.features.expenses.ui.components.ExpenseInvoiceCopy
import com.teco.ventago.features.expenses.ui.components.ExpenseSheetOption
import com.teco.ventago.features.expenses.ui.components.InvoiceReceivedScreen
import com.teco.ventago.features.expenses.ui.components.InvoiceScanningScreen
import com.teco.ventago.utils.randomUUID
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun InvoiceUploadScreen(
    viewModel: InvoiceUploadViewModel,
    onBack: () -> Unit
) {
    var showSourceSheet by remember { mutableStateOf(true) }
    InvoiceUploadCapture(
        viewModel = viewModel,
        showSourceSheet = showSourceSheet,
        onDismissSourceSheet = { showSourceSheet = false },
        onCancelSourceSheet = onBack,
        onFinished = onBack,
        onRequestAnotherUpload = { showSourceSheet = true }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceUploadCapture(
    viewModel: InvoiceUploadViewModel,
    showSourceSheet: Boolean,
    onDismissSourceSheet: () -> Unit,
    onCancelSourceSheet: () -> Unit = onDismissSourceSheet,
    onFinished: () -> Unit,
    onAcceptedAndContinue: () -> Unit = onFinished,
    onRequestAnotherUpload: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var launchCamera by remember { mutableStateOf(false) }
    var launchGallery by remember { mutableStateOf(false) }
    var launchDocument by remember { mutableStateOf(false) }
    var launchSetting by remember { mutableStateOf(false) }
    var showInvoiceCamera by remember { mutableStateOf(false) }
    val sourceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarService: SnackbarService = koinInject()

    val permissionCallback = remember {
        object : PermissionCallback {
            override fun onPermissionStatus(permissionType: PermissionType, status: PermissionStatus) {
                when (status) {
                    PermissionStatus.GRANTED -> when (permissionType) {
                        PermissionType.CAMERA -> launchCamera = true
                        PermissionType.GALLERY -> launchGallery = true
                    }
                    else -> {}
                }
            }
        }
    }
    val permissionsManager = createPermissionsManager(permissionCallback)

    val scope = rememberCoroutineScope()
    val galleryManager = rememberGalleryManager { image ->
        scope.launch {
            val bytes = image?.toByteArray()
            if (bytes != null) {
                viewModel.uploadFile(
                    SharedFile(
                        bytes = bytes,
                        fileName = "factura_${randomUUID()}.jpg",
                        contentType = "image/jpeg"
                    )
                )
            }
        }
    }
    val documentManager = rememberDocumentPickerManager(
        acceptedMimeTypes = listOf(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
        )
    ) { file ->
        scope.launch {
            if (file != null) {
                viewModel.uploadFile(file)
            }
        }
    }

    val cameraGranted = permissionsManager.isPermissionGranted(PermissionType.CAMERA)
    val galleryGranted = permissionsManager.isPermissionGranted(PermissionType.GALLERY)

    if (launchGallery && !galleryGranted) {
        permissionsManager.askPermission(PermissionType.GALLERY)
    }
    if (launchCamera && !cameraGranted) {
        permissionsManager.askPermission(PermissionType.CAMERA)
    }
    LaunchedEffect(launchGallery, galleryGranted) {
        if (launchGallery && galleryGranted) {
            galleryManager.launch()
            launchGallery = false
        }
    }
    LaunchedEffect(launchCamera, cameraGranted) {
        if (launchCamera && cameraGranted) {
            showInvoiceCamera = true
            launchCamera = false
        }
    }
    if (launchSetting) {
        permissionsManager.launchSettings()
        launchSetting = false
    }
    if (launchDocument) {
        documentManager.launch()
        launchDocument = false
    }

    LaunchedEffect(uiState.error) {
        val error = uiState.error ?: return@LaunchedEffect
        snackbarService.show(error)
        viewModel.clearError()
    }

    if (uiState.showAccepted) {
        InvoiceReceivedScreen(
            message = if (uiState.acceptedFromDgi) {
                ExpenseInvoiceCopy.DGI_RECEIVED
            } else {
                ExpenseInvoiceCopy.OCR_RECEIVED
            },
            onUnderstood = {
                viewModel.dismissAccepted()
                onAcceptedAndContinue()
            },
            onScanAnother = {
                viewModel.dismissAccepted()
                onRequestAnotherUpload()
            }
        )
        return
    }

    if (uiState.isScanning || uiState.isUploading) {
        InvoiceScanningScreen(
            title = if (uiState.isUploading) "Enviando factura..." else "Escaneando documento...",
            subtitle = if (uiState.isUploading) {
                ExpenseInvoiceCopy.OCR_SCANNING
            } else {
                "Revisamos que la foto se pueda leer."
            }
        )
        return
    }

    if (showInvoiceCamera) {
        InvoiceDocumentCamera(
            onCaptured = { bytes ->
                showInvoiceCamera = false
                viewModel.uploadFile(
                    SharedFile(
                        bytes = bytes,
                        fileName = "factura_${randomUUID()}.jpg",
                        contentType = "image/jpeg"
                    )
                )
            },
            onClose = { showInvoiceCamera = false }
        )
        return
    }

    if (showSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = onCancelSourceSheet,
            sheetState = sourceSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Sube tu factura",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExpenseSheetOption(
                    icon = Icons.Rounded.CameraAlt,
                    title = "Cámara",
                    subtitle = "Toma una foto de la factura"
                ) {
                    onDismissSourceSheet()
                    launchCamera = true
                }
                ExpenseSheetOption(
                    icon = Icons.Rounded.Collections,
                    title = "Galería",
                    subtitle = "Elige una foto de tu galería"
                ) {
                    onDismissSourceSheet()
                    launchGallery = true
                }
                ExpenseSheetOption(
                    icon = Icons.Rounded.Description,
                    title = "Archivo",
                    subtitle = "Selecciona un PDF o una imagen"
                ) {
                    onDismissSourceSheet()
                    launchDocument = true
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
