package com.teco.ventago.features.menu.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.teco.ventago.AppDistribution
import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.core.authz.RouteKey
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.quotes.domain.QuoteSelectionStore
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.getImageRequest
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.pos

private const val H10_POS_IMAGE_URL = "https://ventago.b-cdn.net/app/h10pos.png"

private sealed class QuickActionIcon {
    data class Vector(val imageVector: ImageVector) : QuickActionIcon()
    data class Drawable(val resource: DrawableResource) : QuickActionIcon()
}

private sealed class ModuleIcon {
    data class Vector(val imageVector: ImageVector) : ModuleIcon()
    data class Url(val url: String) : ModuleIcon()
}

private data class MenuEntry(
    val label: String,
    val icon: QuickActionIcon,
    val destination: PosScreens,
    val routeKey: RouteKey,
    val beforeNavigate: (() -> Unit)? = null,
)

private data class ModuleMenuItem(
    val label: String,
    val icon: ModuleIcon,
    val destination: PosScreens,
    val routeKey: RouteKey,
)

@Composable
fun MenuScreen(
    navigate: (PosScreens) -> Unit
) {
    val authService = koinInject<IAuthService>()
    val betaService = koinInject<BetaService>()
    val financialProfileService = koinInject<FinancialProfileService>()
    val appDistribution = koinInject<AppDistribution>()
    val currentUser by authService.getUser().collectAsState(initial = null)
    val betaResponse by betaService.features().collectAsState()
    val financialProfile by financialProfileService.observe().collectAsState(initial = null)
    val betaSnapshot = remember(betaResponse) {
        betaResponse?.features.orEmpty()
            .mapNotNull(BetaFeature::fromKey)
            .toSet()
    }
    val canShowPaymentsModule = financialProfile?.paymentSummary?.moduleAccess?.hasAccess() == true
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val canCreateQuote = AuthzEvaluator.canRoute(RouteKey.QUOTE_NEW, currentUser, betaSnapshot)
    val quoteOrCustomerQuickAction = if (canCreateQuote) {
        MenuEntry(
            label = "Nueva cotización",
            icon = QuickActionIcon.Vector(Icons.Rounded.Description),
            destination = PosScreens.POSScreen,
            routeKey = RouteKey.QUOTE_NEW,
            beforeNavigate = {
                QuoteSelectionStore.selected = null
                QuoteSelectionStore.startQuoteFlow = true
                QuoteSelectionStore.startOrderFlowFromQuote = false
            }
        )
    } else {
        MenuEntry(
            label = "Agregar cliente",
            icon = QuickActionIcon.Vector(Icons.Rounded.Person),
            destination = PosScreens.CustomerCreateScreen,
            routeKey = RouteKey.CUSTOMER_FORM
        )
    }

    val quickActions = buildList {
        add(
            MenuEntry(
                label = "Nueva venta",
                icon = QuickActionIcon.Drawable(Res.drawable.pos),
                destination = PosScreens.POS,
                routeKey = RouteKey.ORDERS_NEW
            )
        )
        add(quoteOrCustomerQuickAction)
        add(
            MenuEntry(
                label = "Registrar gasto",
                icon = QuickActionIcon.Vector(Icons.Rounded.Receipt),
                destination = PosScreens.NewExpenseScreen,
                routeKey = RouteKey.EXPENSE_NEW
            )
        )
        add(
            MenuEntry(
                label = "Agregar producto",
                icon = QuickActionIcon.Vector(Icons.Rounded.Inventory2),
                destination = PosScreens.AddItemScreen,
                routeKey = RouteKey.PRODUCT_ADD
            )
        )
    }.filter { item ->
        AuthzEvaluator.canRoute(item.routeKey, currentUser, betaSnapshot)
    }

    val modules = buildList {
        add(
            ModuleMenuItem(
                label = "Productos/servicios",
                icon = ModuleIcon.Vector(Icons.Rounded.Inventory2),
                destination = PosScreens.ProductsManage,
                routeKey = RouteKey.PRODUCTS_LIST
            )
        )
        add(
            ModuleMenuItem(
                label = "Clientes",
                icon = ModuleIcon.Vector(Icons.Rounded.Person),
                destination = PosScreens.CustomersManage,
                routeKey = RouteKey.CUSTOMERS_LIST
            )
        )
        add(
            ModuleMenuItem(
                label = "Cotizaciones",
                icon = ModuleIcon.Vector(Icons.Rounded.Description),
                destination = PosScreens.Quotes,
                routeKey = RouteKey.QUOTES_LIST
            )
        )
        add(
            ModuleMenuItem(
                label = "Ventas",
                icon = ModuleIcon.Vector(Icons.Rounded.ReceiptLong),
                destination = PosScreens.Orders,
                routeKey = RouteKey.ORDERS_LIST
            )
        )
        add(
            ModuleMenuItem(
                label = "Gastos",
                icon = ModuleIcon.Vector(Icons.Rounded.Receipt),
                destination = PosScreens.Expenses,
                routeKey = RouteKey.EXPENSES_LIST
            )
        )
        add(
            ModuleMenuItem(
                label = "Sucursales",
                icon = ModuleIcon.Vector(Icons.Rounded.Business),
                destination = PosScreens.Branches,
                routeKey = RouteKey.SETTINGS_BRANCHES_OWNER
            )
        )
        if (canShowPaymentsModule) {
            add(
                ModuleMenuItem(
                    label = "Métodos de pago",
                    icon = ModuleIcon.Vector(Icons.Rounded.Payments),
                    destination = PosScreens.Payments,
                    routeKey = RouteKey.PAYMENTS_PAGE
                )
            )
        }
        if (!appDistribution.isPosBuild) {
            add(
                ModuleMenuItem(
                    label = "Dispositivos POS",
                    icon = ModuleIcon.Url(H10_POS_IMAGE_URL),
                    destination = PosScreens.PosDevicesScreen,
                    routeKey = RouteKey.SETTINGS_POS_DEVICES
                )
            )
        }
    }.filter { item ->
        AuthzEvaluator.canRoute(item.routeKey, currentUser, betaSnapshot)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (quickActions.isNotEmpty()) {
            MenuSection(title = "Acciones rápidas") {
                quickActions.chunked(2).forEach { rowItems ->
                    MenuRow(rowItems = rowItems) { item, modifier ->
                        QuickActionCard(
                            item = item,
                            tint = secondary,
                            onClick = {
                                item.beforeNavigate?.invoke()
                                navigate(item.destination)
                            },
                            modifier = modifier
                        )
                    }
                }
            }
        }

        if (modules.isNotEmpty()) {
            MenuSection(title = "Módulos") {
                modules.chunked(2).forEach { rowItems ->
                    MenuRow(rowItems = rowItems) { item, modifier ->
                        ModuleCard(
                            item = item,
                            tint = primary,
                            onClick = { navigate(item.destination) },
                            modifier = modifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = titleMediumBold()
        )
        content()
    }
}

@Composable
private fun <T> MenuRow(
    rowItems: List<T>,
    content: @Composable (T, Modifier) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        rowItems.forEach { item ->
            content(item, Modifier.weight(1f))
        }
        if (rowItems.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickActionCard(
    item: MenuEntry,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(88.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(horizontal = 14.dp, vertical = 14.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickIconBubble(
                icon = item.icon,
                tint = tint
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.label,
                    style = bodyMediumBold(color = tint),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun QuickIconBubble(
    icon: QuickActionIcon,
    tint: Color
) {
    when (icon) {
        is QuickActionIcon.Vector -> Icon(
            imageVector = icon.imageVector,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )

        is QuickActionIcon.Drawable -> Image(
            painter = painterResource(icon.resource),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ModuleCard(
    item: ModuleMenuItem,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(104.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(horizontal = 10.dp, vertical = 12.dp)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ModuleIconView(icon = item.icon, tint = tint)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.label,
                style = bodyMediumBold(color = tint),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ModuleIconView(
    icon: ModuleIcon,
    tint: Color,
) {
    when (icon) {
        is ModuleIcon.Vector -> Icon(
            imageVector = icon.imageVector,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(30.dp)
        )

        is ModuleIcon.Url -> AsyncImage(
            model = getImageRequest(LocalPlatformContext.current, icon.url),
            contentDescription = null,
            placeholder = ColorPainter(Color.Transparent),
            error = ColorPainter(Color.Transparent),
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(38.dp)
        )
    }
}
