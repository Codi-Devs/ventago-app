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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.core.authz.RouteKey
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.quotes.domain.QuoteSelectionStore
import com.teco.ventago.navigation.PosScreens
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.pos

private sealed class QuickActionIcon {
    data class Vector(val imageVector: ImageVector) : QuickActionIcon()
    data class Drawable(val resource: DrawableResource) : QuickActionIcon()
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
    val icon: ImageVector,
    val destination: PosScreens,
    val routeKey: RouteKey,
)

@Composable
fun MenuScreen(
    navigate: (PosScreens) -> Unit
) {
    val authService = koinInject<IAuthService>()
    val betaService = koinInject<BetaService>()
    val currentUser by authService.getUser().collectAsState(initial = null)
    val betaResponse by betaService.features().collectAsState()
    val betaSnapshot = remember(betaResponse) {
        betaResponse?.features.orEmpty()
            .mapNotNull(BetaFeature::fromKey)
            .toSet()
    }
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

    val quickActions = listOf(
        MenuEntry(
            label = "Nueva venta",
            icon = QuickActionIcon.Drawable(Res.drawable.pos),
            destination = PosScreens.POS,
            routeKey = RouteKey.ORDERS_NEW
        ),
        quoteOrCustomerQuickAction,
        MenuEntry(
            label = "Registrar gasto",
            icon = QuickActionIcon.Vector(Icons.Rounded.Receipt),
            destination = PosScreens.NewExpenseScreen,
            routeKey = RouteKey.EXPENSE_NEW
        ),
        MenuEntry(
            label = "Agregar producto",
            icon = QuickActionIcon.Vector(Icons.Rounded.Inventory2),
            destination = PosScreens.AddItemScreen,
            routeKey = RouteKey.PRODUCT_ADD
        ),
    ).filter { item ->
        AuthzEvaluator.canRoute(item.routeKey, currentUser, betaSnapshot)
    }

    val modules = listOf(
        ModuleMenuItem(
            label = "Productos/servicios",
            icon = Icons.Rounded.Inventory2,
            destination = PosScreens.ProductsManage,
            routeKey = RouteKey.PRODUCTS_LIST
        ),
        ModuleMenuItem(
            label = "Clientes",
            icon = Icons.Rounded.Person,
            destination = PosScreens.CustomersManage,
            routeKey = RouteKey.CUSTOMERS_LIST
        ),
        ModuleMenuItem(
            label = "Cotizaciones",
            icon = Icons.Rounded.Description,
            destination = PosScreens.Quotes,
            routeKey = RouteKey.QUOTES_LIST
        ),
        ModuleMenuItem(
            label = "Ventas",
            icon = Icons.Rounded.ReceiptLong,
            destination = PosScreens.Orders,
            routeKey = RouteKey.ORDERS_LIST
        ),
        ModuleMenuItem(
            label = "Gastos",
            icon = Icons.Rounded.Receipt,
            destination = PosScreens.Expenses,
            routeKey = RouteKey.EXPENSES_LIST
        ),
        ModuleMenuItem(
            label = "Sucursales",
            icon = Icons.Rounded.Business,
            destination = PosScreens.Branches,
            routeKey = RouteKey.SETTINGS_BRANCHES_OWNER
        ),
        ModuleMenuItem(
            label = "Métodos de pago",
            icon = Icons.Rounded.Payments,
            destination = PosScreens.Payments,
            routeKey = RouteKey.PAYMENTS_PAGE
        ),
    ).filter { item ->
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
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(30.dp)
            )
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
