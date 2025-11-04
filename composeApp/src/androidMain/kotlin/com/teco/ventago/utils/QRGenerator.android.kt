package com.teco.ventago.utils

import android.content.Context
import android.media.Image
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
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
import kotlin.getValue

actual fun generateQR(width: Int, height: Int, url: String): SharedImage {
    return SharedImage(QRCode.from(url).withSize(width, height).bitmap())
}

private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }
@Composable
actual fun CameraPreview(modifier: Modifier, onBarcode: (String) -> Unit) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                this.scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            ctx.bindCameraUseCases(previewView, onBarcode)
            previewView
        }
    )
}

val resolutionSelector = ResolutionSelector.Builder()
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

private fun Context.bindCameraUseCases(
    previewView: PreviewView,
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
            .setResolutionSelector(resolutionSelector)
            .build()
            .apply {
                setAnalyzer(cameraExecutor, StableBarcodeAnalyzer(onBarcode))
            }

        val selector = CameraSelector.DEFAULT_BACK_CAMERA

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            /* lifecycleOwner = */ getLifecycleOwner(previewView),
            selector,
            preview,
            analyzer
        )
    }, ContextCompat.getMainExecutor(this))
}

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
    private val stabilityMillis: Long = 300L,     // ~1.2s to confirm
    private val requiredHits: Int = 3,             // at least 3 frames matching
    private val centerTolerance: Float = 0.70f     // how close to center (0..1 of min dim)
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .enableAllPotentialBarcodes()
        .build()
    private val scanner = BarcodeScanning.getClient(options)

    // Confirmation state
    @Volatile private var handled = false
    private var candidateValue: String? = null
    private var firstSeenAt: Long = 0L
    private var hitCount: Int = 0

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
                        handled = true
                        onBarcode(value)
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

