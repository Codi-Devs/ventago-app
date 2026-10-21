package com.teco.ventago.features.printers.domain

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.mp.KoinPlatform.getKoin
import java.io.IOException
import java.util.UUID

private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

/**
 * Puerto serie Bluetooth (SPP) de un dispositivo ya emparejado.
 * Las MPT-II a veces rechazan el socket seguro; hay dos respaldos.
 */
@SuppressLint("MissingPermission")
actual suspend fun sendBluetoothEscPos(
    payload: ByteArray,
    deviceId: String?,
): BluetoothPrintOutcome = withContext(Dispatchers.IO) {
    if (payload.isEmpty()) {
        return@withContext BluetoothPrintOutcome(false, "No hay contenido para imprimir.")
    }
    val adapter = bluetoothAdapter()
        ?: return@withContext BluetoothPrintOutcome(false, "Este celular no tiene Bluetooth.")
    if (!adapter.isEnabled) {
        return@withContext BluetoothPrintOutcome(false, "Bluetooth está apagado. Enciéndelo y vuelve a intentar.")
    }
    if (!hasBluetoothConnectPermission()) {
        return@withContext BluetoothPrintOutcome(false, "Falta permiso de Bluetooth para VentaGO.")
    }

    val bonded = try {
        adapter.bondedDevices.orEmpty().toList()
    } catch (_: SecurityException) {
        return@withContext BluetoothPrintOutcome(false, "Falta permiso de Bluetooth para VentaGO.")
    }
    val targets = bonded
        .filter { deviceId.isNullOrBlank() || it.address.equals(deviceId, ignoreCase = true) }
        .sortedByDescending { printerScore(it) }
    if (targets.isEmpty()) {
        val message = if (deviceId.isNullOrBlank()) {
            "No hay dispositivos emparejados. Empareja la impresora en Ajustes de Bluetooth."
        } else {
            "No encuentro la impresora de este equipo. Vuelve a emparejarla."
        }
        return@withContext BluetoothPrintOutcome(false, message)
    }

    val failures = mutableListOf<String>()
    for (device in targets) {
        val name = device.name?.takeIf { it.isNotBlank() } ?: device.address
        val connected = connect(adapter, device)
        if (connected.isFailure) {
            val error = connected.exceptionOrNull()
            failures += "$name: ${error?.message ?: "sin conexión"}"
            continue
        }
        val (socket, method) = connected.getOrThrow()
        try {
            val output = socket.outputStream
            output.write(payload)
            output.flush()
            Thread.sleep((800L + payload.size / 16L).coerceAtMost(8_000L))
            return@withContext BluetoothPrintOutcome(
                success = true,
                message = "Listo. Imprimí en $name ($method).",
                printerName = name,
            )
        } catch (error: Exception) {
            failures += "$name: ${error.message ?: error.javaClass.simpleName}"
        } finally {
            runCatching { socket.close() }
        }
    }
    BluetoothPrintOutcome(false, "No pude imprimir. ${failures.joinToString(" | ")}")
}

@SuppressLint("MissingPermission")
actual suspend fun listBluetoothPrinters(): BluetoothPrinterAvailability = withContext(Dispatchers.IO) {
    val adapter = bluetoothAdapter()
        ?: return@withContext BluetoothPrinterAvailability(
            enabled = false,
            message = "Este celular no tiene Bluetooth.",
        )
    if (!adapter.isEnabled) {
        return@withContext BluetoothPrinterAvailability(
            enabled = false,
            message = "Bluetooth está apagado.",
        )
    }
    if (!hasBluetoothConnectPermission()) {
        return@withContext BluetoothPrinterAvailability(
            enabled = true,
            needsPermission = true,
            message = "VentaGO necesita permiso de Bluetooth.",
        )
    }
    val bonded = try {
        adapter.bondedDevices.orEmpty().toList()
    } catch (_: SecurityException) {
        return@withContext BluetoothPrinterAvailability(
            enabled = true,
            needsPermission = true,
            message = "VentaGO necesita permiso de Bluetooth.",
        )
    }
    val printers = bonded
        .map { device -> device to printerScore(device) }
        .filter { it.second >= 20 }
        .sortedByDescending { it.second }
        .map { (device, _) ->
            NearbyBluetoothPrinter(
                id = device.address,
                name = device.name?.takeIf { it.isNotBlank() } ?: device.address,
                pairedOnly = !device.isClassicConnected(),
            )
        }
    BluetoothPrinterAvailability(enabled = true, printers = printers)
}

@SuppressLint("MissingPermission")
actual suspend fun isSavedBluetoothPrinterAvailable(deviceId: String): Boolean = withContext(Dispatchers.IO) {
    if (deviceId.isBlank()) return@withContext false
    val adapter = bluetoothAdapter() ?: return@withContext false
    if (!adapter.isEnabled || !hasBluetoothConnectPermission()) return@withContext false
    val device = try {
        adapter.bondedDevices.orEmpty().firstOrNull { it.address.equals(deviceId, ignoreCase = true) }
    } catch (_: SecurityException) {
        null
    } ?: return@withContext false
    device.isClassicConnected()
}

actual fun openBluetoothSettings() {
    startSettings(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
}

actual fun openAppSettings() {
    val context = appContext()
    startSettings(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
    )
}

@Suppress("DEPRECATION")
private fun bluetoothAdapter(): BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

private fun hasBluetoothConnectPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    return ContextCompat.checkSelfPermission(
        appContext(),
        Manifest.permission.BLUETOOTH_CONNECT,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun appContext(): Context = getKoin().get()

private fun startSettings(intent: Intent) {
    appContext().startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@SuppressLint("MissingPermission")
private fun BluetoothDevice.isClassicConnected(): Boolean {
    val reflected = runCatching {
        val method = javaClass.getMethod("isConnected")
        method.invoke(this) as Boolean
    }.getOrDefault(false)
    if (reflected) return true
    val manager = appContext().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return false
    return intArrayOf(BluetoothProfile.HEADSET, BluetoothProfile.A2DP, BluetoothProfile.GATT).any { profile ->
        runCatching {
            manager.getConnectionState(this, profile) == BluetoothProfile.STATE_CONNECTED
        }.getOrDefault(false)
    }
}

@SuppressLint("MissingPermission")
private fun printerScore(device: BluetoothDevice): Int {
    var score = bluetoothPrinterNameScore(device.name)
    val major = runCatching { device.bluetoothClass?.majorDeviceClass }.getOrNull()
    if (major == BluetoothClass.Device.Major.IMAGING) score += 40
    return score
}

@SuppressLint("MissingPermission")
private suspend fun connect(
    adapter: BluetoothAdapter,
    device: BluetoothDevice,
): Result<Pair<BluetoothSocket, String>> {
    val failures = mutableListOf<String>()
    for ((label, open) in socketFactories(device)) {
        val socket = try {
            open()
        } catch (error: Exception) {
            failures += "$label: ${error.message ?: error.javaClass.simpleName}"
            continue
        }
        try {
            withTimeout(12_000) {
                withContext(Dispatchers.IO) {
                    runCatching { adapter.cancelDiscovery() }
                    socket.connect()
                }
            }
            return Result.success(socket to label)
        } catch (_: TimeoutCancellationException) {
            failures += "$label: tiempo agotado"
            runCatching { socket.close() }
        } catch (error: Exception) {
            failures += "$label: ${error.message ?: error.javaClass.simpleName}"
            runCatching { socket.close() }
        }
    }
    return Result.failure(IOException(failures.joinToString("; ").ifBlank { "sin conexión" }))
}

@SuppressLint("MissingPermission")
private fun socketFactories(device: BluetoothDevice): List<Pair<String, () -> BluetoothSocket>> = listOf(
    "SPP seguro" to { device.createRfcommSocketToServiceRecord(SPP_UUID) },
    "SPP inseguro" to { device.createInsecureRfcommSocketToServiceRecord(SPP_UUID) },
    "canal 1" to {
        val method = device.javaClass.getMethod("createRfcommSocket", Integer.TYPE)
        method.invoke(device, 1) as BluetoothSocket
    },
)
