package com.teco.ventago.core.keyboard

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager

@Stable
class KeyboardInputBoundsRegistry {
    private val bounds = mutableMapOf<Any, Rect>()

    fun update(key: Any, rect: Rect) {
        bounds[key] = rect
    }

    fun remove(key: Any) {
        bounds.remove(key)
    }

    fun contains(rootPosition: Offset): Boolean =
        bounds.values.any { it.contains(rootPosition) }
}

val LocalKeyboardInputBounds = staticCompositionLocalOf<KeyboardInputBoundsRegistry?> { null }

@Composable
fun KeyboardDismissHost(
    modifier: Modifier = Modifier.fillMaxSize(),
    content: @Composable () -> Unit
) {
    val registry = remember { KeyboardInputBoundsRegistry() }
    CompositionLocalProvider(LocalKeyboardInputBounds provides registry) {
        Box(modifier = modifier.dismissKeyboardOnOutsideTap()) {
            content()
        }
    }
}

fun Modifier.keyboardDismissTarget(): Modifier = composed {
    val registry = LocalKeyboardInputBounds.current ?: return@composed this
    val key = remember { Any() }
    DisposableEffect(registry) {
        onDispose { registry.remove(key) }
    }
    onGloballyPositioned { coordinates ->
        registry.update(key, coordinates.boundsInRoot())
    }
}

fun Modifier.dismissKeyboardOnOutsideTap(): Modifier = composed {
    val registry = LocalKeyboardInputBounds.current
    val hideKeyboard = rememberHideSoftwareKeyboard()
    val focusManager = LocalFocusManager.current
    var hostBounds by remember { mutableStateOf<Rect?>(null) }
    onGloballyPositioned { hostBounds = it.boundsInRoot() }
        .pointerInput(hideKeyboard, focusManager, registry) {
            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial
                )
                val host = hostBounds ?: return@awaitEachGesture
                val rootTap = Offset(
                    x = host.left + down.position.x,
                    y = host.top + down.position.y
                )
                val tappedInput = registry?.contains(rootTap) == true
                val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                if (up != null && !tappedInput) {
                    val slop = viewConfiguration.touchSlop
                    if ((up.position - down.position).getDistance() <= slop) {
                        focusManager.clearFocus(force = true)
                        hideKeyboard()
                    }
                }
            }
        }
}
