package com.teco.ventago.features.payments.ui.paypal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.molecules.payments.CheckMarkItem
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.theme.Gray50
import com.teco.ventago.design_system.theme.bodyLargeBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.headlineMediumBold
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.features.payments.ui.paypal.viewmodel.PaypalUIEvents
import com.teco.ventago.features.payments.ui.paypal.viewmodel.PaypalViewModel
import com.teco.ventago.utils.openCustomTab
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.accept_payments_paypal
import ventago.composeapp.generated.resources.and_the
import ventago.composeapp.generated.resources.by_continuing_accept
import ventago.composeapp.generated.resources.commission_2_percent
import ventago.composeapp.generated.resources.connect_paypal_account
import ventago.composeapp.generated.resources.fast_secure_professional
import ventago.composeapp.generated.resources.ic_paypal_logo
import ventago.composeapp.generated.resources.ic_paypal_onboarding
import ventago.composeapp.generated.resources.money_goes_to_paypal
import ventago.composeapp.generated.resources.paypal_authorize_message
import ventago.composeapp.generated.resources.paypal_charges
import ventago.composeapp.generated.resources.paypal_user_agreement
import ventago.composeapp.generated.resources.terms_conditions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingPaypalScreen(
    viewModel: PaypalViewModel,
//    navigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })
    val analytics = koinInject<AnalyticsService>()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PaypalUIEvents.OpenConnectUrl -> openCustomTab(event.url)
//                is PaypalUIEvents.ErrorLoadingSummary -> navigateBack()
                else -> println("DEBUG Unhandled event: $event")
            }
        }
    }

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
                    painterResource(Res.drawable.ic_paypal_onboarding),
                    modifier = Modifier
                        .padding(bottom = 24.dp, top = 8.dp)
                        .size(120.dp),
                    contentDescription = null,
                )

                Text(
                    stringResource(Res.string.paypal_charges),
                    style = headlineMediumBold(),
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                )

                CheckMarkItem(text = stringResource(Res.string.accept_payments_paypal))
                CheckMarkItem(text = stringResource(Res.string.fast_secure_professional))
                CheckMarkItem(text = stringResource(Res.string.commission_2_percent))
                CheckMarkItem(
                    text = stringResource(Res.string.money_goes_to_paypal),
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
        }


        Spacer(modifier = Modifier.weight(1f))

        Text(
            buildAnnotatedString {
                append(stringResource(Res.string.by_continuing_accept))
                withLink(
                    LinkAnnotation.Url(
                        url = "https://sites.google.com/view/conditionsofservice/p%C3%A1gina-principal",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = Color(0xFF0070BA),
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    )
                ) {
                    append(stringResource(Res.string.terms_conditions))
                }
                append(stringResource(Res.string.and_the))

                withLink(
                    LinkAnnotation.Url(
                        url = "https://sites.google.com/view/menucodi-paypal",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = Color(0xFF0070BA),
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    )
                ) {
                    append(stringResource(Res.string.paypal_user_agreement))
                }
                append(".")
            },
            style = labelSmall(color = Gray50),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp, start = 4.dp, end = 4.dp),
        )

        ButtonM(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            containerColor = Color(0xFF0070BA),
            onClick = {
                analytics.logEvent(
                    "payments_click_connect",
                    analytics.getAnalyticsBundle().apply {
                        "payment_method" to "paypal"
                    })
                viewModel.connectPaypal()
            }) {
            Icon(
                painterResource(Res.drawable.ic_paypal_logo),
                modifier = Modifier.padding(end = 8.dp),
                contentDescription = null,
            )
            Text(
                stringResource(Res.string.connect_paypal_account),
                style = bodyLargeBold(color = Color.White)
            )
        }

        Text(
            stringResource(Res.string.paypal_authorize_message),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 8.dp),
            textAlign = TextAlign.Center,
            style = labelSmall(color = Gray50)
        )

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
fun PaypalLoadingScreen() {
    val brush = shimmerBrush()

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .clip(shape = RoundedCornerShape(4.dp))
                .background(brush = brush)
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .clip(shape = RoundedCornerShape(4.dp))
                .background(brush = brush)
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .clip(shape = RoundedCornerShape(4.dp))
                .background(brush = brush)
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .clip(shape = RoundedCornerShape(4.dp))
                .background(brush = brush)
        )
    }
}

