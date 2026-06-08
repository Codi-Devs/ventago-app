package com.teco.ventago.features.orders.ui.order_history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.molecules.orders.OrderHistoryList
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.features.orders.domain.models.OrderStatus
import com.teco.ventago.features.orders.ui.order_details.viewModel.OrdersDetailsViewModel
import com.teco.ventago.features.orders.ui.order_history.viewModel.OrderHistoryViewModel
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.accept
import ventago.composeapp.generated.resources.cancel
import ventago.composeapp.generated.resources.cancellation_reason
import ventago.composeapp.generated.resources.complete
import ventago.composeapp.generated.resources.order_ready
import ventago.composeapp.generated.resources.orders_generic_error
import ventago.composeapp.generated.resources.reject
import ventago.composeapp.generated.resources.reject_order
import ventago.composeapp.generated.resources.reject_reason
import ventago.composeapp.generated.resources.start_delivery
import ventago.composeapp.generated.resources.start_processing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    viewModel: OrderHistoryViewModel,
    navigateBack: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsState()
    val order = uiState.order!!
    val snackbarHostState = remember { SnackbarHostState() }

    val genericErrorMsg = stringResource(Res.string.orders_generic_error)
    LaunchedEffect(uiState.changingOrderError) {
        if (uiState.changingOrderError) {
            snackbarHostState.showSnackbar(genericErrorMsg)
        }
    }

    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            OrderHistoryList(order.orderHistories) {
                viewModel.showReasonDialog(true)
            }
        }

        Spacer(modifier = Modifier)
        OrderStatusButton((order.orderHistories).last().statusId, {
//            viewModel.historyPrimaryOnClick()
        }, {
            viewModel.historySecondaryOnClick()
        })

        var rejectReason by remember { mutableStateOf("") }

        if (uiState.showRejectDialog) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.showRejectDialog(false)
                },
                title = {
                    Text(
                        text = stringResource(Res.string.reject_order),
                        style = TextStyle(fontFamily = latoFontFamily())
                    )
                },
                text = {
                    Column {
                        TextField(
                            value = rejectReason,
                            onValueChange = { rejectReason = it }
                        )
                        Text(
                            stringResource(Res.string.cancellation_reason),
                            style = TextStyle(fontFamily = latoFontFamily())
                        )
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.padding(all = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.showRejectDialog(false)
                            },
                            Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .weight(1F)
                        ) {
                            Text(
                                text = stringResource(Res.string.cancel),
                                style = TextStyle(fontFamily = latoFontFamily())
                            )
                        }


                        Button(
                            onClick = {
                                if (rejectReason.isEmpty() || rejectReason.isBlank()) {
                                    viewModel.showRejectDialog(false)
                                } else {
                                    viewModel.showRejectDialog(false)
                                    viewModel.rejectOrder(rejectReason)
                                }
                            },
                            Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .weight(1F)
                        ) {
                            Text(
                                stringResource(Res.string.reject),
                                style = TextStyle(fontFamily = latoFontFamily())
                            )
                        }
                    }
                }
            )
        }

        if (uiState.showReasonDialog) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.showReasonDialog(false)
                },
                title = {
                    if (order.status == OrderStatus.REJECT) {
                        Text(
                            text = stringResource(Res.string.reject_reason),
                            style = TextStyle(fontFamily = latoFontFamily())
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.cancellation_reason),
                            style = TextStyle(fontFamily = latoFontFamily())
                        )
                    }
                },
                text = {
                    if (order.status == OrderStatus.REJECT) {
                        Column {
//                            Text(order.rejectReason)
                        }
                    } else {
                        Column {
//                            Text(order.cancelReason)
                        }
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.padding(all = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {

                        Button(
                            onClick = {
                                viewModel.showReasonDialog(false)
                            },
                            Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .weight(1F)
                        ) {
                            Text(
                                text = stringResource(Res.string.accept),
                                style = TextStyle(fontFamily = latoFontFamily())
                            )
                        }
                    }
                }
            )
        }

        if (uiState.loadingBottomSheet.isLoading()) {
            LoadingSheet(
                state = uiState.loadingBottomSheet,
                sheetState = loadingSheetState
            ) {
                viewModel.hideLoading()
            }
        }
    }
}

@Composable
fun OrderStatusButton(status: Int, primaryOnClick: () -> Unit, secondaryOnClick: () -> Unit) {
    when (status) {
        OrderStatus.DRAFT -> {
            OrderHistoryTwoButton(
                primaryOnClick, getPrimaryButtonString(status),
                secondaryOnClick, getSecondaryButtonString(status)
            )
        }

        OrderStatus.CANCELLED, OrderStatus.COMPLETED, OrderStatus.REJECT -> {
            Spacer(modifier = Modifier)
        }

        else -> {
            OrderHistoryTwoButton(
                primaryOnClick, getPrimaryButtonString(status),
                secondaryOnClick, getSecondaryButtonString(status)
            )
        }
    }
}

@Composable
fun OrderHistoryTwoButton(
    primaryOnClick: () -> Unit,
    primaryText: String,
    secondaryOnClick: () -> Unit,
    secondaryText: String,
) {
    Row {
        ButtonM(
            modifier = Modifier.weight(1f),
            onClick = secondaryOnClick,
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ) {
            Text(text = secondaryText, style = TextStyle(fontFamily = latoFontFamily()))
        }

        Spacer(modifier = Modifier.width(8.dp))

        ButtonM(
            modifier = Modifier.weight(1f),
            onClick = primaryOnClick
        ) {
            Text(text = primaryText, style = TextStyle(fontFamily = latoFontFamily()))
        }
    }
}


@Composable
fun OrderHistoryOneButton(primaryOnClick: () -> Unit, primaryText: String) {
    ButtonM(
        onClick = primaryOnClick,
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Text(text = primaryText, style = TextStyle(fontFamily = latoFontFamily()))

    }
}

@Composable
private fun getPrimaryButtonString(status: Int): String {
    return when (status) {
        OrderStatus.DRAFT -> {
            stringResource(Res.string.accept)
        }

        OrderStatus.CANCELLED -> {
            stringResource(Res.string.start_processing)
        }

        OrderStatus.PROCESSING -> {
            stringResource(Res.string.order_ready)
        }

        OrderStatus.READY -> {
            stringResource(Res.string.start_delivery)
        }

        OrderStatus.COMPLETED, OrderStatus.CANCELLED -> {
            stringResource(Res.string.complete)
        }

        else -> {
            ""
        }
    }
}

@Composable
private fun getSecondaryButtonString(status: Int): String {
    return when (status) {
        OrderStatus.DRAFT -> {
            stringResource(Res.string.reject)
        }

        else -> {
            stringResource(Res.string.cancel)
        }
    }
}

