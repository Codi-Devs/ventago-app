package com.teco.ventago.features.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.AppViewModel
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.organism.BarGraphic
import com.teco.ventago.design_system.organism.HomeTopCard
import com.teco.ventago.design_system.organism.SupportCard
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.headlineLarge
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.home.ui.viewmodel.HomeViewModel
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.rememberPlatformState
import com.teco.ventago.utils.openWhatsappMessage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.configure
import ventago.composeapp.generated.resources.connect_paypal_subtitle
import ventago.composeapp.generated.resources.connect_paypal_title
import ventago.composeapp.generated.resources.dgi
import ventago.composeapp.generated.resources.ic_arrow_forward_ios
import ventago.composeapp.generated.resources.ic_paypal_onboarding
import ventago.composeapp.generated.resources.pos
import ventago.composeapp.generated.resources.sales


@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel<HomeViewModel>(),
    appViewModel: AppViewModel = koinViewModel<AppViewModel>(),
    navigate: (PosScreens) -> Unit
) {
    val platformState = rememberPlatformState()

    val uiState by viewModel.uiState.collectAsState()

    val appState = appViewModel.mainState.collectAsState()

    if (uiState.isLoadingData) {
        HomeLoadingScreen()
        return
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
    ) {
        HomeTopCard(
            title = uiState.business?.name ?: "",
            image = uiState.business?.logo,
            onAddImageClick = {
                navigate(PosScreens.BusinessLogoSettingsScreen)
            },
        )

        if (uiState.invoicingEnabled && uiState.invoicingPlanState != null) {
            InvoicingPlanCard(
                modifier = Modifier.padding(top = 16.dp),
                initialQuota = uiState.invoicingPlanState?.totalDtes ?: 0,
                remainingQuota = uiState.invoicingPlanState?.availableDtes ?: 0,
                activationDate = uiState.invoicingPlanState?.activationDate ?: "-",
                expirationDate = uiState.invoicingPlanState?.expirationDate ?: "-",
                onSeeInvoices = { navigate(PosScreens.Orders) },           // or your invoices section
                onBuyStamps = {
                    openWhatsappMessage("50763879477", "Hola, quiero renovar mi plan de folios.")
                }           // or a purchase flow
            )
        } else if (!uiState.invoicingEnabled) {
            Card(
                modifier = Modifier.padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardContainerColor()
                ),
            ) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp).height(30.dp).width(54.dp),
                        painter = painterResource(Res.drawable.dgi),
                        contentDescription = "",
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                    ) {
                        Text(
                            text = "Comienza a facturar electrónicamente.", style = titleMediumBold().merge(
                                TextStyle(
                                    textAlign = TextAlign.Start
                                )
                            )
                        )
                        Text(
                            text = "Contacta a nuestro equipo para asesorarte y activar tu firma digital ante la DGI.", style = bodyMedium().merge(
                                TextStyle(
                                    textAlign = TextAlign.Start
                                )
                            )
                        )
                        Text(
                            text = "*Si no activas tu firma en 15 días se desactivará tu cuenta.", style = labelSmall().merge(
                                TextStyle(
                                    color = Color(0xFF8A8E93),
                                    textAlign = TextAlign.Start
                                )
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize().padding(end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary/* Other colors use values from MaterialTheme */
                        ),
                        onClick = {
                            navigate(PosScreens.Invoicing)
                        },
                        modifier = Modifier
                            .padding(start = 16.dp, top = 16.dp)
                            .height(42.dp)
                            .wrapContentWidth(),
                        content = {
                            Icon(
                                modifier = Modifier
                                    .padding(start = 8.dp, end = 8.dp)
                                    .size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                imageVector = Icons.AutoMirrored.Rounded.Chat,
                                contentDescription = ""
                            )
                            Text(
                                modifier = Modifier.padding(end = 16.dp),
                                text = "Contactanos",
                                style = bodyMediumBold(color = MaterialTheme.colorScheme.onPrimary)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        enabled = true
                    )
                }
            }
        }




        Text(
            stringResource(Res.string.sales),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            style = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = latoFontFamily(),
                fontWeight = FontWeight(500),
                letterSpacing = 0.15.sp,
            )
        )
        BarGraphic(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
            data = uiState.sales ?: mutableListOf(),
            selectedIndex = uiState.selectedSalesIndex,
            barGraphicHeight = 140.0,
        ) {
            viewModel.setSelectedSalesIndex(it)
        }


        Card(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp).height(90.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            onClick = {
                navigate(PosScreens.POS)
            }
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                    painter = painterResource(Res.drawable.pos),
                    contentDescription = "",
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSecondary),
                )

                Text(
                    text = stringResource(Res.string.pos),
                    style = headlineLarge().copy(color = MaterialTheme.colorScheme.onSecondary)
                )

                Spacer(modifier = Modifier.weight(1f))

                Image(
                    modifier = Modifier.padding(start = 8.dp, end = 16.dp).height(40.dp),
                    painter = painterResource(Res.drawable.ic_arrow_forward_ios),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSecondary),
                    contentDescription = "",
                )
            }

        }


        if (!appState.value.paymentsConfigured) {
            Card(
                modifier = Modifier.padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardContainerColor()
                ),
            ) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp).height(40.dp)
                            .width(54.dp),
                        painter = painterResource(Res.drawable.ic_paypal_onboarding),
                        contentDescription = "",
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.connect_paypal_title),
                            style = titleMediumBold().merge(
                                TextStyle(
                                    textAlign = TextAlign.Start
                                )
                            )
                        )
                        Text(
                            text = stringResource(Res.string.connect_paypal_subtitle),
                            style = bodyMedium().merge(
                                TextStyle(
                                    textAlign = TextAlign.Start
                                )
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize().padding(end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary/* Other colors use values from MaterialTheme */
                        ),
                        onClick = {
                            navigate(PosScreens.Payments)
                        },
                        modifier = Modifier
                            .padding(start = 16.dp, top = 16.dp)
                            .height(42.dp)
                            .wrapContentWidth(),
                        content = {
                            Icon(
                                modifier = Modifier
                                    .padding(start = 0.dp, end = 8.dp)
                                    .size(30.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                imageVector = Icons.Rounded.Payment,
                                contentDescription = ""
                            )
                            Text(
                                modifier = Modifier.padding(end = 16.dp),
                                text = stringResource(Res.string.configure),
                                style = bodyMediumBold(color = MaterialTheme.colorScheme.onPrimary)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        enabled = true
                    )
                }
            }
        }



        SupportCard(uiState.unreadCount) {
            platformState.openEmailIntent("support@tecodigi.com")
        }

    }
}


@Composable
fun HomeLoadingScreen() {
    val brush = shimmerBrush()

    LazyColumn(modifier = Modifier.padding(0.dp)) {
        item {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(start = 0.dp)
                    .clip(shape = RoundedCornerShape(bottomEnd = 20.dp, bottomStart = 20.dp))
                    .background(brush = brush)
            )
        }
        item {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .clip(shape = RoundedCornerShape(4.dp))
                    .background(brush = brush)
            )
        }
        item {
            LazyRow(modifier = Modifier.padding(top = 8.dp)) {
                repeat(6) {
                    item {
                        Spacer(
                            modifier = Modifier
                                .padding(start = 16.dp, top = 8.dp)
                                .width(70.dp)
                                .height(70.dp)
                                .clip(shape = RoundedCornerShape(8.dp))
                                .background(brush = brush)
                        )
                    }
                }
            }
        }
        repeat(6) {
            item {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        .clip(shape = RoundedCornerShape(6.dp))
                        .background(brush = brush)
                )
            }
        }
    }
}

@Composable
fun InvoicingPlanCard(
    modifier: Modifier = Modifier,
    // Quotas (pass counts)
    initialQuota: Int,
    remainingQuota: Int,

    // Dates (pass already formatted strings like "01/10/2025")
    activationDate: String,
    expirationDate: String,

    // Optional actions
    onSeeInvoices: (() -> Unit)? = null,
    onBuyStamps: (() -> Unit)? = null
) {
    val used = (initialQuota - remainingQuota).coerceAtLeast(0)
    val progress = if (initialQuota > 0) used.toFloat() / initialQuota.toFloat() else 0f

    val isExpired = remainingQuota <= 0
    // Consider “expiring soon” if < 15% remaining but > 0
    val isExpiringSoon = !isExpired && initialQuota > 0 && remainingQuota <= (initialQuota * 0.15f)

    val statusText = when {
        isExpired -> "Expirada"
        isExpiringSoon -> "Por expirar"
        else -> "Activa"
    }

    val statusColors = when {
        isExpired -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        isExpiringSoon -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        else -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
    }

    Card(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Plan de facturación",
                    style = titleMediumBold()
                )
                Spacer(Modifier.weight(1f))
                SuggestionChip(
                    onClick = {},
                    label = { Text(statusText, maxLines = 1) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = statusColors.first,
                        labelColor = statusColors.second
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = statusColors.first,
                        borderWidth = 0.dp
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Progress
            Column {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = if (isExpired) MaterialTheme.colorScheme.error
                    else if (isExpiringSoon) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "Inicial: $initialQuota",
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "Usados: $used",
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "Restantes: $remainingQuota",
                        style = labelSmall(
                            color = when {
                                isExpired -> MaterialTheme.colorScheme.error
                                isExpiringSoon -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(12.dp))

            // Dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.EventAvailable,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Activación", style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(activationDate, style = bodyMedium())
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.EventBusy,
                            contentDescription = null,
                            tint = if (isExpired || isExpiringSoon)
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Expiración", style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        expirationDate,
                        style = bodyMedium().merge(
                            TextStyle(
                                color = if (isExpired) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    )
                }
            }

            // Optional actions
            if (onSeeInvoices != null || onBuyStamps != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (onSeeInvoices != null) {
                        TextButton(onClick = onSeeInvoices) {
                            Icon(
                                imageVector = Icons.Rounded.Description,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Ver facturas")
                        }
                    }
                    if (onBuyStamps != null) {
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onBuyStamps,
                            shape = RoundedCornerShape(10.dp),
                            enabled = true
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddShoppingCart,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Comprar folios")
                        }
                    }
                }
            }
        }
    }
}