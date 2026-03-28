package com.teco.ventago.features.orders.ui.orders

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavOptionsBuilder
import com.teco.ventago.core.LocalStorage
import com.teco.ventago.core.camera.PermissionCallback
import com.teco.ventago.core.camera.PermissionStatus
import com.teco.ventago.core.camera.PermissionType
import com.teco.ventago.core.camera.createPermissionsManager
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.molecules.orders.OrderListItem
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.features.quotes.ui.list.QuotesListScreen
import com.teco.ventago.features.quotes.ui.list.QuotesListViewModel
import com.teco.ventago.features.orders.ui.orders.viewmodel.OrdersUiEvent
import com.teco.ventago.features.orders.ui.orders.viewmodel.OrdersViewModel
import com.teco.ventago.navigation.LocalNavController
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.BarcodeScannerScreen
import com.teco.ventago.utils.DateFormat.getFormattedDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.filter_all
import ventago.composeapp.generated.resources.filter_failed
import ventago.composeapp.generated.resources.filter_new
import ventago.composeapp.generated.resources.filter_processing
import ventago.composeapp.generated.resources.orders
import ventago.composeapp.generated.resources.orders_empty
import ventago.composeapp.generated.resources.quotes
import ventago.composeapp.generated.resources.see_more
import ventago.composeapp.generated.resources.tax_empty
import kotlinx.coroutines.delay

private const val KEY_ORDERS_QUOTES_TAB_HINT_SHOWN = "orders_quotes_tab_hint_shown"

@Composable
fun OrdersScreenActions(backStackEntry: NavBackStackEntry?) {
    val navController = LocalNavController.current
    val ordersOwner = remember(navController) {
        navController.getBackStackEntry(PosScreens.Orders.name)
    }
    val viewModel: OrdersViewModel = koinViewModel(viewModelStoreOwner = ordersOwner)

    val uiState by viewModel.uiState.collectAsState()
    var launchCamera by remember { mutableStateOf(value = false) }
    var launchSetting by remember { mutableStateOf(value = false) }

    val permissionsManager = createPermissionsManager(object : PermissionCallback {
        override fun onPermissionStatus(
            permissionType: PermissionType,
            status: PermissionStatus
        ) {
            when (status) {
                PermissionStatus.GRANTED -> {
                    when (permissionType) {
                        PermissionType.CAMERA -> viewModel.showScanner(true)
                        PermissionType.GALLERY -> {
                            // Not handled now
                        }
                    }
                }

                else -> {
                    viewModel.showPermissionRationalDialog(true)
                }
            }
        }
    })

    if (launchCamera) {
        if (permissionsManager.isPermissionGranted(PermissionType.CAMERA)) {
            viewModel.showScanner(!uiState.showScanner)
        } else {
            permissionsManager.askPermission(PermissionType.CAMERA)
        }
        launchCamera = false
    }

    if (launchSetting) {
        permissionsManager.launchSettings()
        launchSetting = false
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                OrdersUiEvent.LaunchSettings -> launchSetting = true
                else -> println("Event not handled here")
            }
        }
    }

    if (uiState.canCreateOrderEntry) {
        IconButton(onClick = {
            navController.navigate(PosScreens.POS.name)
        }) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "New order",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    IconButton(onClick = {
        launchCamera = true
    }) {
        Icon(
            imageVector = if(uiState.showScanner) Icons.Rounded.Close else Icons.Rounded.QrCodeScanner,
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel,
    navigate: (PosScreens, (NavOptionsBuilder.() -> Unit)?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val storage: LocalStorage = koinInject()
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    var showQuotesTabHint by remember { mutableStateOf(false) }

    val hintPulseTransition = rememberInfiniteTransition(label = "quotes_tab_hint")
    val hintPulse by hintPulseTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "quotes_tab_hint_pulse"
    )
//
    LaunchedEffect(uiState.hasQuotesAccess) {
        if (!uiState.hasQuotesAccess) {
            selectedTabIndex = 0
        }
    }

    LaunchedEffect(uiState.hasQuotesAccess) {
        if (uiState.hasQuotesAccess) {
            val shown = storage.bool(KEY_ORDERS_QUOTES_TAB_HINT_SHOWN) == true
            if (!shown) {
                showQuotesTabHint = true
                delay(4500)
                showQuotesTabHint = false
                storage.set(KEY_ORDERS_QUOTES_TAB_HINT_SHOWN, true)
            }
        } else {
            showQuotesTabHint = false
        }
    }

    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })
    val pullRefreshState = rememberPullRefreshState(uiState.refreshingOrder, { viewModel.refreshOrders() })
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                OrdersUiEvent.OpenOrderDetails -> navigate(PosScreens.OrderDetailsScreen, null)
                OrdersUiEvent.LoadingOrdersConnectionError -> Unit
                OrdersUiEvent.LoadingOrdersError -> Unit
                OrdersUiEvent.LaunchSettings -> Unit
            }
        }
    }

    if (uiState.showScanner) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            BarcodeScannerScreen(
                onResult = { cufe ->
                    if (cufe.isEmpty() || cufe.length < 10 || !cufe.contains("-")) {
                        viewModel.showScanner(false)
                        return@BarcodeScannerScreen
                    }

                    viewModel.findOrderByCUFE(cufe)
                    viewModel.showScanner(false)
                },
                onClose = { viewModel.showScanner(false) }
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        if (uiState.hasQuotesAccess) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.secondary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    selectedContentColor = MaterialTheme.colorScheme.secondary,
                    unselectedContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    text = { Text(text = stringResource(Res.string.orders)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        if (showQuotesTabHint) {
                            showQuotesTabHint = false
                            storage.set(KEY_ORDERS_QUOTES_TAB_HINT_SHOWN, true)
                        }
                    },
                    selectedContentColor = MaterialTheme.colorScheme.secondary,
                    unselectedContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = if (showQuotesTabHint) {
                                Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = hintPulse * 0.18f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            } else {
                                Modifier
                            }
                        ) {
                            Text(text = stringResource(Res.string.quotes))
                            if (showQuotesTabHint) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = hintPulse))
                                )
                            }
                        }
                    }
                )
            }
        }

        if (uiState.hasQuotesAccess && selectedTabIndex == 1) {
            val quotesViewModel: QuotesListViewModel = koinViewModel()
            QuotesListScreen(
                viewModel = quotesViewModel,
                navigate = { route -> navigate(route, null) },
                onBack = {}
            )
            return
        }

        if (uiState.isLoadingOrders && uiState.orders.isEmpty()) {
            LoadingOrdersView()
        } else if (uiState.orders.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(Res.drawable.tax_empty),
                    contentDescription = "",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                )

                Text(
                    text = stringResource(Res.string.orders_empty),
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = latoFontFamily(),
                        lineHeight = 20.sp,
                    )
                )
            }
        } else {
            Box(Modifier.pullRefresh(pullRefreshState)) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val dataGroup =
                        uiState.orders.groupBy { dat -> getFormattedDate(dat.createdAt, "yyyy-MM-dd'T'HH:mm:ss", "dd MMMM yyyy") }

                    dataGroup.forEach { (date, orders) ->
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp, top = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = date,
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        lineHeight = 20.sp,
                                        fontFamily = latoFontFamily(),
                                        fontWeight = FontWeight(400),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        letterSpacing = 0.08.sp,
                                    )
                                )
                            }
                        }

                        items(orders.size) { i ->
                            val order = orders[i]

                            OrderListItem(
                                order,
                                onClick = {
                                    viewModel.selectOrder(order)
                                    navigate(PosScreens.OrderDetailsScreen, null)
                                },
                                statusOnClick = {
                                }
                            )
                        }
                    }

                    item {
                        if (!uiState.isLoadingOrders && uiState.orders.isNotEmpty() && !uiState.noMoreOrders) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TextButtonS(
                                    label = stringResource(Res.string.see_more),
                                    onClick = {
                                        viewModel.loadOrders()
                                    })
                            }
                        } else if (uiState.isLoadingOrders && uiState.orders.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }

                PullRefreshIndicator(
                    uiState.refreshingOrder,
                    pullRefreshState,
                    Modifier.align(Alignment.TopCenter)
                )
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

        DMAlertDialog(
            title = "Permiso requerido",
            message = "Para acceder a la cámara, otorgue este permiso. Puedes administrar los permisos en la configuración de tu dispositivo.",
            confirmText = "Settings",
            dismissText = "Cancel",
            onConfirm = {
                viewModel.showPermissionRationalDialog(false)
                viewModel.launchSettings()
            },
            onDismiss = {
                viewModel.showPermissionRationalDialog(false)
            },
            show = uiState.showPermissionRationalDialog
        )
    }
}

@Composable
fun LoadingOrdersView() {
    val brush = shimmerBrush()
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        repeat(5) {
            item {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(top = 8.dp)
                        .clip(shape = RoundedCornerShape(6.dp))
                        .background(brush = brush)
                )
            }
        }
    }
}
