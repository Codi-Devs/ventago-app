package com.teco.ventago.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teco.ventago.core.camera.SharedImage


enum class KmpBarcodeFormat { CODE_128, QR_CODE /*, EAN_13 (Android only unless we add custom) */ }

enum class BarcodeScanMode {
    ALL,
    RETAIL_PRODUCT
}

expect fun generateQR(width: Int, height: Int, url: String): SharedImage

@Composable
expect fun CameraPreview(
    modifier: Modifier = Modifier,
    singleShot: Boolean = true,
    torchEnabled: Boolean = false,
    scanMode: BarcodeScanMode = BarcodeScanMode.ALL,
    stabilityMillis: Long = 300L,
    requiredHits: Int = 3,
    tapToFocus: Boolean = false,
    centerAutoFocus: Boolean = false,
    defaultZoomRatio: Float? = null,
    onBarcode: (String) -> Unit
)


@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    format: KmpBarcodeFormat = KmpBarcodeFormat.CODE_128,
    onResult: (String) -> Unit,
    onClose: () -> Unit
) {
    var hasCam by remember { mutableStateOf(false) }

    // Request permission (Accompanist) or your own handler
    hasCam = remember { true } // <-- replace with real permission check

    if (!hasCam) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("Camera permission is required.")
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        val (cutoutWidthFraction, cutoutAspectRatio) = when (format) {
            KmpBarcodeFormat.QR_CODE -> 0.72f to 1f
            KmpBarcodeFormat.CODE_128 -> 0.8f to 2.2f
        }

        CameraPreview(
            modifier = Modifier.matchParentSize(),
            onBarcode = {
                println("ASDASD: barcode Result = $it")
                onResult(it) }
        )

        // Centered scrim + cutout
        ScannerScrimWithCutout(
            modifier = Modifier.matchParentSize(),
            cutoutWidthFraction = cutoutWidthFraction,
            cutoutAspectRatio = cutoutAspectRatio
        )

        // Corner brackets
        ScannerOverlay(
            modifier = Modifier.matchParentSize(),
            cutoutWidthFraction = cutoutWidthFraction,
            cutoutAspectRatio = cutoutAspectRatio
        )
    }
}

@Composable
fun ScannerOverlay(
    modifier: Modifier = Modifier,
    cornerLengthDp: Dp = 28.dp,
    strokeWidthDp: Dp = 4.dp,
    cornerColor: Color = Color(0xFF34A853),
    cutoutWidthFraction: Float = 0.8f,
    cutoutAspectRatio: Float = 2.2f
) {
    Canvas(modifier = modifier) {
        val cw = size.width * cutoutWidthFraction
        val ch = cw / cutoutAspectRatio
        val left = (size.width - cw) / 2f
        val top  = (size.height - ch) / 2f
        val right = left + cw
        val bottom = top + ch

        val len = cornerLengthDp.toPx()
        val stroke = strokeWidthDp.toPx()

        fun line(a: Offset, b: Offset) = drawLine(
            color = cornerColor, start = a, end = b,
            strokeWidth = stroke, cap = StrokeCap.Round
        )

        // TL
        line(Offset(left, top), Offset(left + len, top))
        line(Offset(left, top), Offset(left, top + len))
        // TR
        line(Offset(right - len, top), Offset(right, top))
        line(Offset(right, top), Offset(right, top + len))
        // BL
        line(Offset(left, bottom - len), Offset(left, bottom))
        line(Offset(left, bottom), Offset(left + len, bottom))
        // BR
        line(Offset(right - len, bottom), Offset(right, bottom))
        line(Offset(right, bottom - len), Offset(right, bottom))
    }
}

@Composable
fun ScannerScrimWithCutout(
    modifier: Modifier = Modifier,
    cutoutWidthFraction: Float = 0.8f, // 80% of screen width
    cutoutAspectRatio: Float = 2.2f,
    cornerRadiusDp: Dp = 12.dp,
    scrimColor: Color = Color(0x99000000) // semi‑transparent black
) {
    Canvas(modifier = modifier) {
        val r = cornerRadiusDp.toPx()
        val cw = size.width * cutoutWidthFraction
        val ch = cw / cutoutAspectRatio
        val left = (size.width - cw) / 2f
        val top  = (size.height - ch) / 2f
        val right = left + cw
        val bottom = top + ch

        val rr = RoundRect(
            left = left, top = top, right = right, bottom = bottom,
            cornerRadius = CornerRadius(r, r)
        )

        // Draw 4 surrounding rectangles instead of clipPath(Difference) to avoid
        // iOS Skia/Metal crashes when opening scanner overlays.
        drawRect(
            color = scrimColor,
            topLeft = Offset.Zero,
            size = Size(size.width, top.coerceAtLeast(0f))
        )
        drawRect(
            color = scrimColor,
            topLeft = Offset(0f, top),
            size = Size(left.coerceAtLeast(0f), ch.coerceAtLeast(0f))
        )
        drawRect(
            color = scrimColor,
            topLeft = Offset(right, top),
            size = Size((size.width - right).coerceAtLeast(0f), ch.coerceAtLeast(0f))
        )
        drawRect(
            color = scrimColor,
            topLeft = Offset(0f, bottom),
            size = Size(size.width, (size.height - bottom).coerceAtLeast(0f))
        )

        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(rr.left, rr.top),
            size = Size(rr.width, rr.height),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = 2f)
        )
    }
}

expect fun generateBarcodeImage(
    data: String,
    format: KmpBarcodeFormat = KmpBarcodeFormat.CODE_128,
    width: Int = 1024,
    height: Int = 300,
    margin: Int = 16
): ImageBitmap
