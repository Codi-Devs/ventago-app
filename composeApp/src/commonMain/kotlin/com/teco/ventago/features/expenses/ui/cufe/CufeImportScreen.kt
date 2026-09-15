package com.teco.ventago.features.expenses.ui.cufe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teco.ventago.core.camera.PermissionCallback
import com.teco.ventago.core.camera.PermissionStatus
import com.teco.ventago.core.camera.PermissionType
import com.teco.ventago.core.camera.createPermissionsManager
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.expenses.ui.components.InvoiceReceivedScreen
import com.teco.ventago.features.expenses.ui.components.InvoiceScanningScreen
import com.teco.ventago.utils.BarcodeScannerScreen
import com.teco.ventago.utils.KmpBarcodeFormat

@Composable
fun CufeImportScreen(
    viewModel: CufeImportViewModel,
    onBack: () -> Unit,
    onExpenseImported: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var launchSetting by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        viewModel.onScreenVisible()
        onDispose {
            viewModel.onScreenHidden()
            viewModel.reset()
        }
    }

    val permissionCallback = remember(viewModel) {
        object : PermissionCallback {
            override fun onPermissionStatus(
                permissionType: PermissionType,
                status: PermissionStatus
            ) {
                when (status) {
                    PermissionStatus.GRANTED -> {
                        if (permissionType == PermissionType.CAMERA) {
                            viewModel.showScanner(true)
                        }
                    }
                    PermissionStatus.SHOW_RATIONAL,
                    PermissionStatus.DENIED -> {
                        if (permissionType == PermissionType.CAMERA) {
                            viewModel.showPermissionDialog(true)
                        }
                    }
                }
            }
        }
    }
    val permissionsManager = createPermissionsManager(permissionCallback)

    val cameraGranted = permissionsManager.isPermissionGranted(PermissionType.CAMERA)
    val needsScanner = uiState.wantsScanner &&
        !uiState.importSuccess &&
        !uiState.isImporting &&
        uiState.hasExpensesQr

    if (needsScanner && !cameraGranted && !uiState.showPermissionDialog) {
        permissionsManager.askPermission(PermissionType.CAMERA)
    }

    LaunchedEffect(cameraGranted, needsScanner) {
        if (needsScanner && cameraGranted) {
            viewModel.showScanner(true)
        }
    }

    if (launchSetting) {
        permissionsManager.launchSettings()
        launchSetting = false
    }

    DMAlertDialog(
        title = "Permiso de cámara",
        message = "Se necesita acceso a la cámara para escanear códigos QR. Por favor habilite el permiso en la configuración.",
        show = uiState.showPermissionDialog,
        confirmText = "Configuración",
        dismissText = "Cancelar",
        onConfirm = {
            viewModel.showPermissionDialog(false)
            launchSetting = true
        },
        onDismiss = {
            viewModel.showPermissionDialog(false)
            onBack()
        }
    )

    when {
        uiState.importSuccess -> InvoiceReceivedScreen(
            onUnderstood = onBack,
            onScanAnother = { viewModel.scanAnother() }
        )

        uiState.isImporting -> InvoiceScanningScreen(
            title = "Escaneando documento...",
            subtitle = "Recibiendo la factura desde el código QR."
        )

        uiState.showScanner -> Box(modifier = Modifier.fillMaxSize()) {
            BarcodeScannerScreen(
                format = KmpBarcodeFormat.QR_CODE,
                onResult = { rawValue -> viewModel.onQrScanned(rawValue) },
                onClose = onBack
            )
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = bodyMedium(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp)
                )
            }
        }

        uiState.betaLoaded && !uiState.hasExpensesQr -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Función no disponible", style = titleMediumBold())
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "La importación por CUFE está disponible para usuarios del beta expenses_qr.",
                style = bodyMedium(),
                textAlign = TextAlign.Center
            )
        }

        uiState.error != null -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = uiState.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = bodyMedium(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            ButtonM(onClick = { viewModel.scanAnother() }, modifier = Modifier.fillMaxWidth()) {
                Text("Escanear otra factura")
            }
            OutlinedButtonM(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Volver a Gastos")
            }
        }

        else -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }
    }
}
