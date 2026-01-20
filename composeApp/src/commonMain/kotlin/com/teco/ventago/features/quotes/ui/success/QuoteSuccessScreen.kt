package com.teco.ventago.features.quotes.ui.success

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.features.pos.ui.viewmodel.FlowMode
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.features.quotes.domain.QuoteSelectionStore
import com.teco.ventago.navigation.PosScreens
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.cancel
import ventago.composeapp.generated.resources.download_pdf
import ventago.composeapp.generated.resources.email
import ventago.composeapp.generated.resources.invalid_email
import ventago.composeapp.generated.resources.new_quote
import ventago.composeapp.generated.resources.quote_number
import ventago.composeapp.generated.resources.quote_success
import ventago.composeapp.generated.resources.quote_updated
import ventago.composeapp.generated.resources.send_by_email
import ventago.composeapp.generated.resources.view_quotes

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun QuoteSuccessScreen(
    viewModel: PosViewModel,
    navController: NavController,
    navigate: (PosScreens, (NavOptionsBuilder.() -> Unit)?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val invalidEmailText = stringResource(Res.string.invalid_email)
    val quoteNumber = uiState.lastQuoteNumber?.ifBlank { null }
    val defaultEmail = uiState.finalEmail ?: uiState.customer?.email.orEmpty()
    var showEmailSheet by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf(defaultEmail) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var isSendingEmail by remember { mutableStateOf(false) }
    val emailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/57767-done.json").decodeToString()
        )
    }
    val progress by animateLottieCompositionAsState(composition, iterations = 1)

    fun goHome() {
        QuoteSelectionStore.selected = null
        QuoteSelectionStore.startQuoteFlow = false
        QuoteSelectionStore.startOrderFlowFromQuote = false
        viewModel.resetForNewSale()

        val popped = navController.popBackStack(
            route = PosScreens.HomeScreen.name,
            inclusive = false,
            saveState = false
        )
        if (!popped) {
            navigate(PosScreens.HomeScreen) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    BackHandler {
        if (showEmailSheet && !isSendingEmail) {
            showEmailSheet = false
        } else {
            goHome()
        }
    }

    LaunchedEffect(showEmailSheet) {
        if (showEmailSheet) {
            emailInput = defaultEmail
            emailError = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = rememberLottiePainter(
                    composition = composition,
                    progress = { progress }
                ),
                modifier = Modifier.size(180.dp),
                contentDescription = "Success animation"
            )

            Text(
                text = if (uiState.quoteId == null) {
                    stringResource(Res.string.quote_success)
                } else {
                    stringResource(Res.string.quote_updated)
                },
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(12.dp))
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(Res.string.quote_number),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = quoteNumber ?: "-",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            OutlinedButtonM(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showEmailSheet = true }
            ) {
                Text(text = stringResource(Res.string.send_by_email))
            }
            Spacer(Modifier.height(12.dp))
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
            Spacer(Modifier.height(12.dp))
            OutlinedButtonM(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    navigate(PosScreens.QuotesListScreen) {
                        popUpTo(PosScreens.HomeScreen.name) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            ) {
                Text(text = stringResource(Res.string.view_quotes))
            }
            Spacer(Modifier.height(12.dp))
            ButtonM(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    viewModel.resetForNewSale()
                    viewModel.setFlowMode(FlowMode.QUOTE, quoteId = null)
                    QuoteSelectionStore.selected = null
                    QuoteSelectionStore.startQuoteFlow = true
                    QuoteSelectionStore.startOrderFlowFromQuote = false
                    navigate(PosScreens.POSScreen) {
                        popUpTo(PosScreens.HomeScreen.name) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            ) {
                Text(text = stringResource(Res.string.new_quote))
            }
        }
    }

    if (showEmailSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!isSendingEmail) {
                    showEmailSheet = false
                }
            },
            sheetState = emailSheetState
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(Res.string.send_by_email),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(16.dp))
                DMOutlinedTextField(
                    label = stringResource(Res.string.email),
                    modifier = Modifier.fillMaxWidth(),
                    text = emailInput,
                    onChange = {
                        emailInput = it
                        emailError = null
                    },
                    keyboardType = KeyboardType.Email,
                    isError = emailError != null,
                    enabled = !isSendingEmail
                )
                emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (isSendingEmail) {
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                Spacer(Modifier.height(20.dp))
                ButtonM(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSendingEmail,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    onClick = {
                        if (emailInput.isBlank() || !emailInput.contains("@")) {
                            emailError = invalidEmailText
                            return@ButtonM
                        }
                        scope.launch {
                            isSendingEmail = true
                            try {
                                viewModel.sendQuoteEmail(uiState.lastQuoteId, emailInput)
                                showEmailSheet = false
                            } finally {
                                isSendingEmail = false
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(Res.string.send_by_email))
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSendingEmail,
                    onClick = { showEmailSheet = false }
                ) {
                    Text(text = stringResource(Res.string.cancel))
                }
            }
        }
    }
}
