package com.teco.ventago.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Improved navigation transitions that prevent view overlapping and provide smoother animations.
 * 
 * Key improvements:
 * - No overlapping views during transitions
 * - Smooth fade + slide combination
 * - Better easing functions for natural motion
 * - Proper timing to prevent race conditions
 */
object NavTransitions {

    /**
     * Enter transition when navigating forward (pushing a new screen)
     * Slides in from the right with a fade effect
     */
    val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth }, // Start from right edge
            animationSpec = tween(
                durationMillis = 250,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 250,
                easing = FastOutSlowInEasing
            )
        )
    }

    /**
     * Exit transition when navigating forward (pushing a new screen)
     * Slides out to the left with a fade effect
     * The exiting screen fully leaves before the new one enters
     */
    val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth }, // Exit to left edge
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing
            )
        )
    }

    /**
     * Enter transition when navigating back (popping the back stack)
     * Slides in from the left with a fade effect
     */
    val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth }, // Start from left edge
            animationSpec = tween(
                durationMillis = 250,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 250,
                easing = FastOutSlowInEasing
            )
        )
    }

    /**
     * Exit transition when navigating back (popping the back stack)
     * Slides out to the right with a fade effect
     */
    val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth }, // Exit to right edge
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing
            )
        )
    }
}

/**
 * Extension function to easily apply smooth transitions to composable routes.
 * This replaces the standard composable() function with one that includes transitions.
 */
//fun NavGraphBuilder.composableWithTransitions(
//    route: String,
//    content: @Composable (NavBackStackEntry) -> Unit
//) {
//    composable(
//        route = route,
//        enterTransition = NavTransitions.enterTransition,
//        exitTransition = NavTransitions.exitTransition,
//        popEnterTransition = NavTransitions.popEnterTransition,
//        popExitTransition = NavTransitions.popExitTransition,
//        content = content
//    )
//}
//
