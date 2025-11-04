package com.teco.ventago.features.orders.ui.order_invoice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import com.teco.ventago.core.SnackbarService
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.features.orders.ui.order_details.viewModel.OrdersDetailsViewModel
import com.teco.ventago.features.orders.ui.order_invoice.viewModel.OrderInvoiceViewModel
import com.teco.ventago.features.pos.ui.InvoiceContent
import com.teco.ventago.utils.ImageSaverFactory
import com.teco.ventago.utils.openFileInGallery
import com.teco.ventago.utils.shareInvoice
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.invoice_saved
import ventago.composeapp.generated.resources.save

@Composable
fun OrderInvoiceActions(backStackEntry: NavBackStackEntry?) {
    val viewModel: OrderInvoiceViewModel = backStackEntry?.let {
        koinViewModel(viewModelStoreOwner = it)
    } ?: koinViewModel()

    val uiState by viewModel.uiState.collectAsState()
    val order = uiState.order!!

    IconButton(onClick = {
        val image = uiState.invoiceBitmap
        var path: String? = null
        if (image != null) {
            val imageSaver = ImageSaverFactory.create()
            path = imageSaver.saveImage(image, "order_${order.internalNumber}.png")
        }
        path?.let {
            shareInvoice(it)
        }

    }) {
        Icon(
            imageVector = Icons.Rounded.Share,
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun OrderInvoiceContent(viewModel: OrderInvoiceViewModel) {
    val snackbarService: SnackbarService = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val invoiceSavedString = stringResource(Res.string.invoice_saved)

    val uiState by viewModel.uiState.collectAsState()
    val order = uiState.order!!

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        InvoiceContent(modifier = Modifier
            .drawWithCache {
                val width = this.size.width.toInt()
                val height = this.size.height.toInt()
                onDrawWithContent {
                    val bitmap = ImageBitmap(width, height)
                    val pictureCanvas = Canvas(bitmap)

                    pictureCanvas.drawRect(
                        0f, 0f, width.toFloat(), height.toFloat(),
                        Paint().apply { color = Color.White } // Fondo blanco
                    )

                    draw(this, this.layoutDirection, pictureCanvas, this.size) {
                        this@onDrawWithContent.drawContent()
                    }

                    viewModel.setInvoiceBitmap(bitmap)

                    drawIntoCanvas { it.drawImage(
                        bitmap, topLeftOffset = Offset.Zero,
                        paint = Paint().apply { color = Color.White } // Fondo blanco
                    ) }
                }
            }
            , order = order, business = viewModel.business!!)
        ButtonM(
            modifier = Modifier.padding(all = 16.dp),
            onClick = {
                val image = uiState.invoiceBitmap
                var path: String? = null
                if (image != null) {
                    val imageSaver = ImageSaverFactory.create()
                    path = imageSaver.saveImage(image, "order_${order.internalNumber}.png")
                }

                coroutineScope.launch {
                    path?.let {
                        val snackResult = snackbarService.showWithAction(
                            message = invoiceSavedString,
                            actionLabel = "Ver"
                        )
                        when (snackResult) {
                            SnackbarResult.Dismissed -> println("SnackbarDemo Dismissed")
                            SnackbarResult.ActionPerformed -> openFileInGallery(it)
                        }
                    }
                }
            }) {
            Text(
                text = stringResource(Res.string.save), style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontFamily = latoFontFamily(),
                    fontWeight = FontWeight.W700,
                    letterSpacing = 0.02.sp,
                )
            )
        }
    }
}