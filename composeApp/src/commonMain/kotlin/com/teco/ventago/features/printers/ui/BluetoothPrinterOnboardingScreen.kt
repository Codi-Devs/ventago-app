package com.teco.ventago.features.printers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.teco.ventago.core.SnackbarService
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.printers.domain.NearbyBluetoothPrinter
import com.teco.ventago.features.printers.domain.openAppSettings
import com.teco.ventago.features.printers.domain.openBluetoothSettings
import com.teco.ventago.features.printers.ui.viewmodel.BluetoothPrinterOnboardingUiEvent
import com.teco.ventago.features.printers.ui.viewmodel.BluetoothPrinterOnboardingViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BluetoothPrinterOnboardingScreen(
    onLinked: () -> Unit,
    onOpenNetworkOnboarding: () -> Unit = {},
    viewModel: BluetoothPrinterOnboardingViewModel = koinViewModel(),
) {
    val snackbarService: SnackbarService = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    BluetoothPermissionEffect(
        onGranted = viewModel::refresh,
        onDenied = viewModel::onPermissionDenied,
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BluetoothPrinterOnboardingUiEvent.Linked -> {
                    snackbarService.show("Impresora lista en este equipo.")
                    onLinked()
                }
            }
        }
    }

    when {
        uiState.loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        uiState.permissionDenied -> {
            BluetoothStatusCard(
                icon = Icons.Rounded.BluetoothDisabled,
                title = "Falta permiso de Bluetooth",
                body = "Actívalo para que VentaGO encuentre la impresora de este celular.",
                action = "Dar permiso",
                onAction = ::openAppSettings,
            )
        }

        !uiState.bluetoothEnabled -> {
            val secondary = MaterialTheme.colorScheme.secondary
            BluetoothStatusCard(
                icon = Icons.Rounded.BluetoothDisabled,
                title = "Bluetooth apagado",
                body = "Enciéndelo para buscar la impresora que ya emparejaste.",
                action = "Activar Bluetooth",
                onAction = ::openBluetoothSettings,
                accent = secondary,
                iconBackground = vanishedBackgroundColor(),
                buttonContainer = secondary,
            )
        }

        uiState.printers.isEmpty() -> {
            BluetoothStatusCard(
                icon = Icons.Rounded.Print,
                title = "No hay impresora",
                body = "Asegúrate de tener la impresora conectada.",
                action = "Buscar de nuevo",
                onAction = viewModel::refresh,
                secondaryAction = "Configurar impresora de red",
                onSecondaryAction = onOpenNetworkOnboarding,
            )
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("Impresoras de este celular", style = bodyMediumBold())
                    Text(
                        "Toca una para dejarla lista. Solo imprime en este equipo.",
                        style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(uiState.printers, key = { it.id }) { printer ->
                    BluetoothPrinterRow(
                        printer = printer,
                        linked = printer.id == uiState.linkedId,
                        enabled = !uiState.linking,
                        onClick = { viewModel.link(printer) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BluetoothStatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    accent: Color = MaterialTheme.colorScheme.primary,
    iconBackground: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    buttonContainer: Color = MaterialTheme.colorScheme.primary,
    secondaryAction: String? = null,
    onSecondaryAction: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(24.dp),
            color = iconBackground,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Text(title, style = bodyMediumBold(color = accent))
        Text(
            body,
            style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
        )
        ButtonM(onClick = onAction, containerColor = buttonContainer) {
            Text(action)
        }
        if (secondaryAction != null) {
            TextButtonS(label = secondaryAction, color = MaterialTheme.colorScheme.secondary, onClick = onSecondaryAction)
        }
    }
}

@Composable
private fun BluetoothPrinterRow(
    printer: NearbyBluetoothPrinter,
    linked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = vanishedBackgroundColor(),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Bluetooth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    printer.name,
                    style = bodyMediumBold(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (linked) {
                    Text(
                        "Lista en este equipo",
                        style = bodySmall(color = MaterialTheme.colorScheme.secondary),
                    )
                }
            }
            if (linked && printer.pairedOnly) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = vanishedBackgroundColor(),
                ) {
                    Text(
                        "Vinculado",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = labelSmall(color = MaterialTheme.colorScheme.secondary),
                    )
                }
            }
        }
    }
}
