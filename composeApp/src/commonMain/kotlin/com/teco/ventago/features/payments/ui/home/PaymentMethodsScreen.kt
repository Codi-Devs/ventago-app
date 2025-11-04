package com.teco.ventago.features.payments.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.Navigation
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.molecules.payments.CheckMarkItem
import com.teco.ventago.design_system.molecules.payments.PaymentItem
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.theme.Gray30
import com.teco.ventago.design_system.theme.Gray50
import com.teco.ventago.design_system.theme.bodyLargeBold
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.headlineMediumBold
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.design_system.theme.titleSmallBold
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentMethodsViewModel
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentUiEvent
import com.teco.ventago.features.payments.ui.paypal.PaypalLoadingScreen
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.rememberPlatformState
import com.teco.ventago.utils.DateFormat
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.openCustomTab
import com.teco.ventago.utils.toDecimalString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.accept
import ventago.composeapp.generated.resources.accept_and_continue
import ventago.composeapp.generated.resources.accept_online_payments
import ventago.composeapp.generated.resources.accept_via_qr_or_links
import ventago.composeapp.generated.resources.authorize_billing_agreement
import ventago.composeapp.generated.resources.bank_transfer
import ventago.composeapp.generated.resources.cancel
import ventago.composeapp.generated.resources.charge_date
import ventago.composeapp.generated.resources.charge_explanation
import ventago.composeapp.generated.resources.commissions
import ventago.composeapp.generated.resources.confirm_address
import ventago.composeapp.generated.resources.contact_support
import ventago.composeapp.generated.resources.create_and_share_links
import ventago.composeapp.generated.resources.email
import ventago.composeapp.generated.resources.enable_auto_charges
import ventago.composeapp.generated.resources.fi_rr_headset
import ventago.composeapp.generated.resources.get_paid_easily
import ventago.composeapp.generated.resources.ic_bank
import ventago.composeapp.generated.resources.ic_paypal_logo
import ventago.composeapp.generated.resources.multiple_payment_options
import ventago.composeapp.generated.resources.need_address_desc
import ventago.composeapp.generated.resources.need_help
import ventago.composeapp.generated.resources.payment_methods
import ventago.composeapp.generated.resources.paypal
import ventago.composeapp.generated.resources.pending_charges
import ventago.composeapp.generated.resources.secure_shield
import ventago.composeapp.generated.resources.security_priority
import ventago.composeapp.generated.resources.suspension_warning
import ventago.composeapp.generated.resources.total_to_charge
import ventago.composeapp.generated.resources.we_only_charge_when_you_do
import ventago.composeapp.generated.resources.yappy_logo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingPaymentScreen(
    viewModel: PaymentMethodsViewModel,
    navigate: (PosScreens) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PaymentUiEvent.OpenBillingAgreementUrl -> openCustomTab(event.url)
                is PaymentUiEvent.OpenPaypalScreen -> navigate(PosScreens.PaymentsPaypalScreen)
                is PaymentUiEvent.OpenPaypalOnboarding -> navigate(PosScreens.PaymentsPaypalOnboardingScreen)
//                is PaymentUiEvent.ErrorLoadingSummary -> navigateBack()
                else -> {}
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .fillMaxSize(),
    ) {

        when {
            uiState.loadingSummaryData -> PaypalLoadingScreen()
            uiState.showOnboarding ->PaymentsOnboarding(viewModel, navigate)
            else -> PaymentsMethods(viewModel, navigate)
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
fun PaymentsMethods(
    viewModel: PaymentMethodsViewModel,
    navigate: (PosScreens) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val analytics = koinInject<AnalyticsService>()
    val platformState = rememberPlatformState()

    if (uiState.tab == 1) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            uiState.availablePaymentMethods["paypal"]?.takeIf { it.visible }?.let { method ->
                PaymentItem(
                    modifier = Modifier,
                    itemId = method.id,
                    title = {
                        Image(
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 8.dp,
                                top = 12.dp,
                                bottom = 12.dp
                            ),
                            painter = painterResource(Res.drawable.paypal),
                            contentDescription = "",
                        )
                    },
                    enabled = method.enabled,
                    onClick = { _ ->
                        viewModel.openPaypal()
                    },
                )
            }

            uiState.availablePaymentMethods["yappy"]?.takeIf { it.visible }?.let { method ->
                PaymentItem(
                    modifier = Modifier,
                    itemId = method.id,
                    title = {
                        Image(
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 8.dp,
                                top = 12.dp,
                                bottom = 12.dp
                            ),
                            painter = painterResource(Res.drawable.yappy_logo),
                            contentDescription = "",
                        )
                    },
                    enabled = method.enabled,
                    onClick = { id ->
                        navigate(PosScreens.PaymentsYappyScreen)
                    },
                )
            }

            uiState.availablePaymentMethods["transference"]?.takeIf { it.visible }?.let { method ->
                PaymentItem(
                    modifier = Modifier,
                    itemId = method.id,
                    title = {
                        Image(
                            modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
                            painter = painterResource(Res.drawable.ic_bank),
                            contentDescription = "",
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground) // Tint the icon
                        )
                        Text(
                            stringResource(Res.string.bank_transfer),
                            style = titleSmallBold(),
                            modifier = Modifier.padding(
                                start = 8.dp,
                                end = 8.dp,
                                top = 10.dp,
                                bottom = 10.dp
                            )
                        )
                    },
                    enabled = method.enabled,
                    onClick = { _ ->
                        navigate(PosScreens.PaymentsTransferenceScreen)
                    },
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // Summary Data
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardContainerColor(),
                ),
            ) {
                Text(
                    stringResource(Res.string.pending_charges),
                    style = titleMediumBold(),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(Res.string.total_to_charge), style = bodyMedium())
                    Text(
                        formatNumberToMoney(uiState.pendingFees.toDecimalString()),
                        style = bodyMediumBold()
                    )
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(Res.string.charge_date), style = bodyMedium())
                    Text(
                        DateFormat.getFormattedDate(
                            uiState.nextBillingDate,
                            "yyyy-MM-dd'T'HH:mm:ss", "dd MMMM yyyy"
                        ), style = bodyMediumBold()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(Res.string.charge_explanation),
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(
                        top = 4.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    ),
                )
            }

            // Link Billing Agreement
            if (!uiState.linkedBillingAgreement) {
                ElevatedCard(
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
                            style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
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
                                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
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
                            Text(
                                stringResource(Res.string.accept_and_continue),
                                style = bodyLargeBold(color = Color.White)
                            )
                        }

                    }
                }
            }
        }
    }

    if (uiState.showBottomBar && !uiState.loadingSummaryData) {
        Column {
            Spacer(modifier = Modifier.weight(1f))
            Box(Modifier.fillMaxSize()) {
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = 16.dp, bottom = 16.dp,
                            end = 16.dp
                        ) // float above bottom/sides
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavigationChip(
                            selected = (uiState.tab == 1),
                            onClick = { viewModel.onTabSelected(1) },
                            icon = { Icon(Icons.Filled.Payment, null) },
                            label = { Text(stringResource(Res.string.payment_methods)) }
                        )
                        NavigationChip(
                            selected = (uiState.tab == 2),
                            onClick = { viewModel.onTabSelected(2) },
                            icon = {
                                BadgedBox(badge = {
                                    if (!uiState.linkedBillingAgreement) {
                                    Badge {
                                        Text("!")
                                    }
                                }
                                }) { Icon(Icons.Filled.LocalAtm, null) }
                            },
                            label = { Text(stringResource(Res.string.commissions)) }
                        )
                    }
                }
            }
//            NavigationBar(
//                modifier = Modifier.fillMaxWidth().shadow(
//                    elevation = 10.dp,
//                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
//                ),
//                containerColor = androidx.compose.material.MaterialTheme.colors.background,
//                tonalElevation = 8.dp,
//
//                )
//            {
//
//                NavigationBarItem(
//                    selected = uiState.tab == 1,
//                    onClick = {
//                        viewModel.onTabSelected(1)
//                    },
//                    icon = {
//                        Icon(
//                            imageVector = Icons.Filled.Payment,
//                            contentDescription = "Home"
//                        )
//                    },
//                    label = {
//                        Text(stringResource(Res.string.payment_methods))
//                    }
//                )
//                NavigationBarItem(
//                    selected = uiState.tab == 2,
//                    onClick = {
//                        viewModel.onTabSelected(2)
//                    },
//                    icon = {
//                        BadgedBox(
//                            badge = {
//                                // Show badge only if you want to (eg: if newPayments > 0)
//                                if (!uiState.linkedBillingAgreement) {
//                                    Badge {
//                                        Text("!")
//                                    }
//                                }
//                            }
//                        ) {
//                            Icon(
//                                imageVector = Icons.Filled.LocalAtm,
//                                contentDescription = "Home"
//                            )
//                        }
//                    },
//                    label = {
//                        Text(stringResource(Res.string.commissions))
//                    }
//                )
//            }
        }
    }

}

@Composable
private fun NavigationChip(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = label,
        leadingIcon = { icon() },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
            labelColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            leadingIconContentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
fun PaymentsOnboarding(
    viewModel: PaymentMethodsViewModel,
    navigate: (PosScreens) -> Unit
) {
    val showEmptyAddressDialog = remember { mutableStateOf(false) }
    val analytics = koinInject<AnalyticsService>()
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        ElevatedCard(
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
                Image(
                    painterResource(Res.drawable.secure_shield),
                    modifier = Modifier
                        .padding(bottom = 24.dp, top = 8.dp)
                        .size(120.dp),
                    contentDescription = null,
                )

                Text(
                    stringResource(Res.string.get_paid_easily),
                    style = headlineMediumBold(),
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                )

                Text(
                    stringResource(Res.string.accept_online_payments),
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                )

                CheckMarkItem(text =  stringResource(Res.string.create_and_share_links),)
                CheckMarkItem(text =  stringResource(Res.string.accept_via_qr_or_links),)
                CheckMarkItem(text =  stringResource(Res.string.multiple_payment_options),)
            }
        }


        Spacer(modifier = Modifier.weight(1f))

        ButtonM(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            onClick = {
                if (viewModel.isAddressEmpty()) {
                    showEmptyAddressDialog.value = true
                } else {
                    analytics.logEvent("payments_onboarding_accept_and_continue")
                    viewModel.onboardPayments()
                }
            }) {
            Text(
                stringResource(Res.string.accept_and_continue),
                style = bodyLargeBold(color = MaterialTheme.colorScheme.onPrimary)
            )
        }

        Text(
            stringResource(Res.string.security_priority),
            style = labelSmall(color = Gray50),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp, top = 24.dp)
        )

        DMAlertDialog(
            title = stringResource(Res.string.confirm_address),
            message = stringResource(Res.string.need_address_desc),
            show = showEmptyAddressDialog.value,
            onDismiss = {
                showEmptyAddressDialog.value = false
            },
            onConfirm = {
                showEmptyAddressDialog.value = false
                navigate(PosScreens.BusinessAddressSettingsScreen)
            },
            confirmText = stringResource(Res.string.accept),
            dismissText = stringResource(Res.string.cancel)
        )
    }
}