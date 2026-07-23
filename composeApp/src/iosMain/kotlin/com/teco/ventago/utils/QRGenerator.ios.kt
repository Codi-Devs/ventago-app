package com.teco.ventago.utils

import com.teco.ventago.core.camera.SharedImage
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreImage.*
import platform.Foundation.*
import platform.UIKit.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.cinterop.*
import platform.AVFoundation.*
import platform.CoreGraphics.*

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import platform.UIKit.UIView

//import androidx.compose.ui.graphics.ImageBitmap
//import androidx.compose.ui.graphics.asImageBitmap
import org.jetbrains.skia.Image
import platform.UIKit.UIImage
import platform.CoreGraphics.CGRectMake
import platform.posix.memcpy

//import cocoapods.MLKitBarcodeScanning.MLKBarcode
//
//import cocoapods.MLKitBarcodeScanning.MLKBarcodeFormat
//import cocoapods.MLKitBarcodeScanning.MLKBarcodeFormatCode128
//import cocoapods.MLKitBarcodeScanning.MLKBarcodeScanner
//import cocoapods.MLKitBarcodeScanning.MLKBarcodeScannerOptions
//import cocoapods.MLKitVision.MLKVisionImage
//import objcnames.protocols.MLKCompatibleImageProtocol
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_t

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun generateQR(width: Int, height: Int, url: String): SharedImage {
    val message = NSString.create(string = url).dataUsingEncoding(NSUTF8StringEncoding)
        ?: error("Unable to encode QR input")

    val filter = CIFilter.filterWithName("CIQRCodeGenerator")
        ?: error("Unable to create CIQRCodeGenerator filter")
    filter.setDefaults()
    filter.setValue(message, forKey = "inputMessage")
    filter.setValue("H", forKey = "inputCorrectionLevel") // L, M, Q, H

    val ciImage = filter.outputImage
        ?: error("QR code generation failed")

    // Scale it to desired size
    val transform = CGAffineTransformMakeScale(
        width.toDouble() / ciImage.extent.size,
        height.toDouble() / ciImage.extent.size
    )
    val scaledImage = ciImage.imageByApplyingTransform(transform)

    val context = CIContext.context()
    val cgImage = context.createCGImage(scaledImage, fromRect = scaledImage.extent)
        ?: error("Failed to create CGImage from QR CIImage")

    val uiImage = UIImage.imageWithCGImage(cgImage)

    return SharedImage(uiImage)
}
actual fun generateBarcodeImage(
    data: String,
    format: KmpBarcodeFormat,
    width: Int,
    height: Int,
    margin: Int
): ImageBitmap {
    val ci: CIImage? = when (format) {
        KmpBarcodeFormat.CODE_128 -> makeCIImage_CODE128(data, width, height, margin)
        KmpBarcodeFormat.QR_CODE  -> makeCIImage_QR(data, width, height, margin)
    }
    requireNotNull(ci) { "Failed to build CIImage for $format" }
    val uiImage = renderCIImageToUIImage(ci!!, width, height)
    return uiImageToImageBitmap(uiImage)
}

/**
 * Compose entry-point used by shared UI to render a live camera
 * preview and get barcode results on iOS.
 */

@OptIn(ExperimentalForeignApi::class)
private fun renderCIImageToUIImage(ciImage: CIImage, width: Int, height: Int): UIImage {
    val context = CIContext.contextWithOptions(null)
    val rect = CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble())
    val cgImage = context.createCGImage(ciImage, rect)!!
    return UIImage.imageWithCGImage(cgImage)
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(this.length.toInt())
    bytes.usePinned { pinned ->
        memScoped {
            memcpy(pinned.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
        }
    }
    return bytes
}

private fun uiImageToImageBitmap(uiImage: UIImage): ImageBitmap {
    // Encode to PNG then decode with Skia (Compose for iOS)
    val data: NSData? = UIImagePNGRepresentation(uiImage)
    requireNotNull(data) { "Failed to encode UIImage to PNG" }
    val skiaImage = Image.makeFromEncoded(data.toByteArray())
    return skiaImage.toComposeImageBitmap()
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun makeCIImage_CODE128(
    data: String,
    width: Int,
    height: Int,
    margin: Int
): CIImage? {
    val message = NSString.create(string = data).dataUsingEncoding(NSASCIIStringEncoding) ?: return null
    val filter = CIFilter.filterWithName("CICode128BarcodeGenerator") as? CIFilter ?: return null
    filter.setValue(message, forKey = "inputMessage")
    // Quiet zone; Core Image uses points;  default ~7. Adjust if desired:
    filter.setValue(NSNumber(int = margin), forKey = "inputQuietSpace")

    val ci = filter.outputImage ?: return null

    // Scale CIImage to desired output size
    val extent = ci.extent()
    val sx = width.toDouble() / extent.size
    val sy = height.toDouble() / extent.size
    return ci.imageByApplyingTransform(CGAffineTransformMakeScale(sx, sy))
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun makeCIImage_QR(
    data: String,
    width: Int,
    height: Int,
    margin: Int
): CIImage? {
    val message = NSString.create(string = data).dataUsingEncoding(NSUTF8StringEncoding) ?: return null
    val filter = CIFilter.filterWithName("CIQRCodeGenerator") as? CIFilter ?: return null
    filter.setValue(message, forKey = "inputMessage")
    filter.setValue("M", forKey = "inputCorrectionLevel") // L/M/Q/H

    val ci = filter.outputImage ?: return null
    // Add a white background + margin by compositing if you like; simplest is just scale:
    val extent = ci.extent()
    val sx = width.toDouble() / extent.size
    val sy = height.toDouble() / extent.size
    return ci.imageByApplyingTransform(CGAffineTransformMakeScale(sx, sy))
}



@OptIn(ExperimentalForeignApi::class)
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
    UIKitView(
        factory = {
            val host = PreviewHostView(frame = CGRectZero.readValue())
            val coordinator = CameraCoordinator(
                hostView = host,
                onBarcode = onBarcode,
                singleShot = singleShot
            )
            host.attach(coordinator)
            host
        },
        modifier = modifier,
        update = { /* no-op */ },
        onRelease = { (it as? PreviewHostView)?.detach() },
        properties = UIKitInteropProperties(isInteractive = true, isNativeAccessibilityEnabled = true)
    )
}

/** Simple UIView that owns the preview layer */
@OptIn(ExperimentalForeignApi::class)
private class PreviewHostView @OptIn(ExperimentalForeignApi::class) constructor(
    frame: CValue<CGRect>
) : UIView(frame) {
    private var coordinator: CameraCoordinator? = null

    fun attach(c: CameraCoordinator) {
        coordinator = c
        c.previewLayer.frame = bounds
        layer.addSublayer(c.previewLayer)
        c.start()
        setNeedsLayout()
    }
    fun detach() {
        coordinator?.stop()
        coordinator?.previewLayer?.removeFromSuperlayer()
        coordinator = null
    }
    @OptIn(ExperimentalForeignApi::class)
    override fun layoutSubviews() {
        super.layoutSubviews()
        coordinator?.previewLayer?.frame = bounds
    }
}

/** Manages AVCaptureSession + native iOS metadata scanning. */
@OptIn(ExperimentalForeignApi::class)
private class CameraCoordinator(
    private val hostView: UIView,
    private val onBarcode: (String) -> Unit,
    private val singleShot: Boolean = true
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    private val session = AVCaptureSession().apply {
        sessionPreset = AVCaptureSessionPreset1280x720
    }
    private val device: AVCaptureDevice? = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
    private var input: AVCaptureDeviceInput? = null
    private val metadataOutput = AVCaptureMetadataOutput()
    private val sessionQueue: dispatch_queue_t = dispatch_queue_create("com.teco.ventago.qr.session", null)
    private val metadataQueue: dispatch_queue_t = dispatch_queue_create("com.teco.ventago.qr.metadata", null)

    val previewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
        videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    private var isConfigured = false
    private var isStarted = false
    private var hasDelivered = false
    private var lastDeliveredValue: String? = null
    private var lastDeliveredAt: Double = 0.0
    private val repeatCooldownSeconds = 0.9

    fun start() {
        dispatch_async(sessionQueue) {
            if (isStarted) return@dispatch_async
            if (!configureSessionIfNeeded()) return@dispatch_async

            isStarted = true
            if (!session.running) {
                session.startRunning()
            }
        }
    }

    fun stop() {
        dispatch_async(sessionQueue) {
            isStarted = false
            if (session.running) {
                session.stopRunning()
            }
        }
    }

    private fun configureSessionIfNeeded(): Boolean {
        if (isConfigured) return true

        val dev = device ?: return false
        val created = runCatching { AVCaptureDeviceInput(device = dev, error = null) }.getOrNull()
            ?: return false

        if (!session.canAddInput(created) || !session.canAddOutput(metadataOutput)) {
            return false
        }

        session.beginConfiguration()
        session.addInput(created)
        session.addOutput(metadataOutput)

        val selectedTypes = supportedMetadataTypes(metadataOutput.availableMetadataObjectTypes)
        if (selectedTypes.isNotEmpty()) {
            metadataOutput.metadataObjectTypes = selectedTypes
            metadataOutput.setMetadataObjectsDelegate(this, queue = metadataQueue)
            input = created
            isConfigured = true
        }

        session.commitConfiguration()
        return isConfigured
    }

    private fun supportedMetadataTypes(availableTypes: List<*>): List<String> {
        val requestedTypes = listOfNotNull(
            AVMetadataObjectTypeQRCode,
            AVMetadataObjectTypeCode128Code,
            AVMetadataObjectTypeEAN13Code,
            AVMetadataObjectTypeEAN8Code,
            AVMetadataObjectTypeUPCECode,
            AVMetadataObjectTypeCode39Code,
            AVMetadataObjectTypeCode93Code,
            AVMetadataObjectTypePDF417Code,
            AVMetadataObjectTypeAztecCode,
            AVMetadataObjectTypeDataMatrixCode,
            AVMetadataObjectTypeITF14Code,
            AVMetadataObjectTypeInterleaved2of5Code
        )

        return requestedTypes.filter { availableTypes.contains(it) }
    }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        if (singleShot && hasDelivered) return

        val value = didOutputMetadataObjects
            .asSequence()
            .mapNotNull { (it as? AVMetadataMachineReadableCodeObject)?.stringValue }
            .firstOrNull { it.isNotBlank() }
        if (value == null) {
            lastDeliveredValue = null
            return
        }

        val now = NSDate().timeIntervalSince1970
        if (
            !singleShot &&
            value == lastDeliveredValue &&
            now - lastDeliveredAt < repeatCooldownSeconds
        ) {
            return
        }

        if (singleShot) {
            hasDelivered = true
        } else {
            lastDeliveredValue = value
            lastDeliveredAt = now
        }

        runOnMain {
            if (singleShot) stop()
            onBarcode(value)
        }
    }
}

private fun runOnMain(block: () -> Unit) {
    dispatch_async(dispatch_get_main_queue()) { block() }
}
