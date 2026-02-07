package com.teco.ventago.features.expenses.ui.cufe

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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teco.ventago.core.camera.PermissionCallback
import com.teco.ventago.core.camera.PermissionStatus
import com.teco.ventago.core.camera.PermissionType
import com.teco.ventago.core.camera.createPermissionsManager
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.utils.BarcodeScannerScreen
import com.teco.ventago.utils.KmpBarcodeFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CufeImportScreen(
    viewModel: CufeImportViewModel,
    onBack: () -> Unit,
    onExpenseImported: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val job = uiState.currentJob

    var launchCamera by remember { mutableStateOf(false) }
    var launchSetting by remember { mutableStateOf(false) }
    val statusSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    DisposableEffect(Unit) {
        viewModel.onScreenVisible()
        onDispose {
            viewModel.onScreenHidden()
            viewModel.reset()
        }
    }

    val permissionsManager = createPermissionsManager(object : PermissionCallback {
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
                else -> {
                    viewModel.showPermissionDialog(true)
                }
            }
        }
    })

    if (launchCamera) {
        if (permissionsManager.isPermissionGranted(PermissionType.CAMERA)) {
            viewModel.showScanner(true)
        } else {
            permissionsManager.askPermission(PermissionType.CAMERA)
        }
        launchCamera = false
    }

    if (launchSetting) {
        permissionsManager.launchSettings()
        launchSetting = false
    }

    // Permission rational dialog
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
        }
    )

    // Full-screen scanner
    if (uiState.showScanner) {
        Column(modifier = Modifier.fillMaxSize()) {
            BarcodeScannerScreen(
                format = KmpBarcodeFormat.QR_CODE,
                onResult = { rawValue ->
                    viewModel.onQrScanned(rawValue)
                },
                onClose = { viewModel.showScanner(false) }
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!uiState.hasExpensesQr) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = cardContainerColor())
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Función no disponible", style = titleMediumBold())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "La importación por CUFE está disponible para usuarios del beta expenses_qr.",
                        style = bodyMedium()
                    )
                }
            }
            return@Column
        }

        // Instructions card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importar por CUFE", style = titleMediumBold())
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Gratis por tiempo limitado",
                        style = labelSmall(color = MaterialTheme.colorScheme.onSecondary),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Escanee el código QR de la factura o ingrese el CUFE manualmente. " +
                        "Puede pegar el CUFE directamente, una URL de la DGI, o un enlace con el código.",
                    style = bodyMedium()
                )
                Spacer(modifier = Modifier.height(12.dp))
                ButtonM(
                    onClick = { launchCamera = true },
                    enabled = !uiState.isImporting && !uiState.isPolling,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Escanear Código QR")
                }
            }
        }

        // CUFE manual input
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("O ingrese manualmente", style = bodyMediumBold())
                Spacer(modifier = Modifier.height(8.dp))
                DMOutlinedTextField(
                    text = uiState.cufeInput,
                    label = "Pegue aquí el CUFE o URL",
                    modifier = Modifier,
                    onChange = { viewModel.setCufeInput(it) }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Formatos aceptados: FE..., URL con chFE=, URL con /FacturasPorCUFE/",
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(12.dp))
                ButtonM(
                    onClick = { viewModel.importCufe() },
                    enabled = !uiState.isImporting && !uiState.isPolling && uiState.cufeInput.isNotBlank()
                ) {
                    if (uiState.isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Importar Factura")
                }
            }
        }

        // Error
        uiState.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = bodyMedium()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (job != null) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!uiState.isPolling) {
                    viewModel.reset()
                }
            },
            sheetState = statusSheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            ImportStatusContent(
                uiState = uiState,
                onReset = { viewModel.reset() },
                onOpenImportedExpense = { viewModel.openImportedExpense(onExpenseImported) }
            )
        }
    }
}

@Composable
private fun ImportStatusContent(
    uiState: CufeImportState,
    onReset: () -> Unit,
    onOpenImportedExpense: () -> Unit
) {
    val job = uiState.currentJob ?: return
    val isInProgress = job.status == "pending" || job.status == "processing"
    val isSuccess = job.status == "success" || job.status == "completed"
    val isFailure = uiState.pollingTimedOut ||
        job.status == "failed" ||
        job.status == "error" ||
        job.status == "cancelled" ||
        job.status == "timeout"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Estado de importación", style = bodyMediumBold())

            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    isInProgress -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    isSuccess -> Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    isFailure -> Icon(
                        imageVector = Icons.Rounded.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = uiState.jobStatusLabel ?: "",
                    style = bodyMedium()
                )
            }

            if (uiState.isPolling || isInProgress) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "La importación se está procesando. Puede salir de esta pantalla, " +
                            "si finaliza correctamente el gasto aparecerá en su lista.",
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            if (isSuccess && uiState.importedExpenseId != null) {
                Spacer(modifier = Modifier.height(4.dp))
                ButtonM(
                    onClick = onOpenImportedExpense,
                    enabled = !uiState.isOpeningExpense
                ) {
                    if (uiState.isOpeningExpense) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Ver gasto importado")
                }
            }

            if (isFailure) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButtonM(onClick = onReset) {
                    Text("Intentar de nuevo")
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}
