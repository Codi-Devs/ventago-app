package com.teco.ventago.features.payments.ui.yappy

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import kotlinx.coroutines.launch
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.utils.openWhatsappMessage
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.molecules.CircularBadge
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.molecules.payments.CheckMarkItem
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.Gray20
import com.teco.ventago.design_system.theme.Gray50
import com.teco.ventago.design_system.theme.Gray70
import com.teco.ventago.design_system.theme.bodyLargeBold
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.graphicNormalColor
import com.teco.ventago.design_system.theme.headlineSmall
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.payments.ui.yappy.viewmodel.YappyViewModel
import com.teco.ventago.rememberPlatformState
import com.teco.ventago.utils.openCustomTab
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.accept_yappy_payments
import ventago.composeapp.generated.resources.and_the
import ventago.composeapp.generated.resources.are_you_sure
import ventago.composeapp.generated.resources.by_continuing_accept
import ventago.composeapp.generated.resources.cancel
import ventago.composeapp.generated.resources.confirm_unlink_yappy
import ventago.composeapp.generated.resources.connect_yappy
import ventago.composeapp.generated.resources.contact_support_yappy
import ventago.composeapp.generated.resources.copy_paste_merchant_id
import ventago.composeapp.generated.resources.delete
import ventago.composeapp.generated.resources.domain
import ventago.composeapp.generated.resources.domain_exact_match
import ventago.composeapp.generated.resources.email
import ventago.composeapp.generated.resources.fi_rr_headset
import ventago.composeapp.generated.resources.funds_go_to_bank
import ventago.composeapp.generated.resources.link_yappy
import ventago.composeapp.generated.resources.merchant_id
import ventago.composeapp.generated.resources.need_help
import ventago.composeapp.generated.resources.need_help_get_values
import ventago.composeapp.generated.resources.payment_success_fee
import ventago.composeapp.generated.resources.secret_key
import ventago.composeapp.generated.resources.see_help_tutorial
import ventago.composeapp.generated.resources.terms_conditions
import ventago.composeapp.generated.resources.understood
import ventago.composeapp.generated.resources.unlink_yappy
import ventago.composeapp.generated.resources.yappy_account_linked
import ventago.composeapp.generated.resources.yappy_commission_info
import ventago.composeapp.generated.resources.yappy_user_agreement


@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun YappyScreenView(
    viewModel: YappyViewModel,
    navigateBack: () -> Unit = {},
    showHelpFromTopBar: Boolean = false,
//    navigate: (Int, Bundle?, NavOptions?) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })
    val analytics = koinInject<AnalyticsService>()
    val platformState = rememberPlatformState()
    val scope = rememberCoroutineScope()

    var showUnlinkDialog by remember { mutableStateOf(false) }
    var showHelpBottomSheet by remember { mutableStateOf(false) }
    val helpSheetState = rememberModalBottomSheetState()

    // Check if Yappy is not configured (not linked and fields are incomplete)
    val isNotConfigured = !uiState.linkedYappyAccount && !viewModel.canConfigureYappy()
    
    // Intercept system back button if Yappy is not configured
    BackHandler(enabled = isNotConfigured) {
        showHelpBottomSheet = true
    }
    
    // Watch for flag from top app bar back button
    LaunchedEffect(showHelpFromTopBar) {
        if (showHelpFromTopBar && isNotConfigured) {
            showHelpBottomSheet = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
//                is YappyUIEvents.UnlinkedYappyAccount -> navigate(
//                    Res.id.payments_navigation, null, NavOptions.Builder()
//                        .setPopUpTo(Res.id.payments_navigation, true)
//                        .build()
//                )
//                is YappyUIEvents.LinkedYappyAccount -> navigate(
//                    Res.id.payments_navigation, null, NavOptions.Builder()
//                        .setPopUpTo(Res.id.payments_navigation, true)
//                        .build()
//                )
                else -> println("DEBUG Unhandled event: $event")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        if (!uiState.linkedYappyAccount) {
            val helAnnotated = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.W400,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Normal,
                        letterSpacing = 0.0025.em,
                        fontFamily = latoFontFamily(),
                        color = Gray20
                    )
                ) {
                    append(stringResource(Res.string.need_help_get_values))
                }

                append(" ")

                pushStringAnnotation(
                    tag = "check_tutorial",
                    annotation = stringResource(Res.string.see_help_tutorial)
                )
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.W700,
                        fontSize = 14.sp,
                        fontFamily = latoFontFamily(),
                        letterSpacing = 0.04.sp,
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.primary
                    )
                ) {
                    append(stringResource(Res.string.see_help_tutorial))
                }
                pop()
            }

            Text(
                helAnnotated,
                style = bodyMedium().merge(
                    textAlign = TextAlign.Start
                ),
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                    .fillMaxWidth(0.9f)
                    .clickable {
                        openCustomTab("https://www.youtube.com/watch?v=1b2g3h4i5j6")
                    }
            )

            DMOutlinedTextField(
                text = uiState.merchantID,
                label = stringResource(Res.string.merchant_id),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                onChange = { value ->
                    viewModel.onMerchantIDChange(value)
                },
                maxLines = 8,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                enabled = true,
                supportingText = stringResource(Res.string.copy_paste_merchant_id),
                isError = false,
                readOnly = false,
            )

            DMOutlinedTextField(
                text = uiState.domain,
                label = stringResource(Res.string.domain),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                onChange = { value ->
                    viewModel.onDomainChange(value)
                },
                maxLines = 8,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                enabled = true,
                supportingText = stringResource(Res.string.domain_exact_match),
                isError = false,
                readOnly = false,
            )

            DMOutlinedTextField(
                text = uiState.secretKey,
                label = stringResource(Res.string.secret_key),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                onChange = { value ->
                    viewModel.onSecretKeyChange(value)
                },
                maxLines = 8,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                enabled = true,
                isError = false,
                readOnly = false,
            )
        } else {
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
                    Icon(
                        Icons.Rounded.CheckCircleOutline,
                        modifier = Modifier.padding(end = 8.dp, top = 16.dp, bottom = 10.dp)
                            .size(40.dp),
                        contentDescription = null,
                        tint = Color(0xFF0070BA)
                    )
                    Text(
                        stringResource(Res.string.yappy_account_linked),
                        modifier = Modifier.padding(bottom = 16.dp),
                        style = headlineSmall()
                    )
                    HorizontalDivider(thickness = 1.dp, color = Gray70)
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(Res.string.payment_success_fee),
                            style = titleMediumBold()
                        )
                        CircularBadge(
                            color = graphicNormalColor,
                            text = "1%",
                        )
                    }

                }
            }
        }

        Spacer(Modifier.weight(1f))


        if (uiState.linkedYappyAccount) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButtonS(
                    label = stringResource(Res.string.unlink_yappy),
                    color = MaterialTheme.colorScheme.error,
                ) {
                    showUnlinkDialog = true
                }
            }
        } else {
            val termsAnnotated = buildAnnotatedString {
                append(stringResource(Res.string.by_continuing_accept))
                
                pushStringAnnotation(
                    tag = "terms",
                    annotation = "https://www.yappy.com.pa/comercial/terminos-y-condiciones/"
                )
                withStyle(
                    style = SpanStyle(
                        color = Color(0xFF0070BA),
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(stringResource(Res.string.terms_conditions))
                }
                pop()
                
                append(stringResource(Res.string.and_the))
                
                pushStringAnnotation(
                    tag = "privacy",
                    annotation = "https://www.yappy.com.pa/comercial/aviso-de-privacidad/"
                )
                withStyle(
                    style = SpanStyle(
                        color = Color(0xFF0070BA),
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(stringResource(Res.string.yappy_user_agreement))
                }
                pop()
                
                append(".")
            }
            
            ClickableText(
                text = termsAnnotated,
                style = labelSmall(color = Gray50).copy(textAlign = TextAlign.Center),
                modifier = Modifier.padding(bottom = 24.dp, start = 16.dp, end = 16.dp, top = 16.dp),
                onClick = { offset ->
                    termsAnnotated.getStringAnnotations(tag = "terms", start = offset, end = offset).firstOrNull()?.let {
                        openCustomTab("https://www.yappy.com.pa/comercial/terminos-y-condiciones/")
                    }
                    termsAnnotated.getStringAnnotations(tag = "privacy", start = offset, end = offset).firstOrNull()?.let {
                        openCustomTab("https://www.yappy.com.pa/comercial/aviso-de-privacidad/")
                    }
                }
            )

            ButtonM(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                enabled = viewModel.canConfigureYappy(),
                onClick = {
                    analytics.logEvent(
                        "payments_click_connect",
                        analytics.getAnalyticsBundle().apply {
                            "payment_method" to "yappy"
                        })
                    viewModel.connectYappy()
                }) {
                Text(
                    stringResource(Res.string.link_yappy),
                    style = bodyLargeBold(color = Color.White)
                )
            }
        }


        if (uiState.showIntroDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(text = stringResource(Res.string.connect_yappy)) },
                text = {
                    Column {
                        CheckMarkItem(text = stringResource(Res.string.accept_yappy_payments))
                        CheckMarkItem(text = stringResource(Res.string.yappy_commission_info))
                        CheckMarkItem(
                            text = stringResource(Res.string.funds_go_to_bank),
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }

                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.showIntroDialog(false)
                    }) { Text(text = stringResource(Res.string.understood)) }
                },
                containerColor = MaterialTheme.colorScheme.background
            )
        }

        if (showUnlinkDialog) {
            DMAlertDialog(
                title = stringResource(Res.string.are_you_sure),
                message = stringResource(Res.string.confirm_unlink_yappy),
                show = showUnlinkDialog,
                onDismiss = {
                    showUnlinkDialog = false
                },
                onConfirm = {
                    analytics.logEvent(
                        "payments_method_unlinked",
                        analytics.getAnalyticsBundle().apply {
                            "payment_method" to "yappy"
                        })
                    viewModel.unlinkYappy()
                    showUnlinkDialog = false
                },
                confirmText = stringResource(Res.string.delete),
                dismissText = stringResource(Res.string.cancel)
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

        // Help bottom sheet when trying to go back without configuring
        if (showHelpBottomSheet) {
            ModalBottomSheet(
                containerColor = MaterialTheme.colorScheme.background,
                sheetState = helpSheetState,
                onDismissRequest = {
                    scope.launch {
                        helpSheetState.hide()
                    }.invokeOnCompletion {
                        showHelpBottomSheet = false
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Necesitas ayuda para configurar Yappy. Contacta a nuestro equipo de soporte estamos para ayudarte",
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = latoFontFamily(),
                            fontWeight = FontWeight.W400,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    ButtonM(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        onClick = {
                            openWhatsappMessage("50763879477", "Necesito ayuda para configurar Yappy")
                            scope.launch {
                                helpSheetState.hide()
                            }.invokeOnCompletion {
                                showHelpBottomSheet = false
                            }
                        }
                    ) {
                        Text(
                            text = "Contactar por WhatsApp",
                            style = bodyLargeBold(color = Color.White)
                        )
                    }

                    TextButtonS(
                        label = "Tal vez más tarde",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            scope.launch {
                                helpSheetState.hide()
                            }.invokeOnCompletion {
                                showHelpBottomSheet = false
                                navigateBack()
                            }
                        }
                    )
                }
            }
        }
    }

}