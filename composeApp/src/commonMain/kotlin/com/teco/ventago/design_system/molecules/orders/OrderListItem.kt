package com.teco.ventago.design_system.molecules.orders

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.ShoppingCartCheckout
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.design_system.theme.AcceptedContainer
import com.teco.ventago.design_system.theme.AcceptedLabel
import com.teco.ventago.design_system.theme.CancelledContainer
import com.teco.ventago.design_system.theme.CancelledLabel
import com.teco.ventago.design_system.theme.CompletedContainer
import com.teco.ventago.design_system.theme.CompletedLabel
import com.teco.ventago.design_system.theme.Gray80
import com.teco.ventago.design_system.theme.InDeliveryContainer
import com.teco.ventago.design_system.theme.InDeliveryLabel
import com.teco.ventago.design_system.theme.NewContainer
import com.teco.ventago.design_system.theme.NewLabel
import com.teco.ventago.design_system.theme.PaymentFailedContainer
import com.teco.ventago.design_system.theme.PaymentFailedLabel
import com.teco.ventago.design_system.theme.ProcessingContainer
import com.teco.ventago.design_system.theme.ProcessingLabel
import com.teco.ventago.design_system.theme.ReadyContainer
import com.teco.ventago.design_system.theme.ReadyForPickupContainer
import com.teco.ventago.design_system.theme.ReadyForPickupLabel
import com.teco.ventago.design_system.theme.ReadyLabel
import com.teco.ventago.design_system.theme.RejectedContainer
import com.teco.ventago.design_system.theme.RejectedLabel
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.labelMedium
import com.teco.ventago.design_system.theme.outlineLight
import com.teco.ventago.features.invoicing.domain.models.FEDocumentType
import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import com.teco.ventago.features.orders.domain.models.OrderStatus
import com.teco.ventago.features.orders.domain.models.PaymentStatus
import com.teco.ventago.utils.formatNumberToMoney
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.accepted
import ventago.composeapp.generated.resources.cancelled
import ventago.composeapp.generated.resources.completed
import ventago.composeapp.generated.resources.filter_new
import ventago.composeapp.generated.resources.filter_processing
import ventago.composeapp.generated.resources.in_delivery
import ventago.composeapp.generated.resources.items
import ventago.composeapp.generated.resources.order_cancelled
import ventago.composeapp.generated.resources.order_completed
import ventago.composeapp.generated.resources.order_shopping_cart
import ventago.composeapp.generated.resources.payment_failed
import ventago.composeapp.generated.resources.ready
import ventago.composeapp.generated.resources.ready_pickup
import ventago.composeapp.generated.resources.rejected

@Composable
fun OrderListItem(order: Order, onClick: () -> Unit, statusOnClick: () -> Unit) {

    val totalItems = order.lines.sumOf { it.quantity }

    val (icon, tint) = getOrderStatusIcon(
        order.status,
        order.paymentStatus,
        order.invoiceStatus ?: 0,
        order.orderType
    )

    Column {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(10),
                color = Gray80,
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = "Order state icon",
                    tint = tint,
                    modifier = Modifier
                        .padding(10.dp, 10.dp)
                        .size(40.dp),
                )
//                Image(
//                    painter = painterResource(getOrderDrawable(order)),
//                    contentDescription = null,
//                    modifier = Modifier
//                        .padding(10.dp, 10.dp)
//                        .size(40.dp),
//                    contentScale = ContentScale.Fit,
//                )
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f, fill = true)
                    .fillMaxHeight()
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    text = "#${order.formattedInternalNumber()}",
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.primary),
                )
                Text(
                    modifier = Modifier.padding(top = 10.dp),
                    text = "$totalItems ${stringResource(Res.string.items)}",
                    textAlign = TextAlign.Center,
                    style = labelMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }

            Spacer(modifier = Modifier)

            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween

            ) {
                OrderStatusChip(order.status)   {
                    statusOnClick()
                }
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = formatNumberToMoney(order.totalAmount),
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }

}

@Composable
fun OrderStatusChip(status: Int, onClick: () -> Unit) {
    val colors  = getOrderStatusColors(status)
    val label = getOrderLabelString(status)
    SuggestionChip(
        modifier = Modifier.height(23.dp),
        onClick = onClick,
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = colors.second,
            labelColor = colors.first,
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = colors.second,
            disabledBorderColor= colors.second,
            borderWidth = 1.dp
        ),
        label = {
            Text(
                text = label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
    )
}

private fun getOrderStatusColors(status: Int): Pair<Color, Color> {
    return when(status) {
        // 1 = Draft
        OrderStatus.DRAFT ->
            Pair(Color(0xFF757575), Color(0xFFF5F5F5))     // Gray text / light gray bg

        // 2 = Confirmed
        OrderStatus.CONFIRMED ->
            Pair(Color(0xFF1976D2), Color(0xFFE3F2FD))     // Blue text / light blue bg

        // 3 = Processing
        OrderStatus.PROCESSING ->
            Pair(Color(0xFFFFA000), Color(0xFFFFF3E0))     // Amber text / light amber bg

        // 4 = Ready
        OrderStatus.READY ->
            Pair(Color(0xFF00897B), Color(0xFFE0F2F1))     // Teal text / light teal bg

        // 5 = Completed
        OrderStatus.COMPLETED ->
            Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))     // Green text / light green bg

        // 6 = Cancelled
        OrderStatus.CANCELLED ->
            Pair(Color(0xFFD32F2F), Color(0xFFFFEBEE))     // Red text / light red bg

        // 7 = Rejected
        OrderStatus.REJECT ->
            Pair(Color(0xFF6A1B9A), Color(0xFFF3E5F5))     // Purple text / light purple bg

        // 8 = Refunded
        OrderStatus.REFUNDED ->
            Pair(Color(0xFF283593), Color(0xFFE8EAF6))     // Indigo text / light indigo bg

        else ->
            Pair(Color(0xFF757575), Color(0xFFF5F5F5))     // Default gray
    }
}


private fun getOrderLabelString(status: Int): String {
    return when(status) {
        OrderStatus.DRAFT -> "Borrador"
        OrderStatus.CONFIRMED -> "Confirmado"
        OrderStatus.PROCESSING -> "En preparación"
        OrderStatus.READY -> "Listo para entregar"
        OrderStatus.COMPLETED -> "Completado"
        OrderStatus.CANCELLED -> "Cancelado"
        OrderStatus.REJECT -> "Rechazado"
        OrderStatus.REFUNDED -> "Reembolsado"
        else -> "Desconocido"
    }
}

private fun getOrderDrawable(order: Order): DrawableResource {
    if (order.status == OrderStatus.CANCELLED) {
        return Res.drawable.order_cancelled
    }



    return Res.drawable.order_shopping_cart
}


data class OrderIconResult(val icon: ImageVector, val tint: Color)

fun getOrderStatusIcon(
    orderStatus: Int,
    paymentStatus: Int,
    invoiceStatus: Int,
    docTypeCode: String
): OrderIconResult {
    // Canceled or refunded first
    if (orderStatus == OrderStatus.CANCELLED || orderStatus == OrderStatus.REJECT || orderStatus == OrderStatus.REFUNDED)
        return OrderIconResult(Icons.Rounded.Cancel, Color.Red)

    // Invoice failed
    if (invoiceStatus == InvoiceStatus.FAILED.id)
        return OrderIconResult(Icons.Rounded.ErrorOutline, Color.Red)

    // Invoice issued → prefer invoice-type icon
    if (invoiceStatus == InvoiceStatus.ISSUED.id) {
        val type = FEDocumentType.fromCode(docTypeCode)
        return when (type) {
            FEDocumentType.CREDIT_NOTE_REFERENCING_FE,
            FEDocumentType.GENERIC_CREDIT_NOTE ->
                OrderIconResult(Icons.Rounded.Undo, Color(0xFF2E7D32)) // green
            FEDocumentType.DEBIT_NOTE_REFERENCING_FE,
            FEDocumentType.GENERIC_DEBIT_NOTE ->
                OrderIconResult(Icons.Rounded.TrendingUp, Color(0xFFFFA000)) // orange
            else -> OrderIconResult(Icons.Rounded.ReceiptLong, Color(0xFF1976D2)) // blue
        }
    }

    // Drafts
    if (orderStatus == OrderStatus.DRAFT)
        return OrderIconResult(Icons.Rounded.Description, Color.Gray)

    // Paid & completed
    if (paymentStatus == PaymentStatus.PAID.id)
        return OrderIconResult(Icons.Rounded.ShoppingCartCheckout, Color(0xFF388E3C))

    // Pending payment
    if (paymentStatus == PaymentStatus.PARTIAL.id)
        return OrderIconResult(Icons.Rounded.Payments, Color(0xFFFFA000))

    // Default
    return OrderIconResult(Icons.Rounded.ShoppingCart, Color.Gray)
}

private fun getOrderDrawable(status: Int): DrawableResource {
    return when(status) {
        OrderStatus.COMPLETED -> Res.drawable.order_completed
        OrderStatus.CANCELLED -> Res.drawable.order_cancelled
        OrderStatus.REJECT -> Res.drawable.order_cancelled
        else -> Res.drawable.order_shopping_cart
    }
}