package com.teco.ventago.features.notifications.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.notifications.domain.models.InAppNotification
import com.teco.ventago.features.notifications.domain.models.NotificationDerivedState
import com.teco.ventago.features.notifications.domain.models.derivedState
import com.teco.ventago.features.notifications.ui.viewmodel.NotificationsUiEvent
import com.teco.ventago.features.notifications.ui.viewmodel.NotificationsViewModel
import com.teco.ventago.navigation.AchPaymentDetailsRoute
import com.teco.ventago.utils.openCustomTab
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.notifications_action_unavailable
import ventago.composeapp.generated.resources.notifications_dismiss_all
import ventago.composeapp.generated.resources.notifications_empty
import ventago.composeapp.generated.resources.notifications_filter_all
import ventago.composeapp.generated.resources.notifications_filter_empty
import ventago.composeapp.generated.resources.notifications_filter_important
import ventago.composeapp.generated.resources.notifications_filter_transactions
import ventago.composeapp.generated.resources.notifications_filter_unread
import ventago.composeapp.generated.resources.notifications_load_more
import ventago.composeapp.generated.resources.notifications_mark_all_read
import ventago.composeapp.generated.resources.notifications_section_today
import ventago.composeapp.generated.resources.notifications_section_undated
import ventago.composeapp.generated.resources.notifications_section_yesterday

private enum class NotificationFilter {
    ALL,
    UNREAD,
    IMPORTANT,
    TRANSACTIONS,
}

private data class NotificationFilterCounts(
    val all: Int,
    val unread: Int,
    val important: Int,
    val transactions: Int,
)

private data class NotificationSection(
    val key: String,
    val label: String,
    val items: List<InAppNotification>,
)

private const val FULL_SWIPE_DISMISS_THRESHOLD_FRACTION = 0.95f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = koinViewModel<NotificationsViewModel>(),
    navigateAny: (Any) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })
    val snackbarHostState = remember { SnackbarHostState() }
    val actionUnavailableMessage = stringResource(Res.string.notifications_action_unavailable)
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = viewModel::refreshNotifications,
    )

    var selectedFilter by rememberSaveable { mutableStateOf(NotificationFilter.ALL.name) }
    val selectedFilterType = remember(selectedFilter) {
        runCatching { NotificationFilter.valueOf(selectedFilter) }.getOrElse { NotificationFilter.ALL }
    }

    val filterCounts = remember(uiState.visibleItems) {
        NotificationFilterCounts(
            all = uiState.visibleItems.size,
            unread = uiState.visibleItems.count { it.derivedState() == NotificationDerivedState.UNREAD },
            important = uiState.visibleItems.count { it.isImportantNotification() },
            transactions = uiState.visibleItems.count { it.isTransactionNotification() },
        )
    }

    val filteredItems = remember(uiState.visibleItems, selectedFilterType) {
        uiState.visibleItems.filter { notification ->
            when (selectedFilterType) {
                NotificationFilter.ALL -> true
                NotificationFilter.UNREAD -> notification.derivedState() == NotificationDerivedState.UNREAD
                NotificationFilter.IMPORTANT -> notification.isImportantNotification()
                NotificationFilter.TRANSACTIONS -> notification.isTransactionNotification()
            }
        }
    }

    val todayLabel = stringResource(Res.string.notifications_section_today)
    val yesterdayLabel = stringResource(Res.string.notifications_section_yesterday)
    val undatedLabel = stringResource(Res.string.notifications_section_undated)

    val sections = remember(filteredItems, todayLabel, yesterdayLabel, undatedLabel) {
        buildNotificationSections(
            items = filteredItems,
            todayLabel = todayLabel,
            yesterdayLabel = yesterdayLabel,
            undatedLabel = undatedLabel,
        )
    }

    LaunchedEffect(Unit) {
        viewModel.onScreenVisible()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NotificationsUiEvent.NavigateToAchPayment -> {
                    navigateAny(AchPaymentDetailsRoute(paymentUid = event.paymentUid))
                }

                is NotificationsUiEvent.OpenExternalUrl -> openCustomTab(event.url)
                NotificationsUiEvent.ActionNotAvailable -> {
                    snackbarHostState.showSnackbar(actionUnavailableMessage)
                }
            }
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearSnackbarMessage()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isInitialLoading -> NotificationsShimmerList()
            uiState.visibleItems.isEmpty() && !uiState.isRefreshing ->
                NotificationsEmptyState(modifier = Modifier.align(Alignment.Center))
            else -> NotificationsContent(
                sections = sections,
                hasFilteredResults = filteredItems.isNotEmpty(),
                filterCounts = filterCounts,
                selectedFilter = selectedFilterType,
                onFilterSelected = { selectedFilter = it.name },
                onMarkAllRead = viewModel::markAllLoadedAsRead,
                onDismissAll = viewModel::dismissAllLoaded,
                onLoadMore = viewModel::loadMore,
                onDismiss = viewModel::dismissNotification,
                onTap = viewModel::onNotificationTapped,
                hasUnread = filterCounts.unread > 0,
                hasMore = uiState.hasMore,
                isRefreshing = uiState.isRefreshing,
                isLoadingMore = uiState.isLoadingMore,
                pullRefreshState = pullRefreshState,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )

        if (uiState.loadingBottomSheet.isLoading()) {
            LoadingSheet(
                state = uiState.loadingBottomSheet,
                sheetState = loadingSheetState,
            ) {
                viewModel.hideLoading()
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun NotificationsContent(
    sections: List<NotificationSection>,
    hasFilteredResults: Boolean,
    filterCounts: NotificationFilterCounts,
    selectedFilter: NotificationFilter,
    onFilterSelected: (NotificationFilter) -> Unit,
    onMarkAllRead: () -> Unit,
    onDismissAll: () -> Unit,
    onLoadMore: () -> Unit,
    onDismiss: (Long) -> Unit,
    onTap: (InAppNotification) -> Unit,
    hasUnread: Boolean,
    hasMore: Boolean,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    pullRefreshState: androidx.compose.material.pullrefresh.PullRefreshState,
) {
    val allFilterLabel = stringResource(Res.string.notifications_filter_all)
    val unreadFilterLabel = stringResource(Res.string.notifications_filter_unread)
    val importantFilterLabel = stringResource(Res.string.notifications_filter_important)
    val transactionsFilterLabel = stringResource(Res.string.notifications_filter_transactions)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        NotificationFilterChip(
                            label = allFilterLabel,
                            count = filterCounts.all,
                            selected = selectedFilter == NotificationFilter.ALL,
                            onClick = { onFilterSelected(NotificationFilter.ALL) },
                        )
                    }
                    item {
                        NotificationFilterChip(
                            label = unreadFilterLabel,
                            count = filterCounts.unread,
                            selected = selectedFilter == NotificationFilter.UNREAD,
                            onClick = { onFilterSelected(NotificationFilter.UNREAD) },
                        )
                    }
                    item {
                        NotificationFilterChip(
                            label = importantFilterLabel,
                            count = filterCounts.important,
                            selected = selectedFilter == NotificationFilter.IMPORTANT,
                            onClick = { onFilterSelected(NotificationFilter.IMPORTANT) },
                        )
                    }
                    item {
                        NotificationFilterChip(
                            label = transactionsFilterLabel,
                            count = filterCounts.transactions,
                            selected = selectedFilter == NotificationFilter.TRANSACTIONS,
                            onClick = { onFilterSelected(NotificationFilter.TRANSACTIONS) },
                        )
                    }
                }
            }

            if (hasUnread) {
                item {
                    MarkAllReadCard(onClick = onMarkAllRead)
                }
            }

            if (!hasFilteredResults) {
                item {
                    Text(
                        text = stringResource(Res.string.notifications_filter_empty),
                        style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }
            }

            sections.forEach { section ->
                item(key = "section-${section.key}") {
                    Text(
                        text = section.label,
                        style = titleMediumBold(),
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                items(section.items, key = { it.id }) { notification ->
                    NotificationRow(
                        notification = notification,
                        onDismiss = { onDismiss(notification.id) },
                        onClick = { onTap(notification) }
                    )
                }
            }

            item {
                when {
                    isLoadingMore -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    hasMore -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButtonS(
                                label = stringResource(Res.string.notifications_load_more),
                                color = MaterialTheme.colorScheme.secondary,
                                onClick = onLoadMore,
                            )
                        }
                    }
                }
            }

            item {
                OutlinedButtonM(
                    onClick = onDismissAll,
                    modifier = Modifier.fillMaxWidth(),
                    contentColor = MaterialTheme.colorScheme.secondary,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                ) {
                    Text(
                        text = stringResource(Res.string.notifications_dismiss_all),
                        style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary)
                    )
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun NotificationRow(
    notification: InAppNotification,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
) {
    val kindMeta = notificationKindMeta(notification.kind)
    val state = notification.derivedState()
    val timestampLabel = notificationTimeLabel(notification.createdAt)
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance ->
            totalDistance * FULL_SWIPE_DISMISS_THRESHOLD_FRACTION
        },
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                return@rememberSwipeToDismissBoxState false
            }
            value != SwipeToDismissBoxValue.StartToEnd
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val swipeActive = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = if (swipeActive) 0.22f else 0.14f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = if (swipeActive) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(kindMeta.containerColor, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(kindMeta.accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = kindMeta.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = notification.title,
                            style = if (state == NotificationDerivedState.UNREAD) bodyMediumBold() else bodyMedium(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timestampLabel,
                            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        if (state == NotificationDerivedState.UNREAD) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = notification.message,
                        style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!notification.status.isNullOrBlank() || !notification.priority.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (!notification.status.isNullOrBlank()) {
                                MetaChip(label = notification.status)
                            }
                            if (!notification.priority.isNullOrBlank()) {
                                PriorityChip(priority = notification.priority)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun MarkAllReadCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(Res.string.notifications_mark_all_read),
                style = bodyMediumBold()
            )
        }
    }
}

@Composable
private fun NotificationFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val selectedContainer = MaterialTheme.colorScheme.secondary
    val selectedLabel = MaterialTheme.colorScheme.onSecondary
    val unselectedContainer = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val unselectedLabel = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(100.dp),
        color = if (selected) selectedContainer else unselectedContainer,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                style = bodyMedium(color = if (selected) selectedLabel else unselectedLabel),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .background(
                        color = if (selected) selectedLabel.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f),
                        shape = CircleShape
                    )
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    style = labelSmall(
                        color = if (selected) selectedLabel else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun NotificationsShimmerList() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .weight(1f)
                            .background(shimmerBrush(), RoundedCornerShape(100.dp))
                    )
                }
            }
        }

        items(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
                    .background(shimmerBrush(), RoundedCornerShape(16.dp))
            )
        }
    }
}

@Composable
private fun NotificationsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.notifications_empty),
            style = titleMediumBold(),
        )
    }
}

@Composable
private fun MetaChip(label: String) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@Composable
private fun PriorityChip(priority: String) {
    val color = when (priority.lowercase()) {
        "critical", "high", "urgent" -> Color(0xFFD32F2F)
        "medium" -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.secondary
    }
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(100.dp),
    ) {
        Text(
            text = priority,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = labelSmall(color = color).copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

private data class NotificationKindMeta(
    val icon: ImageVector,
    val accentColor: Color,
    val containerColor: Color,
)

@Composable
private fun notificationKindMeta(kind: String): NotificationKindMeta {
    return when (kind) {
        "payment.success" -> NotificationKindMeta(
            icon = Icons.Rounded.CheckCircle,
            accentColor = Color(0xFF2E7D32),
            containerColor = Color(0xFFE5F4EA)
        )

        "payment.cancelled" -> NotificationKindMeta(
            icon = Icons.Rounded.Cancel,
            accentColor = Color(0xFFE67E22),
            containerColor = Color(0xFFFFF0E2)
        )

        "payment.failed" -> NotificationKindMeta(
            icon = Icons.Rounded.Close,
            accentColor = Color(0xFFD32F2F),
            containerColor = Color(0xFFFFEBEE)
        )

        "payment.expired" -> NotificationKindMeta(
            icon = Icons.Rounded.AccessTime,
            accentColor = Color(0xFF546E7A),
            containerColor = Color(0xFFECEFF1)
        )

        "ach.proof_uploaded" -> NotificationKindMeta(
            icon = Icons.Rounded.Description,
            accentColor = Color(0xFF1E88E5),
            containerColor = Color(0xFFE8F2FE)
        )

        "ach.decision" -> NotificationKindMeta(
            icon = Icons.Rounded.CheckCircle,
            accentColor = MaterialTheme.colorScheme.secondary,
            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
        )

        "ach.decision.approved" -> NotificationKindMeta(
            icon = Icons.Rounded.CheckCircle,
            accentColor = Color(0xFF2E7D32),
            containerColor = Color(0xFFE5F4EA)
        )

        "ach.decision.rejected" -> NotificationKindMeta(
            icon = Icons.Rounded.Close,
            accentColor = Color(0xFFD32F2F),
            containerColor = Color(0xFFFFEBEE)
        )

        "ach.fraud" -> NotificationKindMeta(
            icon = Icons.Rounded.Warning,
            accentColor = Color(0xFFEF6C00),
            containerColor = Color(0xFFFFF3E0)
        )

        else -> NotificationKindMeta(
            icon = Icons.Rounded.Notifications,
            accentColor = MaterialTheme.colorScheme.secondary,
            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
        )
    }
}

private fun buildNotificationSections(
    items: List<InAppNotification>,
    todayLabel: String,
    yesterdayLabel: String,
    undatedLabel: String,
): List<NotificationSection> {
    if (items.isEmpty()) return emptyList()

    val timeZone = TimeZone.currentSystemDefault()
    val currentDate = Clock.System.now().toLocalDateTime(timeZone).date
    val yesterdayDate = currentDate.minus(1, DateTimeUnit.DAY)

    val sortedItems = items.sortedByDescending {
        parseNotificationInstant(it.createdAt)?.toEpochMilliseconds() ?: Long.MIN_VALUE
    }

    val groupedByDate = linkedMapOf<String, MutableList<InAppNotification>>()
    sortedItems.forEach { notification ->
        val localDate = parseNotificationLocalDateTime(notification.createdAt, timeZone)?.date
        val key = localDate?.toString() ?: "undated"
        groupedByDate.getOrPut(key) { mutableListOf() }.add(notification)
    }

    return groupedByDate.map { (key, sectionItems) ->
        val date = if (key == "undated") null else parseNotificationLocalDate(key)
        val label = when {
            date == null -> undatedLabel
            date == currentDate -> todayLabel
            date == yesterdayDate -> yesterdayLabel
            else -> formatSectionDate(date)
        }
        NotificationSection(key = key, label = label, items = sectionItems)
    }
}

private fun parseNotificationInstant(value: String): Instant? {
    return runCatching { Instant.parse(value) }.getOrNull()
}

private fun parseNotificationLocalDate(value: String): LocalDate? {
    return runCatching { LocalDate.parse(value) }.getOrNull()
}

private fun parseNotificationLocalDateTime(
    value: String,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): LocalDateTime? {
    return parseNotificationInstant(value)?.toLocalDateTime(timeZone)
}

private fun notificationTimeLabel(value: String): String {
    val timeZone = TimeZone.currentSystemDefault()
    val dateTime = parseNotificationLocalDateTime(value, timeZone) ?: return value

    val hour = dateTime.time.hour.toString().padStart(2, '0')
    val minute = dateTime.time.minute.toString().padStart(2, '0')
    return "$hour:$minute"
}

private fun formatSectionDate(date: LocalDate): String {
    val day = date.dayOfMonth.toString().padStart(2, '0')
    val month = date.monthNumber.toString().padStart(2, '0')
    val year = date.year
    return "$day/$month/$year"
}

private fun InAppNotification.isTransactionNotification(): Boolean {
    return kind.startsWith("payment.") || kind.startsWith("ach.")
}

private fun InAppNotification.isImportantNotification(): Boolean {
    val normalizedPriority = priority?.trim()?.lowercase().orEmpty()
    val normalizedStatus = status?.trim()?.lowercase().orEmpty()
    val isPriorityImportant = normalizedPriority in setOf("critical", "high", "urgent")
    val isKindImportant = kind in setOf(
        "payment.failed",
        "payment.cancelled",
        "payment.expired",
        "ach.fraud",
        "ach.decision.rejected",
    )
    val isStatusImportant = normalizedStatus.contains("atenci") ||
        normalizedStatus.contains("attention") ||
        normalizedStatus.contains("fraud") ||
        normalizedStatus.contains("revis")

    return isPriorityImportant || isKindImportant || isStatusImportant
}
