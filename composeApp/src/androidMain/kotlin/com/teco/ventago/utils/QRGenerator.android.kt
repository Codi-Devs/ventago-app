package com.teco.ventago.utils

import android.content.Context
import android.media.Image
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.teco.ventago.core.camera.SharedImage
import net.glxn.qrgen.android.QRCode
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.getValue

actual fun generateQR(width: Int, height: Int, url: String): SharedImage {
    return SharedImage(QRCode.from(url).withSize(width, height).bitmap())
}

private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }
@Composable
actual fun CameraPreview(
    modifier: Modifier,
    singleShot: Boolean,
    torchEnabled: Boolean,
    scanMode: BarcodeScanMode,
    stabilityMillis: Long,
    requiredHits: Int,
    tapToFocus: Boolean,
    centerAutoFocus: Boolean,
    defaultZoomRatio: Float?,
    onBarcode: (String) -> Unit
) {
    val controller = remember { CameraPreviewController() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                clipToOutline = true
                setBackgroundColor(android.graphics.Color.BLACK)
            }

            if (tapToFocus) {
                previewView.installTapToFocus(controller)
            }
            if (centerAutoFocus) {
                previewView.startCenterAutoFocus(controller)
            }
            ctx.bindCameraUseCases(
                previewView = previewView,
                singleShot = singleShot,
                torchEnabled = torchEnabled,
                scanMode = scanMode,
                stabilityMillis = stabilityMillis,
                requiredHits = requiredHits,
                defaultZoomRatio = defaultZoomRatio,
                controller = controller,
                onBarcode = onBarcode
            )
            previewView
        },
        update = {
            controller.setTorchEnabled(torchEnabled)
        }
    )
}

private class CameraPreviewController {
    var camera: Camera? = null
        private set
    private var torchEnabled: Boolean = false

    fun setCamera(value: Camera?) {
        camera = value
        applyTorch()
    }

    fun setTorchEnabled(value: Boolean) {
        torchEnabled = value
        applyTorch()
    }

    private fun applyTorch() {
        val activeCamera = camera ?: return
        if (activeCamera.cameraInfo.hasFlashUnit()) {
            activeCamera.cameraControl.enableTorch(torchEnabled)
        }
    }
}

private val defaultResolutionSelector = ResolutionSelector.Builder()
    .setResolutionStrategy(
        ResolutionStrategy(
            android.util.Size(1280, 720),
            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
        )
    )
    .setAspectRatioStrategy(
        AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
    )
    .build()

private val retailProductResolutionSelector = ResolutionSelector.Builder()
    .setResolutionStrategy(
        ResolutionStrategy(
            android.util.Size(960, 540),
            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
        )
    )
    .setAspectRatioStrategy(
        AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
    )
    .build()

private fun Context.bindCameraUseCases(
    previewView: PreviewView,
    singleShot: Boolean,
    torchEnabled: Boolean,
    scanMode: BarcodeScanMode,
    stabilityMillis: Long,
    requiredHits: Int,
    defaultZoomRatio: Float?,
    controller: CameraPreviewController,
    onBarcode: (String) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val analyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(
                if (scanMode == BarcodeScanMode.RETAIL_PRODUCT) {
                    retailProductResolutionSelector
                } else {
                    defaultResolutionSelector
                }
            )
            .build()
            .apply {
                setAnalyzer(
                    cameraExecutor,
                    StableBarcodeAnalyzer(
                        onBarcode = onBarcode,
                        singleShot = singleShot,
                        scanMode = scanMode,
                        stabilityMillis = stabilityMillis,
                        requiredHits = requiredHits
                    )
                )
            }

        val selector = CameraSelector.DEFAULT_BACK_CAMERA

        cameraProvider.unbindAll()
        val camera = cameraProvider.bindToLifecycle(
            /* lifecycleOwner = */ getLifecycleOwner(previewView),
            selector,
            preview,
            analyzer
        )
        controller.setCamera(camera)
        controller.setTorchEnabled(torchEnabled)
        defaultZoomRatio?.let { camera.applyZoomRatio(it) }
    }, ContextCompat.getMainExecutor(this))
}

private fun Camera.applyZoomRatio(zoomRatio: Float) {
    val zoomState = cameraInfo.zoomState.value
    val minZoomRatio = zoomState?.minZoomRatio ?: 1f
    val maxZoomRatio = zoomState?.maxZoomRatio ?: zoomRatio
    cameraControl.setZoomRatio(zoomRatio.coerceIn(minZoomRatio, maxZoomRatio))
}

@Suppress("ClickableViewAccessibility")
private fun PreviewView.installTapToFocus(controller: CameraPreviewController) {
    setOnTouchListener { view, event ->
        if (event.action != MotionEvent.ACTION_UP) {
            return@setOnTouchListener true
        }
        controller.camera?.let { camera ->
            requestFocusAt(
                camera = camera,
                x = event.x,
                y = event.y
            )
        }
        view.performClick()
        true
    }
}

private fun PreviewView.startCenterAutoFocus(controller: CameraPreviewController) {
    val handler = Handler(Looper.getMainLooper())
    val focusRunnable = object : Runnable {
        override fun run() {
            controller.camera?.let { camera ->
                requestFocusAt(
                    camera = camera,
                    x = width / 2f,
                    y = height / 2f
                )
            }
            if (isAttachedToWindow) {
                handler.postDelayed(this, CENTER_AUTO_FOCUS_INTERVAL_MS)
            }
        }
    }
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            handler.removeCallbacks(focusRunnable)
            handler.postDelayed(focusRunnable, CENTER_AUTO_FOCUS_INITIAL_DELAY_MS)
        }

        override fun onViewDetachedFromWindow(v: View) {
            handler.removeCallbacks(focusRunnable)
        }
    })
    postDelayed(focusRunnable, CENTER_AUTO_FOCUS_INITIAL_DELAY_MS)
}

private fun PreviewView.requestFocusAt(
    camera: Camera,
    x: Float,
    y: Float
) {
    if (width <= 0 || height <= 0) return

    val point = meteringPointFactory.createPoint(x, y)
    val action = FocusMeteringAction.Builder(
        point,
        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
    )
        .setAutoCancelDuration(2, TimeUnit.SECONDS)
        .build()

    camera.cameraControl.startFocusAndMetering(action)
}

private const val CENTER_AUTO_FOCUS_INITIAL_DELAY_MS = 350L
private const val CENTER_AUTO_FOCUS_INTERVAL_MS = 2_500L

private fun getLifecycleOwner(view: PreviewView): androidx.lifecycle.LifecycleOwner {
    var ctx = view.context
    while (ctx is android.content.ContextWrapper) {
        if (ctx is androidx.lifecycle.LifecycleOwner) return ctx
        ctx = ctx.baseContext
    }
    throw IllegalStateException("PreviewView is not in a LifecycleOwner context.")
}

class StableBarcodeAnalyzer(
    private val onBarcode: (String) -> Unit,
    private val singleShot: Boolean = true,
    private val scanMode: BarcodeScanMode = BarcodeScanMode.ALL,
    private val stabilityMillis: Long = 300L,     // ~1.2s to confirm
    private val requiredHits: Int = 3,             // at least 3 frames matching
    private val centerTolerance: Float = 0.70f,    // how close to center (0..1 of min dim)
    private val repeatCooldownMillis: Long = 900L
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .apply {
            if (scanMode == BarcodeScanMode.RETAIL_PRODUCT) {
                setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_ITF,
                    Barcode.FORMAT_CODABAR
                )
            } else {
                setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                enableAllPotentialBarcodes()
            }
        }
        .build()
    private val scanner = BarcodeScanning.getClient(options)

    // Confirmation state
    @Volatile private var handled = false
    private var candidateValue: String? = null
    private var firstSeenAt: Long = 0L
    private var hitCount: Int = 0
    private var lastDeliveredValue: String? = null
    private var lastDeliveredAt: Long = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (handled) { imageProxy.close(); return }

        val mediaImage = imageProxy.image
        if (mediaImage == null) { imageProxy.close(); return }

        val img = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(img)
            .addOnSuccessListener { barcodes ->
                if (handled) return@addOnSuccessListener

                // Choose the most centered barcode (if any)
                val w = img.width.toFloat()
                val h = img.height.toFloat()
                val cx = w / 2f
                val cy = h / 2f
                val tolPx = centerTolerance * minOf(w, h)

                val centered = barcodes
                    .filter { it.boundingBox != null && !it.rawValue.isNullOrBlank() }
                    .sortedBy { b ->
                        val box = b.boundingBox!!
                        val bx = box.centerX().toFloat()
                        val by = box.centerY().toFloat()
                        val dx = bx - cx
                        val dy = by - cy
                        dx * dx + dy * dy // squared distance to center
                    }
                    .firstOrNull { b ->
                        val box = b.boundingBox!!
                        val dx = box.centerX().toFloat() - cx
                        val dy = box.centerY().toFloat() - cy
                        kotlin.math.sqrt(dx * dx + dy * dy) <= tolPx
                    }

                val value = centered?.rawValue
                val now = System.currentTimeMillis()

                if (value.isNullOrBlank()) {
                    // no centered candidate → reset soft counters (but keep confirmed=false)
                    resetCandidate()
                } else {
                    if (
                        !singleShot &&
                        value == lastDeliveredValue &&
                        now - lastDeliveredAt < repeatCooldownMillis
                    ) {
                        return@addOnSuccessListener
                    }

                    if (candidateValue == null || candidateValue != value) {
                        // new candidate
                        candidateValue = value
                        firstSeenAt = now
                        hitCount = 1
                    } else {
                        // same candidate again → accumulate
                        hitCount++
                    }

                    // Confirm either by time window + hits, or strictly by hits
                    val timeStable = (now - firstSeenAt) >= stabilityMillis
                    val hitsStable = hitCount >= requiredHits

                    if (timeStable && hitsStable) {
                        if (singleShot) {
                            handled = true
                        } else {
                            lastDeliveredValue = value
                            lastDeliveredAt = now
                        }
                        onBarcode(value)
                        resetCandidate()
                    }
                }
            }
            .addOnFailureListener {
                // ignore frame errors
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun resetCandidate() {
        candidateValue = null
        firstSeenAt = 0L
        hitCount = 0
    }

    fun reset() {
        handled = false
        lastDeliveredValue = null
        lastDeliveredAt = 0L
        resetCandidate()
    }
}

@Deprecated("Use StableBarcodeAnalyzer. This scans too quickly and may miss some barcodes.")
class BarcodeAnalyzer(
    private val onBarcode: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .enableAllPotentialBarcodes()
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    @Volatile private var handled = false   // simple debounce

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (handled) { imageProxy.close(); return }

        val mediaImage: Image? = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val value = barcodes.firstOrNull()?.rawValue
                    if (!value.isNullOrBlank()) {
                        handled = true
                        onBarcode(value)  // e.g., "INV-ADJS-002981882" or orderId
                    }
                }
                .addOnFailureListener { /* ignore frame */ }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }
}

private fun toAndroidFormat(fmt: KmpBarcodeFormat): BarcodeFormat = when (fmt) {
    KmpBarcodeFormat.CODE_128 -> BarcodeFormat.CODE_128
    KmpBarcodeFormat.QR_CODE  -> BarcodeFormat.QR_CODE
}

actual fun generateBarcodeImage(
    data: String,
    format: KmpBarcodeFormat,
    width: Int,
    height: Int,
    margin: Int
): ImageBitmap {
    val hints = mapOf(EncodeHintType.MARGIN to margin)
    val matrix: BitMatrix = MultiFormatWriter()
        .encode(data, toAndroidFormat(format), width, height, hints)

    val bmp = createBitmap(matrix.width, matrix.height)
    for (x in 0 until matrix.width) {
        for (y in 0 until matrix.height) {
            bmp[x, y] = if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
    }
    return bmp.asImageBitmap()
}
