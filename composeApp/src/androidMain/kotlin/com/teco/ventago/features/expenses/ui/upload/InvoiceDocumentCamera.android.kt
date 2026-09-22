package com.teco.ventago.features.expenses.ui.upload

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.teco.ventago.design_system.theme.bodyMedium
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
actual fun InvoiceDocumentCamera(
    onCaptured: (ByteArray) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            setBackgroundColor(android.graphics.Color.BLACK)
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(100)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                    .build()
            )
            .build()
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var capturing by remember { mutableStateOf(false) }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val focusHandler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var cameraProvider: ProcessCameraProvider? = null
        val bindListener = Runnable {
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { useCase ->
                useCase.surfaceProvider = previewView.surfaceProvider
            }
            cameraProvider?.unbindAll()
            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        }
        cameraProviderFuture.addListener(bindListener, mainExecutor)
        onDispose {
            focusHandler.removeCallbacksAndMessages(null)
            cameraProvider?.unbindAll()
            camera = null
        }
    }

    fun requestFocus(x: Float, y: Float, onSettled: (() -> Unit)? = null) {
        val activeCamera = camera ?: run {
            onSettled?.invoke()
            return
        }
        if (previewView.width <= 0 || previewView.height <= 0) {
            onSettled?.invoke()
            return
        }
        val point = previewView.meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        )
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        val future = activeCamera.cameraControl.startFocusAndMetering(action)
        if (onSettled == null) return
        future.addListener({
            focusHandler.postDelayed({ onSettled() }, 200)
        }, mainExecutor)
    }

    fun takePhoto() {
        if (capturing) return
        capturing = true
        val saveAndFinish = {
            val photoFile = File(context.cacheDir, "factura_${System.currentTimeMillis()}.jpg")
            val output = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            imageCapture.takePicture(
                output,
                mainExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        capturing = false
                        val bytes = runCatching { photoFile.readBytes() }.getOrNull()
                        if (bytes != null && bytes.isNotEmpty()) {
                            onCaptured(bytes)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        capturing = false
                    }
                }
            )
        }
        requestFocus(
            x = previewView.width / 2f,
            y = previewView.height / 2f,
            onSettled = saveAndFinish
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = {
                previewView.apply {
                    @Suppress("ClickableViewAccessibility")
                    setOnTouchListener { view, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            requestFocus(event.x, event.y)
                            view.performClick()
                        }
                        true
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Cerrar",
                tint = Color.White
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Acerca la factura y espera a que se vea nítida. Toca la pantalla para enfocar.",
                style = bodyMedium(color = Color.White),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            FloatingActionButton(
                onClick = { takePhoto() },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (capturing) 28.dp else 56.dp)
                        .background(
                            color = Color.White,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
