package com.teco.ventago.features.payments.ui.paypal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.molecules.CircularBadge
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.theme.Gray30
import com.teco.ventago.design_system.theme.Gray70
import com.teco.ventago.design_system.theme.bodyLargeBold
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.graphicNormalColor
import com.teco.ventago.design_system.theme.headlineMediumBold
import com.teco.ventago.design_system.theme.headlineSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.payments.ui.paypal.viewmodel.PaypalUIEvents
import com.teco.ventago.features.payments.ui.paypal.viewmodel.PaypalViewModel
import com.teco.ventago.utils.openCustomTab
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.accept_and_continue
import ventago.composeapp.generated.resources.are_you_sure
import ventago.composeapp.generated.resources.authorize_billing_agreement
import ventago.composeapp.generated.resources.cancel
import ventago.composeapp.generated.resources.check_paypal_commissions
import ventago.composeapp.generated.resources.confirm_unlink_paypal
import ventago.composeapp.generated.resources.delete
import ventago.composeapp.generated.resources.enable_auto_charges
import ventago.composeapp.generated.resources.ic_paypal_logo
import ventago.composeapp.generated.resources.linked_account
import ventago.composeapp.generated.resources.payment_success_fee
import ventago.composeapp.generated.resources.paypal_commissions
import ventago.composeapp.generated.resources.paypal_connected
import ventago.composeapp.generated.resources.suspension_warning
import ventago.composeapp.generated.resources.unlink_paypal
import ventago.composeapp.generated.resources.we_only_charge_when_you_do

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaypalScreenContent(
    viewModel: PaypalViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showUnlinkDialog by remember { mutableStateOf(false) }
    val analytics = koinInject<AnalyticsService>()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PaypalUIEvents.OpenBillingAgreementUrl -> openCustomTab(event.url)
//                is PaypalUIEvents.ErrorLoadingSummary -> navigateBack()
//                is PaypalUIEvents.UnlinkedPaypalAccount -> navigate(
//                    R.id.payments_navigation, null, NavOptions.Builder()
//                        .setPopUpTo(R.id.payments_navigation, true)
//                        .build()
//                )
                else -> println("DEBUG Unhandled event: $event")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {

        // Summary Data
        ElevatedCard (
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardContainerColor(),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Rounded.CheckCircleOutline,
                    modifier = Modifier.padding(end = 8.dp, top = 16.dp, bottom = 10.dp).size(40.dp),
                    contentDescription = null,
                    tint = Color(0xFF0070BA)
                )
                Text(
                    stringResource(Res.string.paypal_connected),
                    modifier = Modifier.padding(bottom = 16.dp),
                    style = headlineSmall())

            }

            HorizontalDivider(thickness = 1.dp, color = Gray70)

            Text(
                stringResource(Res.string.linked_account),
                style = titleMediumBold(),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                color = Color.Black)
            Spacer(modifier = Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(uiState.linkedEmail, style = bodyMedium())
            }

            HorizontalDivider(thickness = 1.dp, color = Gray70)
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 4.dp), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.payment_success_fee), style = titleMediumBold())
                CircularBadge(
                    color = graphicNormalColor,
                    text = "2%",
                )
            }

            Text(
                buildAnnotatedString {
                    append(stringResource(Res.string.paypal_commissions))
                    withLink(
                        LinkAnnotation.Url(
                            url = "https://www.paypal.com/us/business/paypal-business-fees",
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = Color(0xFF0070BA),
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        )
                    ) {
                        append(stringResource(Res.string.check_paypal_commissions))
                    }
                    append(".")
                },
                style = bodyMedium(color = Gray30),
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(bottom = 10.dp, start = 16.dp, end = 16.dp),
            )

        }

        // Link Billing Agreement
        if (!uiState.linkedBillingAgreement) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(Res.string.enable_auto_charges),
                        style = headlineMediumBold(),
                        modifier = Modifier.padding(
                            start = 24.dp,
                            end = 24.dp,
                            bottom = 16.dp,
                            top = 16.dp
                        ),
                    )

                    Text(
                        stringResource(Res.string.authorize_billing_agreement),
                        style = bodyMedium(color = Gray30),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    )


                    Row(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(16.dp),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            stringResource(Res.string.suspension_warning),
                            style = bodyMedium(),
                            textAlign = TextAlign.Start,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .padding(start = 24.dp, end = 24.dp, bottom = 10.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(16.dp),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(Res.string.we_only_charge_when_you_do),
                            style = bodyMedium(color = Gray30),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    ButtonM(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        containerColor = Color(0xFF0070BA),
                        onClick = {
                            analytics.logEvent("paypal_click_create_agreement")
                            viewModel.createBillingAgreement()
                        }) {
                        Icon(
                            painterResource(Res.drawable.ic_paypal_logo),
                            modifier = Modifier.padding(end = 8.dp),
                            contentDescription = null,
                        )
                        Text(stringResource(Res.string.accept_and_continue), style = bodyLargeBold(color = Color.White))
                    }

                }
            }
        }

        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.Center) {
            TextButtonS(
                label = stringResource(Res.string.unlink_paypal),
                color = MaterialTheme.colorScheme.error,
            ) {
                showUnlinkDialog = true
            }
        }


        if (uiState.loadingBottomSheet.isLoading()) {
            LoadingSheet(
                state = uiState.loadingBottomSheet,
                sheetState = loadingSheetState
            ) {
                viewModel.hideLoading()
            }
        }

        if (showUnlinkDialog) {
            DMAlertDialog(
                title = stringResource(Res.string.are_you_sure),
                message = stringResource(Res.string.confirm_unlink_paypal),
                show = showUnlinkDialog,
                onDismiss = {
                    showUnlinkDialog = false
                },
                onConfirm = {
//                    AnalyticsHelper.logEvent("payments_method_unlinked", AnalyticsHelper.getAnalyticsBundle().apply {
//                        putString("payment_method", "paypal")
//                        putString("paypal_email", uiState.linkedEmail)
//                    })
                    viewModel.unlinkPaypal()
                    showUnlinkDialog = false
                },
                confirmText = stringResource(Res.string.delete),
                dismissText = stringResource(Res.string.cancel)
            )
        }
    }
}

