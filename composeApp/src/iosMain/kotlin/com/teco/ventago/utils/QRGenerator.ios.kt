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
import platform.CoreMedia.CMSampleBufferRef
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_t
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey

@OptIn(ExperimentalForeignApi::class)
actual fun generateQR(width: Int, height: Int, url: String): SharedImage {
    val data = url.encodeToByteArray()

    val filter = CIFilter.filterWithName("CIQRCodeGenerator")
        ?: error("Unable to create CIQRCodeGenerator filter")
    filter.setDefaults()
    filter.setValue(data, forKey = "inputMessage")
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

@OptIn(ExperimentalForeignApi::class)
private fun makeCIImage_CODE128(
    data: String,
    width: Int,
    height: Int,
    margin: Int
): CIImage? {
    val message = (data as NSString).dataUsingEncoding(NSASCIIStringEncoding) ?: return null
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

@OptIn(ExperimentalForeignApi::class)
private fun makeCIImage_QR(
    data: String,
    width: Int,
    height: Int,
    margin: Int
): CIImage? {
    val message = (data as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return null
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
    onBarcode: (String) -> Unit
) {
    UIKitView(
        factory = {
            val host = PreviewHostView(frame = CGRectZero.readValue())
            val coordinator = CameraCoordinator(host, onBarcode)
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
private class PreviewHostView @OptIn(ExperimentalForeignApi::class) constructor(
    frame: CValue<CGRect>
) : UIView(frame) {
    private var coordinator: CameraCoordinator? = null

    fun attach(c: CameraCoordinator) {
        coordinator = c
        layer.addSublayer(c.previewLayer)
        c.start()
        setNeedsLayout()
        layoutIfNeeded()
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

/** Manages AVCaptureSession + MLKit scanning */
@OptIn(ExperimentalForeignApi::class)
private class CameraCoordinator(
    private val hostView: UIView,
    private val onBarcode: (String) -> Unit,
    private val singleShot: Boolean = true
) : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {

    private val session = AVCaptureSession().apply {
        sessionPreset = AVCaptureSessionPreset1280x720
    }
    private val device: AVCaptureDevice? = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
    private var input: AVCaptureDeviceInput? = null
    private val output = AVCaptureVideoDataOutput().apply { alwaysDiscardsLateVideoFrames = true }
    private val queue: dispatch_queue_t = dispatch_queue_create("mlkit.barcode.queue", null)

    val previewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
        videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    // MLKit (CODE_128 only; OR more formats if you need)
//    private val scanner: MLKBarcodeScanner = run {
//        val options = MLKBarcodeScannerOptions(formats = MLKBarcodeFormatCode128)
//        MLKBarcodeScanner.barcodeScannerWithOptions(options)
//    }

    private var isProcessing = false
    private var hasDelivered = false

    fun start() {
        if (session.running) return

        device?.let { dev ->
            val created = runCatching { AVCaptureDeviceInput(device = dev, error = null) }.getOrNull()
            input = created
            if (created != null && session.canAddInput(created)) session.addInput(created)
        }

        if (session.canAddOutput(output)) session.addOutput(output)

        // BGRA pixel format (NSDictionary<CFString, Any>)
        output.videoSettings = mapOf(
            kCVPixelBufferPixelFormatTypeKey to NSNumber(unsignedInt = kCVPixelFormatType_32BGRA)
        )

        output.setSampleBufferDelegate(this, queue)

        session.startRunning()
    }

    fun stop() {
        if (session.running) session.stopRunning()
    }

    // NOTE: buffer is nullable in the protocol signature
    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection
    ) {
        if (didOutputSampleBuffer == null) return
        if (isProcessing) return
        if (singleShot && hasDelivered) return

        isProcessing = true
//        val image = MLKVisionImage(buffer = didOutputSampleBuffer).apply {
//            orientation = currentImageOrientation()
//        } as MLKCompatibleImageProtocol

        // Use async API for widest compatibility
//        scanner.processImage(image) { barcodes, error ->
//            try {
//                if (error != null) return@processImage
//                val list = (barcodes as? List<*>) ?: return@processImage
//                val first = list.firstOrNull() as? MLKBarcode ?: return@processImage
//                val value = first.rawValue ?: return@processImage
//                if (value.isNotEmpty()) {
//                    if (singleShot) {
//                        hasDelivered = true
//                        runOnMain { stop() }
//                    }
//                    runOnMain { onBarcode(value) }
//                }
//            } finally {
//                isProcessing = false
//            }
//        }
    }

    private fun currentImageOrientation(): UIImageOrientation {
        val deviceOrientation = UIDevice.currentDevice.orientation
        val cameraPosition = device?.position ?: AVCaptureDevicePositionBack
        return when (deviceOrientation) {
            UIDeviceOrientation.UIDeviceOrientationPortrait ->
                if (cameraPosition == AVCaptureDevicePositionFront)
                    UIImageOrientation.UIImageOrientationLeftMirrored
                else UIImageOrientation.UIImageOrientationRight
            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft ->
                if (cameraPosition == AVCaptureDevicePositionFront)
                    UIImageOrientation.UIImageOrientationDownMirrored
                else UIImageOrientation.UIImageOrientationUp
            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown ->
                if (cameraPosition == AVCaptureDevicePositionFront)
                    UIImageOrientation.UIImageOrientationRightMirrored
                else UIImageOrientation.UIImageOrientationLeft
            UIDeviceOrientation.UIDeviceOrientationLandscapeRight ->
                if (cameraPosition == AVCaptureDevicePositionFront)
                    UIImageOrientation.UIImageOrientationUpMirrored
                else UIImageOrientation.UIImageOrientationDown
            else -> UIImageOrientation.UIImageOrientationUp
        }
    }
}

private fun runOnMain(block: () -> Unit) {
    dispatch_async(dispatch_get_main_queue()) { block() }
}
