package com.teco.ventago.design_system.molecules

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.design_system.theme.latoFontFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DMTopAppBar(
    title: String,
    navigateBack: () -> Unit,
    showBackButton: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
    ),
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    modifier: Modifier = Modifier
) {

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    androidx.compose.material3.TopAppBar(
        modifier = modifier,
        colors = colors,
        windowInsets = windowInsets,
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontFamily = latoFontFamily(),
                    fontWeight = FontWeight.W500,
                    textAlign = TextAlign.Start,
                )
            )
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = {
                    navigateBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Localized description"
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
        actions = actions,
    )
}

@Stable
class AppChromeState(
    initial: Color,
    private val scope: CoroutineScope
) {
    private val target = MutableStateFlow(initial)
    @Composable
    fun animated(): Color = animateColorAsState(target.collectAsState().value).value
    fun set(c: Color) = scope.launch { target.emit(c) }
}

@Composable
fun rememberAppChromeState(
    initial: Color = MaterialTheme.colorScheme.background
): AppChromeState {
    val scope = rememberCoroutineScope()
    return remember { AppChromeState(initial, scope) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColoredTopBarHost(
    color: Color,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)            // taller so it can sit “behind” the first card
            .clip(shape)
            .background(color)
            .shadow(6.dp, shape)
    ) {
        // The bar itself handles the status bar inset, not the host
        Box(Modifier.statusBarsPadding()) {
            content()
        }
    }
}