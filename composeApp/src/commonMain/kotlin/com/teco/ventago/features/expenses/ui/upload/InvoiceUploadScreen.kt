package com.teco.ventago.features.expenses.ui.upload

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teco.ventago.core.camera.PermissionCallback
import com.teco.ventago.core.camera.PermissionStatus
import com.teco.ventago.core.camera.PermissionType
import com.teco.ventago.core.camera.createPermissionsManager
import com.teco.ventago.core.camera.rememberCameraManager
import com.teco.ventago.core.camera.rememberGalleryManager
import com.teco.ventago.core.file.SharedFile
import com.teco.ventago.core.file.rememberDocumentPickerManager
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.features.expenses.ui.components.InvoiceReceivedScreen
import com.teco.ventago.features.expenses.ui.components.InvoiceScanningScreen
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.utils.randomUUID
import kotlinx.coroutines.launch

@Composable
fun InvoiceUploadScreen(
    viewModel: InvoiceUploadViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var launchCamera by remember { mutableStateOf(false) }
    var launchGallery by remember { mutableStateOf(false) }
    var launchDocument by remember { mutableStateOf(false) }
    var launchSetting by remember { mutableStateOf(false) }

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
    val cameraManager = rememberCameraManager { image ->
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
            cameraManager.launch()
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

    if (uiState.showAccepted) {
        InvoiceReceivedScreen(
            onUnderstood = {
                viewModel.dismissAccepted()
                onBack()
            },
            onScanAnother = { viewModel.dismissAccepted() }
        )
        return
    }

    if (uiState.isScanning || uiState.isUploading) {
        InvoiceScanningScreen(
            title = if (uiState.isUploading) "Enviando factura..." else "Escaneando documento...",
            subtitle = if (uiState.isUploading) {
                "La IA la procesará en segundo plano."
            } else {
                "Buscamos el QR y revisamos que la foto se pueda leer."
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!uiState.hasInvoiceUploadAccess) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = cardContainerColor())
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Función no disponible", style = titleMediumBold())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "La carga de facturas está disponible para usuarios del beta expenses_ocr o expenses_qr.",
                        style = bodyMedium()
                    )
                }
            }
            return@Column
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sube tu factura", style = titleMediumBold())
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Toma una foto, elige una imagen o un PDF. La IA la escanea y el gasto aparece en la lista cuando está listo.",
                    style = bodyMedium()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "PDF, JPEG o PNG. Máximo 10 MB.",
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButtonM(
                        onClick = { launchCamera = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cámara")
                    }
                    OutlinedButtonM(
                        onClick = { launchGallery = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Collections,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Galería")
                    }
                    OutlinedButtonM(
                        onClick = { launchDocument = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Archivo")
                    }
                }

                uiState.selectedFileName?.takeIf { it.isNotBlank() }?.let { name ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(name, style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
        }

        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = bodyMedium()
            )
        }
    }
}
