package com.teco.ventago.features.home.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.design_system.organism.SalesLineGraphic
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.home.domain.model.HomeSalesRange
import com.teco.ventago.features.home.domain.model.HomeSummary
import com.teco.ventago.features.home.domain.model.TopCustomer
import com.teco.ventago.features.home.ui.viewmodel.HomeViewModel
import com.teco.ventago.isTablet
import com.teco.ventago.navigation.ExpensesListScreenRoute
import com.teco.ventago.navigation.LocalNavController
import com.teco.ventago.navigation.OrdersScreenRoute
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.openWhatsappMessage
import com.teco.ventago.utils.toDecimalString
import com.teco.ventago.utils.toLongCents
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.home_base
import ventago.composeapp.generated.resources.home_daily_average
import ventago.composeapp.generated.resources.home_includes_itbms
import ventago.composeapp.generated.resources.home_itbms
import ventago.composeapp.generated.resources.home_month_expenses
import ventago.composeapp.generated.resources.home_month_sales
import ventago.composeapp.generated.resources.home_no_month_expenses
import ventago.composeapp.generated.resources.home_range_15d
import ventago.composeapp.generated.resources.home_range_7d
import ventago.composeapp.generated.resources.home_range_month
import ventago.composeapp.generated.resources.home_range_year
import ventago.composeapp.generated.resources.home_summary_empty
import ventago.composeapp.generated.resources.home_summary_no_top_clients
import ventago.composeapp.generated.resources.home_summary_orders_word
import ventago.composeapp.generated.resources.home_summary_overdue
import ventago.composeapp.generated.resources.home_summary_payable
import ventago.composeapp.generated.resources.home_summary_receivable
import ventago.composeapp.generated.resources.home_summary_records_word
import ventago.composeapp.generated.resources.home_summary_top_clients
import ventago.composeapp.generated.resources.home_summary_view_details
import ventago.composeapp.generated.resources.home_summary_vs_previous_month
import ventago.composeapp.generated.resources.home_today_sales
import ventago.composeapp.generated.resources.home_year_sales
import ventago.composeapp.generated.resources.sales

@Composable
fun HomeSummaryScreen(
    viewModel: HomeViewModel = koinViewModel<HomeViewModel>(),
    navigate: (PosScreens) -> Unit
) {
    val navController = LocalNavController.current
    val uiState by viewModel.uiState.collectAsState()
    val isTabletDevice = isTablet()

    if (uiState.isLoadingData) {
        HomeLoadingScreen()
        return
    }

    val summary = uiState.homeSummary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (summary == null && !uiState.isSummaryLoading) {
            EmptySummaryCard(
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (summary != null) {
            if (isTabletDevice) {
                TabletSalesCards(summary = summary)
            } else {
                MobileSalesCards(summary = summary)
            }
        }

        if (uiState.invoicingEnabled && uiState.invoicingPlanState != null) {
            InvoicingPlanCard(
                initialQuota = uiState.invoicingPlanState?.totalDtes ?: 0,
                remainingQuota = uiState.invoicingPlanState?.availableDtes ?: 0,
                activationDate = uiState.invoicingPlanState?.activationDate ?: "-",
                expirationDate = uiState.invoicingPlanState?.expirationDate ?: "-",
                onSeeInvoices = { navigate(PosScreens.Orders) },
                onBuyStamps = {
                    openWhatsappMessage("50763879477", "Hola, quiero renovar mi plan de folios.")
                }
            )
        }

        SummaryChartSection(
            selectedRange = uiState.selectedRange,
            salesChart = uiState.salesChart,
            selectedSalesIndex = uiState.selectedSalesIndex,
            onRangeSelected = viewModel::setSalesRange,
            onItemClick = viewModel::setSelectedSalesIndex,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (summary != null) {
            if (isTabletDevice) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReceivableCard(
                        summary = summary,
                        onViewDetails = {
                            navController.navigate(OrdersScreenRoute(paymentStatus = 0))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PayableCard(
                        summary = summary,
                        onViewDetails = {
                            navController.navigate(ExpensesListScreenRoute(initialPaymentStatus = "not_paid"))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                ReceivableCard(
                    summary = summary,
                    onViewDetails = {
                        navController.navigate(OrdersScreenRoute(paymentStatus = 0))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                PayableCard(
                    summary = summary,
                    onViewDetails = {
                        navController.navigate(ExpensesListScreenRoute(initialPaymentStatus = "not_paid"))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            TopCustomersCard(
                customers = summary.topCustomers,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun EmptySummaryCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Text(
            text = stringResource(Res.string.home_summary_empty),
            modifier = Modifier.padding(16.dp),
            style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@Composable
private fun TabletSalesCards(summary: HomeSummary) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            YearSalesCard(summary = summary, modifier = Modifier.weight(1f))
            MonthSalesCard(summary = summary, modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TodaySalesCard(summary = summary, modifier = Modifier.weight(1f))
            MonthExpensesCard(summary = summary, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MobileSalesCards(summary: HomeSummary) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        YearSalesCard(summary = summary)
        MonthSalesCard(summary = summary)
        TodaySalesCard(summary = summary)
        MonthExpensesCard(summary = summary)
    }
}

@Composable
private fun YearSalesCard(summary: HomeSummary, modifier: Modifier = Modifier) {
    val cardShape = RoundedCornerShape(18.dp)
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF6F7CFF),
            Color(0xFF8B95F8)
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(Res.string.home_year_sales),
                style = bodyMediumBold(color = MaterialTheme.colorScheme.onPrimary)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = toMoney(summary.yearSalesTotal),
                style = titleMediumBold(color = MaterialTheme.colorScheme.onPrimary)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.home_includes_itbms),
                style = labelSmall(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
            )
        }
    }
}

@Composable
private fun MonthSalesCard(summary: HomeSummary, modifier: Modifier = Modifier) {
    val monthChangePositive = summary.monthSalesChangePerc >= 0.0
    val trendColor = if (monthChangePositive) Color(0xFF2E7D32) else Color(0xFFD32F2F)
    val trendBackground = if (monthChangePositive) {
        Color(0xFF2E7D32).copy(alpha = 0.12f)
    } else {
        Color(0xFFD32F2F).copy(alpha = 0.12f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = stringResource(Res.string.home_month_sales), style = bodyMediumBold())
            Spacer(Modifier.height(8.dp))
            Text(text = toMoney(summary.monthSalesTotalWithTaxes), style = titleMediumBold())
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${stringResource(Res.string.home_base)}: ${toMoney(summary.monthSalesTotal)} · ${
                    stringResource(
                        Res.string.home_itbms
                    )
                }: ${toMoney(summary.monthSalesTaxTotal)}",
                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(trendBackground)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (monthChangePositive) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = "${abs(summary.monthSalesChangePerc).toOneDecimal()}%",
                        style = labelSmall(color = trendColor)
                    )
                }
                Spacer(Modifier.size(6.dp))
                Text(
                    text = stringResource(Res.string.home_summary_vs_previous_month),
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${summary.monthOrderCount} ${stringResource(Res.string.home_summary_orders_word)}",
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun TodaySalesCard(summary: HomeSummary, modifier: Modifier = Modifier) {
    val dayOfMonth = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.dayOfMonth
    val averagePerDay = if (dayOfMonth > 0) summary.monthSalesTotalWithTaxes / dayOfMonth else 0.0

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = vanishedBackgroundColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = stringResource(Res.string.home_today_sales), style = bodyMediumBold())
            Spacer(Modifier.height(8.dp))
            Text(text = toMoney(summary.todaySalesTotal), style = titleMediumBold())
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(Res.string.home_daily_average)}: ${toMoney(averagePerDay)}",
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${summary.todayOrderCount} ${stringResource(Res.string.home_summary_orders_word)}",
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun MonthExpensesCard(summary: HomeSummary, modifier: Modifier = Modifier) {
    val hasMonthExpenses = summary.monthExpenseTotal != 0.0 || summary.monthExpenseCount > 0

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = stringResource(Res.string.home_month_expenses), style = bodyMediumBold())
            Spacer(Modifier.height(8.dp))
            if (hasMonthExpenses) {
                Text(text = toMoney(summary.monthExpenseTotal), style = titleMediumBold())
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${summary.monthExpenseCount} ${stringResource(Res.string.home_summary_records_word)}",
                        style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            } else {
                Text(text = stringResource(Res.string.home_no_month_expenses), style = bodyMedium())
            }
        }
    }
}

@Composable
private fun SummaryChartSection(
    selectedRange: HomeSalesRange,
    salesChart: List<Pair<String, Double>>,
    selectedSalesIndex: Int,
    onRangeSelected: (HomeSalesRange) -> Unit,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.sales),
            style = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = latoFontFamily(),
                letterSpacing = 0.15.sp
            )
        )
        HomeSummaryRangeSelector(
            selectedRange = selectedRange,
            onRangeSelected = onRangeSelected,
            modifier = Modifier.padding(top = 12.dp)
        )
        SalesLineGraphic(
            modifier = Modifier.padding(top = 12.dp),
            data = salesChart,
            selectedIndex = selectedSalesIndex,
            chartHeight = 220.dp,
            onItemClick = onItemClick
        )
    }
}

@Composable
private fun HomeSummaryRangeSelector(
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
private fun ReceivableCard(
    summary: HomeSummary,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalAmount = summary.receivablePendingAmount
    val overdueAmount = summary.receivableOverdueAmount
    val nonOverduePendingAmount = (totalAmount - overdueAmount).coerceAtLeast(0.0)
    SummaryDebtCard(
        title = stringResource(Res.string.home_summary_receivable),
        totalAmount = totalAmount,
        totalCount = summary.receivablePendingCount,
        overdueAmount = overdueAmount,
        pendingAmount = nonOverduePendingAmount,
        countWord = stringResource(Res.string.home_summary_orders_word),
        onViewDetails = onViewDetails,
        modifier = modifier
    )
}

@Composable
private fun PayableCard(
    summary: HomeSummary,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalAmount = summary.payablePendingAmount
    val overdueAmount = summary.payableOverdueAmount
    val nonOverduePendingAmount = (totalAmount - overdueAmount).coerceAtLeast(0.0)
    SummaryDebtCard(
        title = stringResource(Res.string.home_summary_payable),
        totalAmount = totalAmount,
        totalCount = summary.payablePendingCount,
        overdueAmount = overdueAmount,
        pendingAmount = nonOverduePendingAmount,
        countWord = stringResource(Res.string.home_summary_records_word),
        onViewDetails = onViewDetails,
        modifier = modifier
    )
}

@Composable
private fun SummaryDebtCard(
    title: String,
    totalAmount: Double,
    totalCount: Int,
    overdueAmount: Double,
    pendingAmount: Double,
    countWord: String,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safePending = pendingAmount.coerceAtLeast(0.0)
    val safeOverdue = overdueAmount.coerceAtLeast(0.0)
    val safeTotal = (safePending + safeOverdue).coerceAtLeast(0.0)
    val overdueRatio = if (safeTotal > 0.0) safeOverdue / safeTotal else 0.0
    val pendingRatio = if (safeTotal > 0.0) safePending / safeTotal else 0.0

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = bodyMediumBold(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = toMoney(totalAmount), style = titleMediumBold())
                    Text(
                        text = "$totalCount $countWord",
                        style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = stringResource(Res.string.home_summary_view_details),
                        style = bodyMediumBold(color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .align(Alignment.Start)
                            .clickable(onClick = onViewDetails)
                    )
                }

                SummaryDonutChart(
                    overdueRatio = overdueRatio.toFloat(),
                    pendingRatio = pendingRatio.toFloat(),
                    modifier = Modifier.size(132.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryDonutChart(
    overdueRatio: Float,
    pendingRatio: Float,
    modifier: Modifier = Modifier
) {
    val safeOverdueRatio = overdueRatio.coerceIn(0f, 1f)
    val safePendingRatio = pendingRatio.coerceIn(0f, 1f)
    val overduePercent = (safeOverdueRatio * 100f).roundToInt()

    val pendingColor = Color(0xFF17C59A)
    val overdueColor = Color(0xFFF65B83)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Butt)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )

            if (safePendingRatio > 0f) {
                drawArc(
                    color = pendingColor,
                    startAngle = -90f,
                    sweepAngle = 360f * safePendingRatio,
                    useCenter = false,
                    style = stroke
                )
            }

            if (safeOverdueRatio > 0f) {
                drawArc(
                    color = overdueColor,
                    startAngle = -90f + (360f * safePendingRatio),
                    sweepAngle = 360f * safeOverdueRatio,
                    useCenter = false,
                    style = stroke
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.home_summary_overdue),
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Text(
                text = "$overduePercent%",
                style = titleMediumBold(color = MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}

@Composable
private fun TopCustomersCard(
    customers: List<TopCustomer>,
    modifier: Modifier = Modifier
) {
    val topCustomers = remember(customers) {
        customers.sortedByDescending { it.total }.take(5)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = stringResource(Res.string.home_summary_top_clients), style = bodyMediumBold())
            if (topCustomers.isEmpty()) {
                Text(
                    text = stringResource(Res.string.home_summary_no_top_clients),
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                return@Column
            }

            topCustomers.forEachIndexed { index, customer ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = customer.name.ifBlank { "-" },
                            style = bodyMediumBold()
                        )
                        Text(
                            text = "${customer.orderCount} ${stringResource(Res.string.home_summary_orders_word)}",
                            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Text(
                        text = toMoney(customer.total),
                        style = bodyMediumBold(color = MaterialTheme.colorScheme.primary)
                    )
                }
                if (index < topCustomers.lastIndex) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    )
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
