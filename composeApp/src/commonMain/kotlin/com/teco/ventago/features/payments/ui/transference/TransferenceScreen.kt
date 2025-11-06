package com.teco.ventago.features.payments.ui.transference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.molecules.CircularBadge
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.graphicNormalColor
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentMethodsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.active
import ventago.composeapp.generated.resources.commission
import ventago.composeapp.generated.resources.enter_payment_instructions
import ventago.composeapp.generated.resources.payment_instructions
import ventago.composeapp.generated.resources.update


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferenceScreenView(viewModel: PaymentMethodsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })
    val analytics = koinInject<AnalyticsService>()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = stringResource(Res.string.active), modifier = Modifier.padding(end = 8.dp))
            Switch(
                checked = uiState.availablePaymentMethods["transference"]?.enabled ?: false,
                onCheckedChange = { active ->
                    viewModel.activatePaymentMethod("transference", active)
                }
            )

            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.commission), style = titleMediumBold())
                CircularBadge(
                    color = graphicNormalColor,
                    text = "0%",
                )
            }
        }
        DMOutlinedTextField(
            text = uiState.transferenceInstructions,
            label = stringResource(Res.string.payment_instructions),
            modifier = Modifier.padding(horizontal = 16.dp),
            onChange = { value ->
                viewModel.onInstructionsChanged(value)
            },
            maxLines = 8,
            imeAction = ImeAction.Default,
            enabled = true,
            supportingText = stringResource(Res.string.enter_payment_instructions),
            isError = false,
            readOnly = false,
        )

        Spacer(modifier = Modifier.weight(1f))

        ButtonM(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            onClick = {
                if (uiState.availablePaymentMethods["transference"]?.enabled ?: false) {
                    analytics.logEvent("transference_payment_method_enabled")
                }
                viewModel.updateTransferencePaymentMethod()
            },
            enabled = viewModel.canUpdateInstructions(),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Text(text = stringResource(Res.string.update))
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