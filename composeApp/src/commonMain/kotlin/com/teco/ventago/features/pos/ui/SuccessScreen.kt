package com.teco.ventago.features.pos.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.molecules.DMDivider
import com.teco.ventago.design_system.organism.PosSuccessScreen
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.isTablet
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.DateFormat
import com.teco.ventago.utils.ImageSaverFactory
import com.teco.ventago.utils.KmpBarcodeFormat
import com.teco.ventago.utils.doubleTryParse
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.generateBarcodeImage
import com.teco.ventago.utils.getImageRequest
import com.teco.ventago.utils.openFileInGallery
import com.teco.ventago.utils.shareInvoice
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.invoice_i_date
import ventago.composeapp.generated.resources.invoice_i_items
import ventago.composeapp.generated.resources.invoice_i_order
import ventago.composeapp.generated.resources.invoice_i_subtotal
import ventago.composeapp.generated.resources.invoice_i_thanks
import ventago.composeapp.generated.resources.invoice_i_total
import ventago.composeapp.generated.resources.invoice_saved
import ventago.composeapp.generated.resources.pos_discount
import ventago.composeapp.generated.resources.pos_tips
import ventago.composeapp.generated.resources.save
import ventago.composeapp.generated.resources.share

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SuccessScreen(
    viewModel: PosViewModel,
    navController: NavController,
    navigate: (PosScreens, (NavOptionsBuilder.() -> Unit)?) -> Unit) {

    fun goToStart() {
        viewModel.resetForNewSale()
        val popped = navController.popBackStack(
            route = PosScreens.POSScreen.name,
            inclusive = false,
            saveState = false
        )
        if (!popped) {
            // Fallback: ensure we land on POSScreen
            navigate(PosScreens.POSScreen) {
                popUpTo(PosScreens.POS.name) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    BackHandler {
        goToStart()
    }

    if (isTablet()) {
        TabletSuccessScreen(viewModel, newSale = {
            goToStart()
        }, navigate)
    } else {
        PosSuccessScreen(viewModel, newSale = {
            goToStart()
        }, navigate = navigate)
    }
}

@Composable
private fun TabletSuccessScreen(viewModel: PosViewModel, newSale: () -> Unit, navigate: (PosScreens, (NavOptionsBuilder.() -> Unit)?) -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val invoiceSavedString = stringResource(Res.string.invoice_saved)

    Row(
        modifier = Modifier
            .fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PosSuccessScreen(
            viewModel = viewModel,
            modifier = Modifier
                .weight(1f),
            newSale = newSale,
            navigate = navigate,
            showSeeInvoice = false
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 0.dp)
                .width(350.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
//            InvoiceContent(
//                modifier = Modifier
//                    .drawWithCache {
//                        val width = this.size.width.toInt()
//                        val height = this.size.height.toInt()
//                        onDrawWithContent {
//                            val bitmap = ImageBitmap(width, height)
//                            val pictureCanvas = Canvas(bitmap)
//
//                            pictureCanvas.drawRect(
//                                0f, 0f, width.toFloat(), height.toFloat(),
//                                Paint().apply { color = Color.White } // Fondo blanco
//                            )
//
//                            draw(this, this.layoutDirection, pictureCanvas, this.size) {
//                                this@onDrawWithContent.drawContent()
//                            }
//
//                            imageBitmap = bitmap
//
//                            drawIntoCanvas { it.drawImage(
//                                bitmap, topLeftOffset = Offset.Zero,
//                                paint = Paint().apply { color = Color.White } // Fondo blanco
//                            ) }
//                        }
//                    },
//                order = viewModel.getOrder(),
//                business = viewModel.business!!
//            )

            ButtonM(
                modifier = Modifier.padding(all = 16.dp),
                onClick = {
                    val image = imageBitmap
                    var path: String? = null
                    if (image != null) {
                        val imageSaver = ImageSaverFactory.create()
//                        path = imageSaver.saveImage(image, "order_${viewModel.getOrder().orderNumber}.png")
                    }

                    coroutineScope.launch {
                        path?.let {
                            val snackResult = snackbarHostState.showSnackbar(
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

            OutlinedButtonM(modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp), onClick = {
                val image = imageBitmap
                var path: String? = null
                if (image != null) {
                    val imageSaver = ImageSaverFactory.create()
//                    path = imageSaver.saveImage(image, "order_${viewModel.getOrder().orderNumber}.png")
                }
                path?.let {
                    shareInvoice(it)
                }
            }) {
                Text(stringResource(Res.string.share))
            }


        }
    }
}

@Composable
fun InvoiceContent(modifier: Modifier = Modifier, order: Order, business: Business) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (business.logo.isNotBlank() && business.logo != "null") {
            AsyncImage(
                model = getImageRequest(LocalPlatformContext.current, business.logo),
                contentDescription = "Image Styles preview",
                placeholder = ColorPainter(Color.LightGray),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .height(60.dp)
                    .width(60.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp), horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = business.name,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 24.sp)
        }
        // TODO add view to modify invoice and add this info
//        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
//            Text(text = "RUC 155166-2-2024 DV 1", fontWeight = FontWeight.Normal, fontSize = 14.sp)
//        }
        if (business.address.placeAddress != null && business.address.placeAddress != "null") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    text = business.address.placeAddress,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))



        Row(
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.invoice_i_order),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(text = " ${order.internalNumber}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.invoice_i_date),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(text = DateFormat.getOrdersFormattedDate(order.createdAt), fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

//        order.customer?.let {
//            DMDivider(
//                label = stringResource(Res.string.invoice_i_client),
//                dividerColor = Color.Gray
//            )
//
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(top = 8.dp, bottom = 8.dp),
//                horizontalAlignment = Alignment.Start
//            ) {
//                Text(
//                    text = "${stringResource(Res.string.invoice_i_name)} ${it.name}",
//                    fontSize = 16.sp
//                )
//                if (it.email.isNotBlank()) {
//                    Text(
//                        text = "${stringResource(Res.string.invoice_i_email)} ${it.email}",
//                        fontSize = 16.sp
//                    )
//                }
//
//                if (it.phone.isNotBlank()) {
//                    Text(
//                        text = "${stringResource(Res.string.invoice_i_phone)} ${it.phone}",
//                        fontSize = 16.sp
//                    )
//                }
//            }
//
//        }

        DMDivider(
            label = stringResource(Res.string.invoice_i_items),
            dividerColor = Color.Gray
        )


        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            order.lines.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = item.itemName, fontSize = 16.sp)
                    Text(text = "${item.quantity} x ${formatNumberToMoney(item.baseUnitPrice)}", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(thickness = 1.dp, color = Color.Gray)

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${stringResource(Res.string.invoice_i_subtotal)} ${formatNumberToMoney(order.subtotal)}",
                fontSize = 16.sp
            )
        }


        Spacer(modifier = Modifier.height(4.dp))

        Row {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Impuestos: ${formatNumberToMoney(order.taxTotal)}",
                fontSize = 16.sp
            )
        }


        if (order.discountTotal.doubleTryParse() > 0.00) {
            Spacer(modifier = Modifier.height(4.dp))

            Row {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${stringResource(Res.string.pos_discount)}: ${formatNumberToMoney(order.discountTotal)}",
                    fontSize = 16.sp
                )
            }
        }

        if (order.tipsTotal.doubleTryParse() > 0.00) {
            Spacer(modifier = Modifier.height(4.dp))

            Row {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${stringResource(Res.string.pos_tips)}: ${formatNumberToMoney(order.tipsTotal)}",
                    fontSize = 16.sp
                )
            }
        }




        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${stringResource(Res.string.invoice_i_total)} ${formatNumberToMoney(order.totalAmount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(thickness = 1.dp, color = Color.Gray)

        Spacer(modifier = Modifier.height(8.dp))

        Barcode(
            data = order.internalNumber,
            format = KmpBarcodeFormat.CODE_128,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(
                text = stringResource(Res.string.invoice_i_thanks),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

//        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
//            Text(
//                text = "${stringResource(Res.string.invoice_i_visit_us)} ${business.domain}.menucodi.com",
//                fontWeight = FontWeight.Normal,
//                fontSize = 14.sp
//            )
//        }
    }
}

@Composable
fun Barcode(
    data: String,
    modifier: Modifier = Modifier,
    format: KmpBarcodeFormat = KmpBarcodeFormat.CODE_128,
    widthPx: Int = 1024,
    heightPx: Int = 300
) {
    // Cache per input string
    val bitmap = remember(data, format, widthPx, heightPx) {
        generateBarcodeImage(
            data = data,
            format = format,
            width = widthPx,
            height = heightPx
        )
    }
    Image(
        bitmap = bitmap,
        contentDescription = "Invoice barcode",
        modifier = modifier,
        contentScale = ContentScale.FillWidth
    )
}