package com.teco.ventago.design_system.organism

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.titleLarge
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import ventago.composeapp.generated.resources.Res

enum class LoadingState {
    HIDDEN,   // bottom sheet is hidden
    LOADING,  // bottom sheet is visible, and disables screen interaction, is loading
    ERROR,    // bottom sheet is visible and will hide after success animation ends
    SUCCESS,  // bottom sheet is visible and will hide after error animation ends
}

data class LoadingBottomSheetState(
    var state: LoadingState = LoadingState.HIDDEN,
    val title: String = "Cargando...",
) {
    fun isLoading(): Boolean {
      return state == LoadingState.LOADING || state == LoadingState.ERROR || state == LoadingState.SUCCESS
    }

    fun isError(): Boolean {
      return state == LoadingState.ERROR
    }

    fun isSuccess(): Boolean {
      return state == LoadingState.SUCCESS
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadingBottomSheet(
    loadingState: MutableState<LoadingBottomSheetState>,
    sheetState: SheetState,
    loadingAnimationFile: String = "files/61209-loading-loop.json",
    onDismissRequest: () -> Unit = {},
) {

    val state by loadingState
    val canDismiss = state.state != LoadingState.LOADING
    val dismissSheet = rememberDismissSheetAfterHide(
        sheetState,
        state.state,
        canDismiss,
        onDismissRequest,
    )

    ModalBottomSheet(
        containerColor = cardContainerColor(),
        onDismissRequest = dismissSheet,
        sheetState = sheetState,
        sheetGesturesEnabled = canDismiss,
        properties = loadingSheetProperties(canDismiss),
    ) {
        when(state.state) {
            LoadingState.LOADING -> {
                LoadingContent(
                    title = state.title,
                    loadingAnimationFile = loadingAnimationFile
                )
            }
            LoadingState.ERROR -> {
                ActionContent(false, state.title, dismissSheet)
            }
            LoadingState.SUCCESS -> {
                ActionContent(true, state.title, dismissSheet)
            }
            else -> {
                LoadingContent(state.title)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadingSheet(
    state: LoadingBottomSheetState,
    sheetState: SheetState,
    loadingAnimationFile: String = "files/61209-loading-loop.json",
    onDismissRequest: () -> Unit = {},
) {
    val canDismiss = state.state != LoadingState.LOADING
    val dismissSheet = rememberDismissSheetAfterHide(
        sheetState,
        state.state,
        canDismiss,
        onDismissRequest,
    )

    ModalBottomSheet(
        containerColor = cardContainerColor(),
        onDismissRequest = dismissSheet,
        sheetState = sheetState,
        sheetGesturesEnabled = canDismiss,
        properties = loadingSheetProperties(canDismiss),
    ) {
        when(state.state) {
            LoadingState.LOADING -> {
                LoadingContent(
                    title = state.title,
                    loadingAnimationFile = loadingAnimationFile
                )
            }
            LoadingState.ERROR -> {
                ActionContent(false, state.title, dismissSheet)
            }
            LoadingState.SUCCESS -> {
                ActionContent(true, state.title, dismissSheet)
            }
            else -> {
                LoadingContent(state.title)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun loadingSheetProperties(canDismiss: Boolean) = ModalBottomSheetProperties(
    shouldDismissOnBackPress = canDismiss,
    shouldDismissOnClickOutside = canDismiss,
)


internal class SheetDismissLock {
    private var phase: Any? = null
    private var held = false

    fun sync(phase: Any) {
        if (this.phase != phase) {
            this.phase = phase
            held = false
        }
    }

    fun tryAcquire(): Boolean {
        if (held) return false
        held = true
        return true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberDismissSheetAfterHide(
    sheetState: SheetState,
    phase: Any,
    canDismiss: Boolean,
    onDismissed: () -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    val lock = remember { SheetDismissLock() }
    lock.sync(phase)
    return dismiss@{
        if (!canDismiss || !lock.tryAcquire()) return@dismiss
        scope.launch {
            try {
                sheetState.hide()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // iOS aborts the process if hide() throws out of this coroutine.
            }
            onDismissed()
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun ActionContent(success: Boolean, title: String, onAnimationFinished: () -> Unit = {}) {
    val composition by rememberLottieComposition {
        if (success) {
            LottieCompositionSpec.JsonString(
                Res.readBytes("files/57767-done.json").decodeToString()
            )
        } else {
            LottieCompositionSpec.JsonString(
                Res.readBytes("files/wrong.json").decodeToString()
            )
        }
    }
    val progress by animateLottieCompositionAsState(composition, iterations = 1,)
    val finished = remember { mutableStateOf(false) }
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = title,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            style = titleLarge()
        )

        Image(
            painter = rememberLottiePainter(
                composition = composition,
                progress = {progress},
            ),
            modifier = Modifier.size(height = 150.dp, width = 150.dp),
            contentDescription = "Lottie animation"
        )


    }

    LaunchedEffect(progress, composition) {
        if (finished.value || composition == null || progress < 1f) return@LaunchedEffect
        finished.value = true
        onAnimationFinished()
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun LoadingContent(
    title: String,
    loadingAnimationFile: String = "files/61209-loading-loop.json",
) {
    val loadingComposition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes(loadingAnimationFile).decodeToString()
        )
    }

    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = title,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            style = titleLarge()
        )

        Image(
            modifier = Modifier.size(height = 150.dp, width = 150.dp),
            painter = rememberLottiePainter(
                composition = loadingComposition,
                iterations = Compottie.IterateForever
            ),
            contentDescription = "Lottie animation"
        )
    }
}
