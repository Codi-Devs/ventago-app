package com.teco.ventago.features.pos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.organism.CartOrganism
import com.teco.ventago.design_system.organism.PosListOrganism
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.features.pos.ui.viewmodel.totalItems
import com.teco.ventago.features.pos.ui.viewmodel.FlowMode
import com.teco.ventago.isTablet
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.toDecimalString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.pos_cart
import ventago.composeapp.generated.resources.pos_new_invoice
import ventago.composeapp.generated.resources.pos_new_quote
import ventago.composeapp.generated.resources.pos_update_quote

@Composable
fun PosProductScreenBottomBar(backStackEntry: NavBackStackEntry?, navigate: (PosScreens) -> Unit) {
    val viewModel: PosViewModel = backStackEntry?.let {
        koinViewModel(viewModelStoreOwner = it)
    } ?: koinViewModel()

    val uiState by viewModel.uiState.collectAsState()
    val isQuoteFlow = uiState.flowMode == FlowMode.QUOTE
    val snackbarHostState = remember { SnackbarHostState() }
    var lastShownProductAddedSnackbarToken by remember {
        mutableStateOf(uiState.productAddedSnackbarToken)
    }

    LaunchedEffect(uiState.productAddedSnackbarToken) {
        val token = uiState.productAddedSnackbarToken
        if (token == 0L || token == lastShownProductAddedSnackbarToken) return@LaunchedEffect
        lastShownProductAddedSnackbarToken = token

        val result = snackbarHostState.showSnackbar(
            message = "Producto agregado",
            actionLabel = "Facturar",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            navigate(PosScreens.CartScreen)
        }
    }

    if (isTablet()) {
        val actionLabel = when {
            isQuoteFlow && uiState.quoteId != null -> stringResource(Res.string.pos_update_quote)
            isQuoteFlow -> stringResource(Res.string.pos_new_quote)
            else -> stringResource(Res.string.pos_new_invoice)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            ButtonM(
                onClick = {
                    navigate(if (isQuoteFlow) PosScreens.QuoteSummaryScreen else PosScreens.PaymentScreen)
                },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                enabled = uiState.cart.isNotEmpty(),
            ) {
                Text(
                    "$actionLabel ${
                        formatNumberToMoney(
                            viewModel.legalInvoiceTotal().toDecimalString()
                        )
                    }"
                )
            }
            ProductAddedSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            ButtonM(
                onClick = {
                    navigate(PosScreens.CartScreen)
                },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                enabled = uiState.cart.isNotEmpty(),
            ) {
                Text(
                    "${stringResource(Res.string.pos_cart)} ${uiState.cart.totalItems()} items ${
                        formatNumberToMoney(
                            viewModel.legalInvoiceTotal().toDecimalString()
                        )
                    }"
                )
            }
            ProductAddedSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun ProductAddedSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { snackbarData ->
        Snackbar(
            modifier = Modifier.height(56.dp),
            action = {
                snackbarData.visuals.actionLabel?.let { actionLabel ->
                    TextButton(
                        onClick = { snackbarData.performAction() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(text = actionLabel)
                    }
                }
            }
        ) {
            Text(snackbarData.visuals.message)
        }
    }
}

@Composable
fun PosProductScreen(
    viewModel: PosViewModel,
    navigate: (PosScreens) -> Unit
) {
    if (isTablet()) {
        TabletPosProductScreen(viewModel, navigate)
    } else {
        MobilePosProductScreen(viewModel, navigate)
    }

}

@Composable
private fun MobilePosProductScreen(viewModel: PosViewModel, navigate: (PosScreens) -> Unit) {
    PosListOrganism(viewModel, Modifier, navigate)
}

@Composable
private fun TabletPosProductScreen(viewModel: PosViewModel, navigate: (PosScreens) -> Unit) {
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
        PosListOrganism(viewModel, Modifier.weight(1f), navigate)
        CartOrganism(viewModel, Modifier.weight(1f), navigate)
    }
}
