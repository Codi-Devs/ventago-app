package com.teco.ventago.features.quotes.ui.success

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.features.pos.ui.viewmodel.FlowMode
import com.teco.ventago.navigation.PosScreens
import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.download_pdf
import ventago.composeapp.generated.resources.new_quote
import ventago.composeapp.generated.resources.quote_success

@Composable
fun QuoteSuccessScreen(
    viewModel: PosViewModel,
    navigate: (PosScreens, (NavOptionsBuilder.() -> Unit)?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text(text = stringResource(Res.string.quote_success))

        Spacer(Modifier.height(16.dp))
        ButtonM(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                viewModel.resetForNewSale()
                viewModel.setFlowMode(com.teco.ventago.features.pos.ui.viewmodel.FlowMode.QUOTE, quoteId = null)
                navigate(PosScreens.POSScreen, null)
            }
        ) {
            Text(text = stringResource(Res.string.new_quote))
        }

        Spacer(Modifier.height(8.dp))
        ButtonM(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    viewModel.openQuotePdf(uiState.lastQuoteId)
                }
            }
        ) {
            Text(text = stringResource(Res.string.download_pdf))
        }
    }
}
