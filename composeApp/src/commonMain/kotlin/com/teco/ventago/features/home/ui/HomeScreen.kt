package com.teco.ventago.features.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.AppViewModel
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.molecules.flags.DgiDownAlertBanner
import com.teco.ventago.design_system.organism.HomeTopCard
import com.teco.ventago.design_system.organism.SalesLineGraphic
import com.teco.ventago.design_system.organism.SupportCard
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.headlineLarge
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.core.flags.IFlagsService
import com.teco.ventago.features.home.ui.viewmodel.HomeViewModel
import com.teco.ventago.features.home.domain.model.HomeSalesRange
import com.teco.ventago.features.home.domain.model.HomeSummary
import com.teco.ventago.core.LocalStorage
import com.teco.ventago.features.quotes.domain.QuotesOnboarding
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.rememberPlatformState
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.openWhatsappMessage
import com.teco.ventago.utils.toDecimalString
import com.teco.ventago.utils.toLongCents
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.round
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.action_continue
import ventago.composeapp.generated.resources.configure
import ventago.composeapp.generated.resources.connect_paypal_subtitle
import ventago.composeapp.generated.resources.connect_paypal_title
import ventago.composeapp.generated.resources.ic_arrow_forward_ios
import ventago.composeapp.generated.resources.ic_paypal_onboarding
import ventago.composeapp.generated.resources.pos
import ventago.composeapp.generated.resources.pos_clients
import ventago.composeapp.generated.resources.pos_new_quote
import ventago.composeapp.generated.resources.quotes
import ventago.composeapp.generated.resources.quotes_badge_beta
import ventago.composeapp.generated.resources.quotes_badge_new
import ventago.composeapp.generated.resources.quotes_welcome_message
import ventago.composeapp.generated.resources.quotes_welcome_message_small
import ventago.composeapp.generated.resources.quotes_welcome_title
import ventago.composeapp.generated.resources.sales
import ventago.composeapp.generated.resources.expenses
import ventago.composeapp.generated.resources.home_base
import ventago.composeapp.generated.resources.home_daily_average
import ventago.composeapp.generated.resources.home_expenses_count
import ventago.composeapp.generated.resources.home_includes_itbms
import ventago.composeapp.generated.resources.home_itbms
import ventago.composeapp.generated.resources.home_month_expenses
import ventago.composeapp.generated.resources.home_month_sales
import ventago.composeapp.generated.resources.home_no_month_expenses
import ventago.composeapp.generated.resources.home_orders
import ventago.composeapp.generated.resources.home_range_15d
import ventago.composeapp.generated.resources.home_range_7d
import ventago.composeapp.generated.resources.home_range_month
import ventago.composeapp.generated.resources.home_range_year
import ventago.composeapp.generated.resources.home_today_sales
import ventago.composeapp.generated.resources.home_year_sales
import ventago.composeapp.generated.resources.yappy_logo
import ventago.composeapp.generated.resources.yappy_logo_portrait


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel<HomeViewModel>(),
    appViewModel: AppViewModel = koinViewModel<AppViewModel>(),
    navigate: (PosScreens) -> Unit
) {
    val platformState = rememberPlatformState()
    val storage: LocalStorage = koinInject()
    val flagsService: IFlagsService = koinInject()

    val uiState by viewModel.uiState.collectAsState()
    val flagsState by flagsService.flags().collectAsState()

    val appState = appViewModel.mainState.collectAsState()
    var showQuotesWelcomeSheet by remember { mutableStateOf(false) }
    val quotesWelcomeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Request notification permission when user successfully logs in and lands on home screen
    LaunchedEffect(Unit) {
        platformState.requestNotificationPermission()
    }

    if (uiState.isLoadingData) {
        HomeLoadingScreen()
        return
    }

    val hasEnteredQuotes = storage.bool(QuotesOnboarding.KEY_HAS_ENTERED_QUOTES) == true
    val hasShownQuotesWelcome = storage.bool(QuotesOnboarding.KEY_WELCOME_SHEET_SHOWN) == true

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

        if (flagsState.dgiDown) {
            DgiDownAlertBanner(
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
            )
        }

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
        }

        val salesData = uiState.salesChart
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

        HomeSalesRangeSelector(
            selectedRange = uiState.selectedRange,
            onRangeSelected = viewModel::setSalesRange,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
        )

        SalesLineGraphic(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            data = salesData,
            selectedIndex = uiState.selectedSalesIndex,
            chartHeight = 220.dp,
            onItemClick = viewModel::setSelectedSalesIndex
        )


        val actionRowPadding = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
        if (uiState.hasQuotesAccess) {
            Row(
                modifier = actionRowPadding.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.weight(1f).height(90.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    onClick = {
                        if (!hasShownQuotesWelcome) {
                            showQuotesWelcomeSheet = true
                            return@Card
                        }
                        storage.set(QuotesOnboarding.KEY_HAS_ENTERED_QUOTES, true)
                        com.teco.ventago.features.quotes.domain.QuoteSelectionStore.selected = null
                        com.teco.ventago.features.quotes.domain.QuoteSelectionStore.startQuoteFlow = true
                        navigate(PosScreens.POSScreen)
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Description,
                                contentDescription = "",
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.pos_new_quote),
                                style = bodyMediumBold(color = MaterialTheme.colorScheme.onPrimary)
                            )
                        }

                        QuotesFeatureBadge(
                            isNew = !hasEnteredQuotes,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f).height(90.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                    onClick = { navigate(PosScreens.POS) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.pos),
                            contentDescription = "",
                            modifier = Modifier.size(28.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSecondary),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.pos),
                            style = bodyMediumBold(color = MaterialTheme.colorScheme.onSecondary)
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = actionRowPadding.fillMaxWidth().height(90.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                onClick = { navigate(PosScreens.POS) }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(Res.drawable.pos),
                        contentDescription = "",
                        modifier = Modifier.size(28.dp),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSecondary),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.pos),
                        style = bodyMediumBold(color = MaterialTheme.colorScheme.onSecondary)
                    )
                }
            }
        }


        Row(
            modifier = Modifier
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.weight(1f).height(90.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                onClick = { navigate(PosScreens.CustomersManage) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.pos_clients),
                        style = bodyMediumBold(color = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f).height(90.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                onClick = { navigate(PosScreens.Expenses) }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.expenses),
                            style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(shimmeringSecondaryBrush())
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nuevo",
                            style = labelSmall(color = MaterialTheme.colorScheme.onSecondary),
                            maxLines = 1
                        )
                    }
                }
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
                        painter = painterResource(Res.drawable.yappy_logo_portrait),
                        contentDescription = "",
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                    ) {
                        Text(
                            text = "Recibe pagos con Yappy",
                            style = titleMediumBold().merge(
                                TextStyle(
                                    textAlign = TextAlign.Start
                                )
                            )
                        )
                        Text(
                            text = "Conecta tu cuenta Yappy y acepta pagos del método de pago más popular de Panamá. Los fondos llegan directamente a tu cuenta bancaria.",
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
            platformState.openEmailIntent("soporte@tecodigi.com")
        }

    }

    if (showQuotesWelcomeSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                storage.set(QuotesOnboarding.KEY_WELCOME_SHEET_SHOWN, true)
                showQuotesWelcomeSheet = false
            },
            sheetState = quotesWelcomeSheetState,
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.Redeem,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.quotes_welcome_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.quotes_welcome_message),
                    style = bodyMedium(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.quotes_welcome_message_small),
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    onClick = {
                        storage.set(QuotesOnboarding.KEY_WELCOME_SHEET_SHOWN, true)
                        storage.set(QuotesOnboarding.KEY_HAS_ENTERED_QUOTES, true)
                        showQuotesWelcomeSheet = false
                        com.teco.ventago.features.quotes.domain.QuoteSelectionStore.selected = null
                        com.teco.ventago.features.quotes.domain.QuoteSelectionStore.startQuoteFlow = true
                        navigate(PosScreens.POSScreen)
                    }
                ) {
                    Text(stringResource(Res.string.action_continue), style = bodyMediumBold(color = MaterialTheme.colorScheme.onSecondary))
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun HomeSalesRangeSelector(
    selectedRange: HomeSalesRange,
    onRangeSelected: (HomeSalesRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HomeSalesRange.values().forEach { range ->
            val selected = range == selectedRange
            SuggestionChip(
                onClick = { onRangeSelected(range) },
                label = {
                    Text(
                        text = when (range) {
                            HomeSalesRange.D7 -> stringResource(Res.string.home_range_7d)
                            HomeSalesRange.D15 -> stringResource(Res.string.home_range_15d)
                            HomeSalesRange.MONTH -> stringResource(Res.string.home_range_month)
                            HomeSalesRange.YEAR -> stringResource(Res.string.home_range_year)
                        },
                        style = labelSmall(
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            )
        }
    }
}

@Composable
private fun HomeSummaryCards(
    summary: HomeSummary,
    modifier: Modifier = Modifier
) {
    val dayOfMonth = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.dayOfMonth
    val averagePerDay = if (dayOfMonth > 0) summary.monthSalesTotalWithTaxes / dayOfMonth else 0.0
    val hasMonthExpenses = summary.monthExpenseTotal != 0.0 || summary.monthExpenseCount > 0
    val monthChangePositive = summary.monthSalesChangePerc >= 0.0

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(Res.string.home_year_sales),
                        style = bodyMediumBold(color = MaterialTheme.colorScheme.onSecondary)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = toMoney(summary.yearSalesTotal),
                        style = titleMediumBold(color = MaterialTheme.colorScheme.onSecondary)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.home_includes_itbms),
                        style = labelSmall(color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f))
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = cardContainerColor())
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.home_month_sales),
                        style = bodyMediumBold()
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (monthChangePositive) Color(0xFF2E7D32).copy(alpha = 0.15f)
                                else Color(0xFFD32F2F).copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (monthChangePositive) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                            contentDescription = null,
                            tint = if (monthChangePositive) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${abs(summary.monthSalesChangePerc).toOneDecimal()}%",
                            style = labelSmall(
                                color = if (monthChangePositive) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = toMoney(summary.monthSalesTotalWithTaxes),
                    style = titleMediumBold(color = MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${stringResource(Res.string.home_base)}: ${toMoney(summary.monthSalesTotal)}",
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${stringResource(Res.string.home_itbms)}: ${toMoney(summary.monthSalesTaxTotal)}",
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${stringResource(Res.string.home_orders)}: ${summary.monthOrderCount}",
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = vanishedBackgroundColor())
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(Res.string.home_today_sales),
                        style = bodyMediumBold()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = toMoney(summary.todaySalesTotal),
                        style = titleMediumBold(color = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${stringResource(Res.string.home_orders)}: ${summary.todayOrderCount}",
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${stringResource(Res.string.home_daily_average)}: ${toMoney(averagePerDay)}",
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = cardContainerColor())
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(Res.string.home_month_expenses),
                        style = bodyMediumBold()
                    )
                    Spacer(Modifier.height(8.dp))
                    if (hasMonthExpenses) {
                        Text(
                            text = toMoney(summary.monthExpenseTotal),
                            style = titleMediumBold(color = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${stringResource(Res.string.home_expenses_count)}: ${summary.monthExpenseCount}",
                            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.home_no_month_expenses),
                            style = bodyMedium()
                        )
                    }
                }
            }
        }
    }
}

private fun toMoney(value: Double): String {
    return formatNumberToMoney(value.toLongCents().toDecimalString())
}

private fun Double.toOneDecimal(): String {
    val text = (round(this * 10.0) / 10.0).toString()
    return if (text.contains('.')) {
        val parts = text.split('.')
        "${parts[0]}.${parts.getOrElse(1) { "0" }.take(1).padEnd(1, '0')}"
    } else {
        "$text.0"
    }
}

@Composable
private fun QuotesFeatureBadge(
    isNew: Boolean,
    modifier: Modifier = Modifier
) {
    val label = if (isNew) {
        stringResource(Res.string.quotes_badge_new)
    } else {
        stringResource(Res.string.quotes_badge_beta)
    }

    val shape = RoundedCornerShape(50)
    val container = if (isNew) {
        shimmeringSecondaryBrush()
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
            )
        )
    }
    val contentColor = if (isNew) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .clip(shape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = labelSmall(color = contentColor),
            maxLines = 1
        )
    }
}

@Composable
private fun shimmeringSecondaryBrush(): Brush {
    val base = MaterialTheme.colorScheme.secondary
    val highlight = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.35f)
    val transition = rememberInfiniteTransition(label = "quotesNewBadge")
    val x by transition.animateFloat(
        initialValue = 0f,
        targetValue = 220f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "quotesNewBadgeX"
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(x - 220f, 0f),
        end = Offset(x, 60f)
    )
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
