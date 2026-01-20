package com.teco.ventago.features.pos.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.organism.CartOrganism
import com.teco.ventago.features.pos.ui.viewmodel.CartCalc
import com.teco.ventago.features.pos.ui.viewmodel.FlowMode
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.toDecimalString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.pos_new_invoice
import ventago.composeapp.generated.resources.pos_new_quote
import ventago.composeapp.generated.resources.pos_update_quote

@Composable
fun CartScreen(viewModel: PosViewModel, navigate: (PosScreens) -> Unit) {
    CartOrganism(viewModel, Modifier, navigate)
}


@Composable
fun CartScreenBottomBar(backStackEntry: NavBackStackEntry?, navigate: (PosScreens) -> Unit,) {
    val viewModel: PosViewModel = backStackEntry?.let {
        koinViewModel(viewModelStoreOwner = it)
    } ?: koinViewModel()

    val uiState by viewModel.uiState.collectAsState()
    val isQuoteFlow = uiState.flowMode == FlowMode.QUOTE
    
    // Calculate total amount directly from collected state to ensure recomposition
    val totalAmount = CartCalc.summarize(uiState).grandTotal
    val actionLabel = when {
        isQuoteFlow && uiState.quoteId != null -> stringResource(Res.string.pos_update_quote)
        isQuoteFlow -> stringResource(Res.string.pos_new_quote)
        else -> stringResource(Res.string.pos_new_invoice)
    }

    ButtonM(
        onClick = {
            if (uiState.selectedDocType == "03" || uiState.selectedDocType == "10") { // Exportation
                if (!viewModel.hasExportationData()) {
                    // Missing exportation data → open modal, expand export section
                    viewModel.openAdditionalSheetExpandExport()
                    return@ButtonM
                }
            }
            navigate(if (isQuoteFlow) PosScreens.QuoteSummaryScreen else PosScreens.PaymentScreen)
        },
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Text("$actionLabel ${formatNumberToMoney(totalAmount.toDecimalString())}")
    }
}
