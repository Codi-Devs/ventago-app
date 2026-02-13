package com.teco.ventago.design_system.organism

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Money
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FindReplace
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavOptionsBuilder
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.buttons.TextButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.buttons.dashedBorder
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.molecules.DMSimpleAlertDialog
import com.teco.ventago.design_system.molecules.InverseTicketDivider
import com.teco.ventago.design_system.molecules.ListRowCard
import com.teco.ventago.design_system.molecules.PosItemRow
import com.teco.ventago.design_system.molecules.customer.PosCustomerSelection
import com.teco.ventago.design_system.molecules.pos.GlobalInvoiceSheet
import com.teco.ventago.design_system.molecules.pos.ModifyCartItemSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.design_system.theme.WarningAmber
import com.teco.ventago.design_system.theme.Gray70
import com.teco.ventago.design_system.theme.bodyLargeBold
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.headlineMediumBold
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.design_system.theme.titleLarge
import com.teco.ventago.design_system.theme.titleMedium
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import com.teco.ventago.features.pos.domain.models.CartLine
import com.teco.ventago.features.pos.domain.models.Tax
import com.teco.ventago.features.pos.ui.viewmodel.PosState
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.generateQR
import com.teco.ventago.utils.openWhatsappMessage
import com.teco.ventago.utils.shareLink
import com.teco.ventago.utils.toDecimalString
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.add_products
import ventago.composeapp.generated.resources.add_products_desc
import ventago.composeapp.generated.resources.empty_pos_clients
import ventago.composeapp.generated.resources.fi_rs_comment_user
import ventago.composeapp.generated.resources.invoice_i_subtotal
import ventago.composeapp.generated.resources.pos_change
import ventago.composeapp.generated.resources.pos_discount
import ventago.composeapp.generated.resources.pos_new_sale
import ventago.composeapp.generated.resources.pos_see_orders
import ventago.composeapp.generated.resources.pos_see_ticket
import ventago.composeapp.generated.resources.pos_tip
import ventago.composeapp.generated.resources.qr_not_available
import ventago.composeapp.generated.resources.ready
import ventago.composeapp.generated.resources.search
import ventago.composeapp.generated.resources.share_payment_link
import ventago.composeapp.generated.resources.taxes_title
import ventago.composeapp.generated.resources.total

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartOrganism(
    viewModel: PosViewModel, modifier: Modifier = Modifier, navigate: (PosScreens) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val uiState by viewModel.uiState.collectAsState()

    var itemToModify by remember { mutableStateOf<CartLine?>(null) }
    var showModifyItemDialog by remember { mutableStateOf(false) }

    var showGlobalPricingDialog by remember { mutableStateOf(false) }

    var showNotInvoiceCustomerDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(
            color = MaterialTheme.colorScheme.background
        )
    ) {
        // Calculate max height for LazyColumn: available height minus button space
        // Button: ~56dp height + 32dp vertical padding = ~88dp total
        val buttonSpace = 88.dp
        val topSpacer = 16.dp
        val maxLazyColumnHeight = maxHeight - buttonSpace - topSpacer

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxLazyColumnHeight)
                    .padding(horizontal = 16.dp)
                    .background(vanishedBackgroundColor(), RoundedCornerShape(10.dp)),
                state = lazyListState
            ) {
                // Customer Section
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item {
                    uiState.customer?.let { customer ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier, text = "Cliente", style = bodyMediumBold()
                            )

                            if (uiState.invoicingEnabled && customer.invoiceCustomer <= 0) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.size(24.dp).padding(start = 8.dp)
                                        .clickable(onClick = {
                                            showNotInvoiceCustomerDialog = true
                                        })
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f, fill = true))

                            IconButton(
                                onClick = {
                                    navigate(PosScreens.SearchCustomerScreen)
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FindReplace,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.selectCustomer(null)
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }


                        }

                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(bottom = 8.dp, start = 24.dp, end = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                modifier = Modifier,
                                text = "Nombre: ${customer.name}",
                                style = bodyMedium()
                            )
                        }

                        if (customer.ruc != null && customer.ruc.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(bottom = 8.dp, start = 24.dp, end = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    modifier = Modifier,
                                    text = "Ruc: ${customer.ruc}",
                                    style = bodyMedium()
                                )
                            }
                        }

                        if (customer.email != null && customer.email.isNotEmpty() && !customer.email.contains(
                                "pos.com"
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(bottom = 8.dp, start = 24.dp, end = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    modifier = Modifier,
                                    text = "Correo: ${customer.email}",
                                    style = bodyMedium()
                                )
                            }
                        }
                    } ?: run {
                        Column(
                            modifier = Modifier.fillMaxWidth().clickable(onClick = {
                                navigate(PosScreens.SearchCustomerScreen)
                            }),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            TextButtonS(
                                label = "Consumidor Final",
                                onClick = {
                                    navigate(PosScreens.SearchCustomerScreen)
                                },
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(bottom = 0.dp)
                            )
                        }
                    }
                }

                // Items Section
                item { InverseTicketDivider() }
                items(uiState.cart, key = { it.lineId }) { cartItem ->
                    Row(
                        modifier = Modifier.padding(
                            start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp
                        ).clickable(onClick = {
                            itemToModify = cartItem
                            showModifyItemDialog = true
                        })
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = cartItem.name,
                                style = bodyMediumBold(),
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row {
                                Text(
                                    text = formatNumberToMoney(
                                        (cartItem.overrideUnitPrice
                                            ?: cartItem.baseUnitPrice).toDecimalString()
                                    ),
                                    style = bodyMedium(
                                        color = if (cartItem.discountAmount() > 0L) MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.5f
                                        ) else null
                                    ),
                                    textDecoration = if (cartItem.discountAmount() > 0L) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                                )
                                if (cartItem.discountAmount() > 0L) {
                                    Text(
                                        text = formatNumberToMoney(
                                            ((cartItem.overrideUnitPrice
                                                ?: cartItem.baseUnitPrice) - cartItem.discountAmount()).toDecimalString()
                                        ),
                                        style = bodyMedium(),
                                    )
                                }
                            }

                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                modifier = Modifier.clickable {
                                    viewModel.setLineQty(cartItem.lineId, cartItem.quantity + 1)
                                },
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "",
                                tint = MaterialTheme.colorScheme.secondary
                            )

                            Text(
                                text = cartItem.quantity.toString(),
                                style = bodyLargeBold(),
                                modifier = Modifier.padding(start = 12.dp, end = 12.dp)
                            )

                            Icon(
                                modifier = Modifier.clickable {
                                    if (cartItem.quantity <= 1) {
                                        viewModel.removeLine(cartItem.lineId)
                                    } else {
                                        viewModel.setLineQty(
                                            cartItem.lineId, cartItem.quantity - 1
                                        )
                                    }
                                },
                                imageVector = Icons.Outlined.Remove,
                                contentDescription = "",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }


                    }
                }

                // Totals Section
                item { InverseTicketDivider() }
                // 1. Subtotal
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(bottom = 8.dp, start = 32.dp, end = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier,
                            text = stringResource(Res.string.invoice_i_subtotal),
                            style = bodyMedium()
                        )
                        Spacer(modifier = Modifier.weight(1f, fill = true))
                        Text(
                            text = formatNumberToMoney(
                                viewModel.getSubtotalAmount().toDecimalString()
                            ),
                            style = bodyMediumBold(),
                        )
                    }
                }

                // 2. Discounts
                val totalDiscounts = viewModel.getDiscountTotal()
                if (totalDiscounts > 0L) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(bottom = 8.dp, start = 32.dp, end = 32.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween

                        ) {
                            Text(
                                modifier = Modifier,
                                text = stringResource(Res.string.pos_discount),
                                style = bodyMedium()
                            )
                            Spacer(modifier = Modifier.weight(1f, fill = true))
                            Text(
                                text = "-${formatNumberToMoney(totalDiscounts.toDecimalString())}",
                                style = bodyMedium(color = MaterialTheme.colorScheme.error),
                            )
                        }
                    }
                }

                // 3. ITBMS Taxes
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(bottom = 8.dp, start = 32.dp, end = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier,
                            text = stringResource(Res.string.taxes_title),
                            style = bodyMedium()
                        )
                        Spacer(modifier = Modifier.weight(1f, fill = true))
                        Text(
                            text = formatNumberToMoney(
                                viewModel.getITBMS().toDecimalString()
                            ),
                            style = bodyMedium(),
                        )
                    }
                }

                // 4. ISC Taxes
                if (viewModel.getISC() > 0L) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(bottom = 8.dp, start = 32.dp, end = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                modifier = Modifier,
                                text = "ISC",
                                style = bodyMedium()
                            )
                            Spacer(modifier = Modifier.weight(1f, fill = true))
                            Text(
                                text = formatNumberToMoney(
                                    viewModel.getISC().toDecimalString()
                                ),
                                style = bodyMedium(),
                            )
                        }
                    }
                }


                // 5. OTI Taxes
                if (viewModel.getOTI() > 0L) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(bottom = 8.dp, start = 32.dp, end = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                modifier = Modifier,
                                text = "OTI",
                                style = bodyMedium()
                            )
                            Spacer(modifier = Modifier.weight(1f, fill = true))
                            Text(
                                text = formatNumberToMoney(
                                    viewModel.getOTI().toDecimalString()
                                ),
                                style = bodyMedium(),
                            )
                        }
                    }
                }


                // 6. Acarreos/Shipping
                val itemAcarreos = viewModel.getItemsShippingTotal()
                val globalAcarreo = uiState.globalShippingCents ?: 0L
                val acarreosEffective = if (itemAcarreos > 0L) itemAcarreos else globalAcarreo
                if (acarreosEffective > 0L) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(bottom = 8.dp, start = 32.dp, end = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Acarreo", style = bodyMedium())
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = formatNumberToMoney(acarreosEffective.toDecimalString()),
                                style = bodyMedium()
                            )
                        }
                    }
                }

                // 7. Insurance
                val itemIns = viewModel.getItemsInsuranceTotal()
                val globalIns = uiState.globalInsuranceCents ?: 0L
                val insuranceEffective = if (itemIns > 0L) itemIns else globalIns
                if (insuranceEffective > 0L) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(bottom = 8.dp, start = 32.dp, end = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Seguro", style = bodyMedium())
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = formatNumberToMoney(insuranceEffective.toDecimalString()),
                                style = bodyMedium()
                            )
                        }
                    }
                }

                // 8. Other Charges
                uiState.globalOtherChargesCents?.let { otherCharges ->
                    if (otherCharges > 0L) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(bottom = 8.dp, start = 32.dp, end = 32.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    modifier = Modifier,
                                    text = "Otros cargos",
                                    style = bodyMedium()
                                )
                                Spacer(modifier = Modifier.weight(1f, fill = true))
                                Text(
                                    text = formatNumberToMoney(otherCharges.toDecimalString()),
                                    style = bodyMedium(),
                                )
                            }
                        }
                    }
                }

                // 9. Grand Total
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(bottom = 0.dp, start = 32.dp, end = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier,
                            text = stringResource(Res.string.total),
                            style = bodyMediumBold()
                        )
                        Spacer(modifier = Modifier.weight(1f, fill = true))
                        Text(
                            text = formatNumberToMoney(
                                viewModel.getTotalAmount().toDecimalString()
                            ),
                            style = bodyMediumBold(),
                        )
                    }
                }


                // Tax Exempt Checkbox
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(bottom = 8.dp, start = 24.dp, end = 32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.taxExempt,
                            onCheckedChange = { viewModel.toggleTaxExempt(it) }
                        )
                        Text(
                            text = "Orden exenta de impuestos",
                            style = bodyMedium(),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // 10. Modify Global Pricing Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(bottom = 16.dp, start = 32.dp, end = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(modifier = Modifier.weight(1f, fill = true))
                        TextButtonS(
                            label = "Otros ajustes de precios",
                            onClick = {
                                showGlobalPricingDialog = true
                            },
                            prefixIcon = rememberVectorPainter(Icons.Outlined.Money),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier,
                        )
                    }
                }
            }

            // Spacer to push button to bottom when LazyColumn content is short
            Spacer(modifier = Modifier.weight(1f, fill = true))

            OutlinedButtonM(
                onClick = { viewModel.openAdditionalSheet() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentColor = MaterialTheme.colorScheme.secondary,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
            ) {
                Text("Información adicional")
            }
        }

        if (showNotInvoiceCustomerDialog) {
            DMAlertDialog(
                "Cliente incompleto",
                "El cliente seleccionado no está habilitado para facturar. Por favor, complete su información fiscal o seleccione otro cliente.",
                show = showNotInvoiceCustomerDialog,
                onDismiss = { showNotInvoiceCustomerDialog = false },
                onConfirm = { showNotInvoiceCustomerDialog = false },
                confirmText = "Completar información",
                dismissText = "Cancelar",
            )
        }
    }

    if (uiState.showAdditionalSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { viewModel.closeAdditionalSheet() },
            containerColor = MaterialTheme.colorScheme.background,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            AdditionalInfoSheet(
                uiState = uiState,
                viewModel = viewModel,
                onClose = { viewModel.closeAdditionalSheet() }
            )
        }
    }

    if (showGlobalPricingDialog) {
        GlobalInvoiceSheet(
            currencySymbol = uiState.currency,
            subtotalCents = viewModel.getSubtotalAmount(),
            anyItemHasShipping = viewModel.getItemsShippingTotal() > 0L,
            anyItemHasInsurance = viewModel.getItemsInsuranceTotal() > 0L,
            initialMode = uiState.globalDiscountMode,
            initialPercent = uiState.globalDiscountPercent,
            initialFixedCents = uiState.globalDiscountFixedCents,
            initialGlobalShippingCents = uiState.globalShippingCents,
            initialGlobalInsuranceCents = uiState.globalInsuranceCents,
            initialGlobalOtherCents = uiState.globalOtherChargesCents,
            onDismiss = {
                showGlobalPricingDialog = false
            },
            onApply = { mode, discountValue, shippingCents, insuranceCents, otherCents ->
                viewModel.applyGlobalSettings(
                    mode = mode,
                    discountValue = discountValue,
                    shippingCents = shippingCents,
                    insuranceCents = insuranceCents,
                    otherCents = otherCents,
                )
            })
    }

    if (showModifyItemDialog && itemToModify != null) {
        ModifyCartItemSheet(
            itemToModify = itemToModify!!,
            currencySymbol = uiState.currency,
            onDismiss = {
                itemToModify = null
                showModifyItemDialog = false
            },
            onApply = { newUnitPriceCents, qty, mode, discountValue, itemShippingCents, itemInsuranceCents, pharmaBatchNumber, pharmaBatchQty ->
                viewModel.updateCartLine(
                    lineId = itemToModify!!.lineId,
                    unitPriceCents = newUnitPriceCents,
                    quantity = qty,
                    discountMode = mode,          // map to your domain type if different
                    discountValue = discountValue, // percent or cents depending on mode
                    itemShippingCents = itemShippingCents,
                    itemInsuranceCents = itemInsuranceCents,
                    pharmaBatchNumber = pharmaBatchNumber,
                    pharmaBatchQty = pharmaBatchQty
                )
            },
            invoiceHasGlobalShipping = uiState.globalShippingCents != null && uiState.globalShippingCents!! > 0L,
            invoiceHasGlobalInsurance = uiState.globalInsuranceCents != null && uiState.globalInsuranceCents!! > 0L,
        )
    }
}


@Composable
fun PosListOrganism(
    viewModel: PosViewModel, modifier: Modifier = Modifier, navigate: (PosScreens) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()

    Column(
        modifier = modifier.padding(horizontal = 0.dp).fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        if (!uiState.invoicingEnabled) {
            PosCustomerSelection(
                customer = uiState.customer
            ) { screen -> navigate(screen) }
        }


        DMOutlinedTextField(
            text = uiState.query,
            label = stringResource(Res.string.search),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp, top = 8.dp),
            onChange = {
                viewModel.onSearchChange(it)
            },
            leadingIcon = Icons.Rounded.Search,
            maxLines = 1,
            imeAction = ImeAction.Done,
        )

        if (uiState.items.isEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().background(
                    color = vanishedBackgroundColor(), shape = RoundedCornerShape(12.dp)
                ).padding(top = 8.dp, bottom = 8.dp),
                state = lazyListState,
            ) {
                item(key = "new_product") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(
                                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp
                            )
                            .dashedBorder(
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.secondary,
                                cornerRadiusDp = 10.dp
                            ),
                        enabled = true,
                        elevation = CardDefaults.elevatedCardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                        shape = RoundedCornerShape(10.dp),
                        onClick = {
                            navigate(PosScreens.AddItemScreen)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(end = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(color = Gray70)
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Producto Personalizado",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().background(
                    color = vanishedBackgroundColor(), shape = RoundedCornerShape(12.dp)
                ).padding(top = 8.dp, bottom = 8.dp),
                state = lazyListState,
            ) {
                // Add "New Product" card at the beginning
                item(key = "new_product") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp) // Make it taller
                            .padding(
                                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp
                            )
                            .dashedBorder(
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.secondary,
                                cornerRadiusDp = 10.dp
                            ),
                        enabled = true,
                        elevation = CardDefaults.elevatedCardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                        shape = RoundedCornerShape(10.dp),
                        onClick = {
                            navigate(PosScreens.AddItemScreen)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // LEFT - Icon
                            Column(
                                modifier = Modifier
                                    .padding(end = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Divider - Full height
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(color = Gray70)
                            )

                            // CENTER - Text content
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Producto Personalizado",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary)
                                )
                            }
                        }
                    }
                }

                items(uiState.items, key = { it.itemId }) { item ->
                    PosItemRow(
                        item = item,
                        currency = uiState.currency,
                        modifier = Modifier.padding(
                            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp
                        ),
                        onClick = {
                            viewModel.addItemToCart(
                                item, tax = item.taxPercent?.let { tax ->
                                    Tax(
                                        id = tax, name = "$tax", rateBps = tax * 100
                                    )
                                })
                        },
                    )

                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun PosSuccessScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier,
    newSale: () -> Unit,
    navigate: (PosScreens, (NavOptionsBuilder.() -> Unit)?) -> Unit,
    showSeeInvoice: Boolean = true
) {

    val uiState by viewModel.uiState.collectAsState()

    // Determine screen state from viewModel state
    val orderFailed = uiState.orderCreationFailed
    // Invoice failed if: order succeeded, order number exists, and invoice status is FAILED
    val invoiceFailed = !orderFailed &&
            uiState.orderNumber.isNotEmpty() &&
            uiState.invoiceStatus == InvoiceStatus.FAILED

    // State to control invoice failure alert dialog
    // Track which order number we've already shown the alert for to avoid showing it multiple times
    var shownAlertForOrderNumber by rememberSaveable { mutableStateOf<String?>(null) }

    // State to control when to actually display the dialog
    var showInvoiceFailureAlert by remember { mutableStateOf(false) }

    // Trigger showing the alert dialog when invoice failure is detected for a new order
    LaunchedEffect(uiState.orderNumber, uiState.invoiceStatus, invoiceFailed) {
        val shouldShow = invoiceFailed &&
                uiState.orderNumber.isNotEmpty() &&
                shownAlertForOrderNumber != uiState.orderNumber

        if (shouldShow) {
            // Wait a moment for the screen to render before showing the dialog
            delay(500)

            // Double-check conditions after delay (in case state changed)
            // Re-read from uiState to get latest values
            val currentOrderNumber = uiState.orderNumber
            val currentInvoiceStatus = uiState.invoiceStatus
            val currentOrderFailed = uiState.orderCreationFailed

            val stillFailed = !currentOrderFailed &&
                    currentOrderNumber.isNotEmpty() &&
                    currentInvoiceStatus == InvoiceStatus.FAILED &&
                    shownAlertForOrderNumber != currentOrderNumber

            if (stillFailed) {
                showInvoiceFailureAlert = true
            }
        } else {
            // Hide dialog if conditions no longer apply
            showInvoiceFailureAlert = false
        }
    }

    // Use failure animation if order failed, success animation otherwise
    val animationFile = if (orderFailed) {
        "files/wrong.json"
    } else {
        "files/57767-done.json"
    }

    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes(animationFile).decodeToString()
        )
    }
    val progress by animateLottieCompositionAsState(composition, iterations = 1)

    val analytics = koinInject<AnalyticsService>()

    Column(
        modifier = modifier.padding(horizontal = 0.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f, fill = true))

        if (orderFailed) {
            // Order creation failed - show failure UI
            Image(
                painter = rememberLottiePainter(
                    composition = composition,
                    progress = { progress },
                ),
                modifier = Modifier.size(height = 150.dp, width = 150.dp),
                contentDescription = "Lottie animation"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Error al crear pedido",
                style = titleLarge(),
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "No se pudo crear el pedido. Por favor, intenta nuevamente o contacta al soporte.",
                style = bodyMedium(),
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f, fill = true))

            ButtonM(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                onClick = {
                    openWhatsappMessage(
                        "50763879477",
                        "Hola, necesito ayuda. El pedido no se pudo crear."
                    )
                },
                containerColor = Color(0xFF25D366), // WhatsApp green
            ) {
                Text("Contactar soporte por WhatsApp", color = Color.White)
            }

            ButtonM(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                onClick = {
                    newSale()
                },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Text(stringResource(Res.string.pos_new_sale))
            }
        } else {
            // Normal success flow
            if (uiState.paymentLink.isNotEmpty()) {
                val bitmap = remember(uiState.paymentLink) {
                    generateQR(
                        width = 500, // Pass width in pixels
                        height = 500,
                        url = uiState.paymentLink
                    )
                }
                bitmap.toImageBitmap()?.let {
                    Image(
                        painter = BitmapPainter(it),
                        contentDescription = "QR Code for Table",
                        modifier = Modifier.fillMaxWidth().height(300.dp)
                    )
                } ?: run {
                    Text(stringResource(Res.string.qr_not_available))
                }

                TextButtonM(
                    label = stringResource(Res.string.share_payment_link), icon = Icons.Filled.Share
                ) {
                    analytics.logEvent("share_payment_link", analytics.getAnalyticsBundle().apply {
                        "payment_link" to uiState.paymentLink
                    })
                    shareLink(uiState.paymentLink)
                }
            }

            Image(
                painter = rememberLottiePainter(
                    composition = composition,
                    progress = { progress },
                ),
                modifier = Modifier.size(height = 150.dp, width = 150.dp),
                contentDescription = "Lottie animation"
            )

            Spacer(modifier = Modifier.weight(1f, fill = true))

            Text(stringResource(Res.string.ready), style = titleLarge())
            Text(
                formatNumberToMoney(viewModel.legalInvoiceTotal().toDecimalString()),
                modifier = Modifier.padding(8.dp),
                style = headlineMediumBold(color = Color(0xFF5A6372))
            )

            // Show invoice warning if invoice failed
            if (invoiceFailed) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WarningAmber.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(1.dp, WarningAmber)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Advertencia",
                                style = bodyMediumBold(),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "La factura electrónica no se pudo generar. El pedido fue creado exitosamente, pero deberás generar la factura manualmente.",
                                style = bodyMedium(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            if (viewModel.getChange() > 0) {
                if (viewModel.getTipsTotal() > 0L) {
                    Text(
                        "${stringResource(Res.string.pos_tip)}: ${
                            formatNumberToMoney(
                                viewModel.getTipsTotal().toDecimalString()
                            )
                        }", style = bodyMedium()
                    )
                }
                Text(
                    "${stringResource(Res.string.pos_change)}: ${
                        formatNumberToMoney(
                            viewModel.getChange().toDecimalString()
                        )
                    }", modifier = Modifier.padding(bottom = 42.dp), style = bodyMedium()
                )
            } else if (viewModel.getTipsTotal() > 0L) {
                Text(
                    "${stringResource(Res.string.pos_tip)}: ${
                        formatNumberToMoney(
                            viewModel.tipsTotal().toDecimalString()
                        )
                    }", modifier = Modifier.padding(bottom = 42.dp), style = bodyMedium()
                )
            }


            OutlinedButtonM(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp), onClick = {
                    navigate(PosScreens.Orders) {
                        popUpTo(PosScreens.POS.name) {
                            inclusive = true
                        }
                    }
                }) {
                Text(stringResource(Res.string.pos_see_orders))
            }

            if (uiState.invoiceStatus == InvoiceStatus.ISSUED && uiState.pdfDocument.isNotEmpty()) {
                OutlinedButtonM(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    onClick = {
                        viewModel.openPdfDocument()
                    }) {
                    Text("Ver factura")
                }
            }

            ButtonM(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                onClick = {
                    newSale()
                },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Text(stringResource(Res.string.pos_new_sale))
            }
        }

    }

    // Invoice failure alert dialog - shows automatically when invoice fails
    DMSimpleAlertDialog(
        title = "Advertencia: Factura no generada",
        message = "La factura electrónica no se pudo generar. El pedido fue creado exitosamente (N° ${uiState.orderNumber}), pero deberás generar la factura manualmente desde la sección de pedidos.",
        show = showInvoiceFailureAlert,
        onDismiss = {
            // Hide dialog and mark that we've shown it for this order
            showInvoiceFailureAlert = false
            shownAlertForOrderNumber = uiState.orderNumber
        },
        onConfirm = {
            // Hide dialog and mark that we've shown it for this order
            showInvoiceFailureAlert = false
            shownAlertForOrderNumber = uiState.orderNumber
        },
        btnText = "Entendido"
    )
}

@Composable
fun EmptyClientsContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(top = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(Res.drawable.fi_rs_comment_user),
            contentDescription = "",
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 32.dp)
        )

        Text(
            text = stringResource(Res.string.empty_pos_clients),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
            style = TextStyle(
                fontSize = 16.sp,
                fontFamily = latoFontFamily(),
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
        )
    }


}


@Composable
private fun AdditionalInfoSheet(
    uiState: PosState,
    viewModel: PosViewModel,
    onClose: () -> Unit
) {
    // local expand states (start collapsed)
    var openLogistics by rememberSaveable { mutableStateOf(false) }
    var openCustomerAddresses by rememberSaveable { mutableStateOf(false) }
    var openDelivery by rememberSaveable { mutableStateOf(false) }
    var openRetentions by rememberSaveable { mutableStateOf(false) }
    var openExport by rememberSaveable { mutableStateOf(uiState.expandExportSection) }

    val isExport = uiState.selectedDocType == "03" || uiState.selectedDocType == "10" // Exportación

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Información adicional", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
            }
        }

        // ===== 1) Logistics =====
        CollapsibleCard(
            title = "Logística",
            expanded = openLogistics,
            onToggle = { openLogistics = !openLogistics }
        )
        {
            DMOutlinedTextField(
                text = uiState.logisticsInfo,
                label = "Información (opcional)",
                onChange = viewModel::onLogInfo,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
            DMOutlinedTextField(
                text = uiState.logisticsVehiclePlate,
                label = "Matrícula / Placa del vehículo",
                onChange = viewModel::onLogPlate,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                maxLines = 1
            )
            DMOutlinedTextField(
                text = uiState.logisticsCarrierLegalName,
                label = "Razón social del transportista",
                onChange = viewModel::onLogCarrierName,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                maxLines = 1
            )
            Row(Modifier.fillMaxWidth()) {
                DMOutlinedTextField(
                    text = uiState.logisticsCarrierRuc,
                    label = "RUC transportista",
                    onChange = viewModel::onLogCarrierRuc,
                    modifier = Modifier
                        .weight(0.70f)
                        .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    maxLines = 1
                )
                DMOutlinedTextField(
                    text = uiState.logisticsCarrierDv,
                    label = "DV",
                    onChange = viewModel::onLogCarrierDv,
                    modifier = Modifier
                        .weight(0.30f)
                        .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                    maxLines = 1
                )
            }
            DMDropDownField(
                label = "Tipo de contribuyente",
                items = viewModel.taxpayerTypeOptions(),
                selectedIndex = uiState.logisticsCarrierTaxpayerTypeIndex,
                onItemSelected = { idx, _ -> viewModel.onLogCarrierType(idx) },
                isError = false,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            Row(Modifier.fillMaxWidth()) {
                DMOutlinedTextField(
                    text = uiState.logisticsBoxesQty,
                    label = "Bultos",
                    onChange = viewModel::onLogBoxes,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    maxLines = 1
                )
                DMOutlinedTextField(
                    text = uiState.logisticsTotalWeightLb,
                    label = "Peso total (lb)",
                    onChange = viewModel::onLogWeightLb,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                    maxLines = 1
                )
            }
        }

        // ===== 2) Delivery Location =====
        if (uiState.finalCustomer == false && uiState.customer != null) {
            CollapsibleCard(
                title = "Direcciones del cliente",
                expanded = openCustomerAddresses,
                onToggle = { openCustomerAddresses = !openCustomerAddresses }
            ) {
                Text(
                    text = "Selecciona una dirección adicional para la factura.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                when {
                    uiState.customerAddressesLoading -> {
                        Text(
                            text = "Cargando direcciones...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    uiState.customerAddresses.isEmpty() -> {
                        Text(
                            text = "Este cliente no tiene direcciones disponibles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    else -> {
                        uiState.customerAddresses.forEach { address ->
                            val selected = uiState.selectedCustomerAddressId == address.id
                            val locationText = listOf(
                                address.province,
                                address.district,
                                address.corregimiento
                            ).filterNotNull().filter { it.isNotBlank() }.joinToString(" / ")

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable { viewModel.onCustomerAddressSelected(address.id) },
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) {
                                        vanishedBackgroundColor()
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = address.addressLine,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (address.isDefault) {
                                            Text(
                                                text = "Predeterminada",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                    if (locationText.isNotBlank()) {
                                        Text(
                                            text = locationText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    if (!address.locationCode.isNullOrBlank()) {
                                        Text(
                                            text = "Codigo: ${address.locationCode}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Si deseas agregar o modificar direcciones del cliente, hazlo desde la version web.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // ===== 3) Delivery Location =====
        CollapsibleCard(
            title = "Lugar de entrega",
            expanded = openDelivery,
            onToggle = { openDelivery = !openDelivery }
        )
        {
            DMOutlinedTextField(
                text = uiState.deliveryReceiverLegalName,
                label = "Razón social del receptor",
                onChange = viewModel::onDelName,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                maxLines = 1
            )
            Row(Modifier.fillMaxWidth()) {
                DMOutlinedTextField(
                    text = uiState.deliveryReceiverRuc,
                    label = "RUC receptor",
                    onChange = viewModel::onDelRuc,
                    modifier = Modifier
                        .weight(0.7f)
                        .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    maxLines = 1
                )
                DMOutlinedTextField(
                    text = uiState.deliveryReceiverDv,
                    label = "DV",
                    onChange = viewModel::onDelDv,
                    modifier = Modifier
                        .weight(0.3f)
                        .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                    maxLines = 1
                )
            }
            DMDropDownField(
                label = "Tipo de contribuyente",
                items = viewModel.taxpayerTypeOptions(),
                selectedIndex = uiState.deliveryReceiverTaxpayerTypeIndex,
                onItemSelected = { idx, _ -> viewModel.onDelType(idx) },
                isError = false,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            Row(Modifier.fillMaxWidth()) {
                DMOutlinedTextField(
                    text = uiState.deliveryContactPhone,
                    label = "Teléfono de contacto",
                    onChange = viewModel::onDelPhone,
                    keyboardType = KeyboardType.Phone,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    maxLines = 1
                )
                DMOutlinedTextField(
                    text = uiState.deliveryAltContactPhone,
                    label = "Teléfono alterno",
                    onChange = viewModel::onDelAltPhone,
                    keyboardType = KeyboardType.Phone,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                    maxLines = 1
                )
            }

            // Ubicación: Prov / Distrito / Corregimiento
            DMDropDownField(
                label = "Provincia",
                items = viewModel.provinceOptions(),
                selectedIndex = uiState.deliveryProvinceIndex,
                onItemSelected = { idx, _ -> viewModel.onProvinceSelected(idx) },
                isError = false,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            DMDropDownField(
                label = "Distrito",
                items = viewModel.districtOptions(uiState.deliveryProvinceIndex),
                selectedIndex = uiState.deliveryDistrictIndex,
                onItemSelected = { idx, _ -> viewModel.onDistrictSelected(idx) },
                isError = false,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            DMDropDownField(
                label = "Corregimiento",
                items = viewModel.corregOptions(
                    uiState.deliveryProvinceIndex,
                    uiState.deliveryDistrictIndex
                ),
                selectedIndex = uiState.deliveryCorregIndex,
                onItemSelected = { idx, _ -> viewModel.onCorregSelected(idx) },
                isError = false,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // ===== 4) Retenciones =====
        CollapsibleCard(
            title = "Retenciones",
            expanded = openRetentions,
            onToggle = { openRetentions = !openRetentions }
        )
        {
            DMDropDownField(
                label = "Código de retención",
                items = viewModel.retentionLabels(),
                selectedIndex = uiState.retentionCodeIndex,
                onItemSelected = { idx, _ -> viewModel.onRetentionSelected(idx) },
                isError = false,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (viewModel.selectedRetentionRequiresAmount()) {
                DMOutlinedTextField(
                    text = uiState.retentionAmount,
                    label = "Tasa de retención (%)",
                    onChange = viewModel::onRetentionAmount,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    maxLines = 1,
                    prefix = "%"
                )
            }
        }

        // ===== 5) Exportación =====
        if (isExport) {
            CollapsibleCard(
                title = "Exportación",
                expanded = openExport,
                onToggle = { openExport = !openExport }
            )
            {
                DMOutlinedTextField(
                    text = uiState.exportIncoterm,
                    label = "Incoterm (ej: FOB, CIF, EXW)",
                    onChange = viewModel::onIncoterm,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    maxLines = 1
                )
                DMOutlinedTextField(
                    text = uiState.exportCurrency,
                    label = "Moneda (por defecto PAB)",
                    onChange = viewModel::onExportCurrency,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    maxLines = 1
                )
                DMOutlinedTextField(
                    text = uiState.exportPortOfLoading,
                    label = "Puerto de embarque",
                    onChange = viewModel::onPortOfLoading,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onClose) { Text("Cerrar") }
        }
    }
}

@Composable
private fun CollapsibleCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = vanishedBackgroundColor()
        )
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.fillMaxWidth()) {
                    Divider(Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(8.dp))
                    content()
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
