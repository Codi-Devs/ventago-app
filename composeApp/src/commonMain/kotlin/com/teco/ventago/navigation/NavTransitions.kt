package com.teco.ventago.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry

/**
 * Different transition styles available for navigation.
 * Change the [currentStyle] to switch between different animation styles.
 */
enum class TransitionStyle {
    HORIZONTAL_SLIDE,      // Classic horizontal slide (current default)
    VERTICAL_SLIDE,        // Vertical slide up/down
    SCALE_FADE,           // Zoom in/out with fade
    MATERIAL_BOTTOM,       // Material-style bottom sheet slide
    CROSS_FADE,           // Simple cross-fade only
    MATERIAL_SLIDE         // Material Design horizontal slide with elevation feel
}

/**
 * Current transition style. Change this to switch between different animation styles.
 */
private val currentStyle: TransitionStyle = TransitionStyle.MATERIAL_SLIDE

/**
 * Improved navigation transitions that prevent view overlapping and provide smoother animations.
 * 
 * Multiple styles available - change [currentStyle] to switch between them.
 * 
 * Key improvements:
 * - No overlapping views during transitions
 * - Smooth fade + slide combinations
 * - Better easing functions for natural motion
 * - Proper timing to prevent race conditions
 */
object NavTransitions {

    /**
     * Enter transition when navigating forward (pushing a new screen)
     */
    val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when (currentStyle) {
            TransitionStyle.HORIZONTAL_SLIDE -> horizontalSlideEnter()
            TransitionStyle.VERTICAL_SLIDE -> verticalSlideEnter()
            TransitionStyle.SCALE_FADE -> scaleFadeEnter()
            TransitionStyle.MATERIAL_BOTTOM -> materialBottomEnter()
            TransitionStyle.CROSS_FADE -> crossFadeEnter()
            TransitionStyle.MATERIAL_SLIDE -> materialSlideEnter()
        }
    }

    /**
     * Exit transition when navigating forward (pushing a new screen)
     */
    val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when (currentStyle) {
            TransitionStyle.HORIZONTAL_SLIDE -> horizontalSlideExit()
            TransitionStyle.VERTICAL_SLIDE -> verticalSlideExit()
            TransitionStyle.SCALE_FADE -> scaleFadeExit()
            TransitionStyle.MATERIAL_BOTTOM -> materialBottomExit()
            TransitionStyle.CROSS_FADE -> crossFadeExit()
            TransitionStyle.MATERIAL_SLIDE -> materialSlideExit()
        }
    }

    /**
     * Enter transition when navigating back (popping the back stack)
     */
    val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when (currentStyle) {
            TransitionStyle.HORIZONTAL_SLIDE -> horizontalSlidePopEnter()
            TransitionStyle.VERTICAL_SLIDE -> verticalSlidePopEnter()
            TransitionStyle.SCALE_FADE -> scaleFadePopEnter()
            TransitionStyle.MATERIAL_BOTTOM -> materialBottomPopEnter()
            TransitionStyle.CROSS_FADE -> crossFadeEnter()
            TransitionStyle.MATERIAL_SLIDE -> materialSlidePopEnter()
        }
    }

    /**
     * Exit transition when navigating back (popping the back stack)
     */
    val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when (currentStyle) {
            TransitionStyle.HORIZONTAL_SLIDE -> horizontalSlidePopExit()
            TransitionStyle.VERTICAL_SLIDE -> verticalSlidePopExit()
            TransitionStyle.SCALE_FADE -> scaleFadePopExit()
            TransitionStyle.MATERIAL_BOTTOM -> materialBottomPopExit()
            TransitionStyle.CROSS_FADE -> crossFadeExit()
            TransitionStyle.MATERIAL_SLIDE -> materialSlidePopExit()
        }
    }

    // ========== STYLE 1: Horizontal Slide (Classic) ==========
    private fun AnimatedContentTransitionScope<NavBackStackEntry>.horizontalSlideEnter(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(250, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(250, easing = FastOutSlowInEasing))
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.horizontalSlideExit(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth },
            animationSpec = tween(200, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(200, easing = FastOutSlowInEasing))
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.horizontalSlidePopEnter(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth },
            animationSpec = tween(250, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(250, easing = FastOutSlowInEasing))
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.horizontalSlidePopExit(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(200, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(200, easing = FastOutSlowInEasing))
    }

    // ========== STYLE 2: Vertical Slide ==========
    private fun AnimatedContentTransitionScope<NavBackStackEntry>.verticalSlideEnter(): EnterTransition {
        return slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(300, easing = FastOutSlowInEasing))
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.verticalSlideExit(): ExitTransition {
        return slideOutVertically(
            targetOffsetY = { fullHeight -> -fullHeight },
            animationSpec = tween(250, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(250, easing = FastOutSlowInEasing))
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.verticalSlidePopEnter(): EnterTransition {
        return slideInVertically(
            initialOffsetY = { fullHeight -> -fullHeight },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(300, easing = FastOutSlowInEasing))
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.verticalSlidePopExit(): ExitTransition {
        return slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(250, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(250, easing = FastOutSlowInEasing))
    }

    // ========== STYLE 3: Scale + Fade (Zoom) ==========
    private fun AnimatedContentTransitionScope<NavBackStackEntry>.scaleFadeEnter(): EnterTransition {
        return scaleIn(
            initialScale = 0.85f,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(300, easing = FastOutSlowInEasing))
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.scaleFadeExit(): ExitTransition {
        return scaleOut(
            targetScale = 0.95f,
            animationSpec = tween(250, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(250, easing = FastOutSlowInEasing))
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.scaleFadePopEnter(): EnterTransition {
        return scaleIn(
            initialScale = 0.95f,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(300, easing = FastOutSlowInEasing))
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.scaleFadePopExit(): ExitTransition {
        return scaleOut(
            targetScale = 0.85f,
            animationSpec = tween(250, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(250, easing = FastOutSlowInEasing))
    }

    // ========== STYLE 4: Material Bottom Sheet ==========
    private fun AnimatedContentTransitionScope<NavBackStackEntry>.materialBottomEnter(): EnterTransition {
        return slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(300, easing = FastOutSlowInEasing))
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.materialBottomExit(): ExitTransition {
        return slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(250, easing = FastOutSlowInEasing))
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.materialBottomPopEnter(): EnterTransition {
        return slideInVertically(
            initialOffsetY = { fullHeight -> -fullHeight / 4 },
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(300, easing = FastOutSlowInEasing))
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.materialBottomPopExit(): ExitTransition {
        return slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(250, easing = FastOutSlowInEasing))
    }

    // ========== STYLE 5: Cross Fade (Simple) ==========
    private fun AnimatedContentTransitionScope<NavBackStackEntry>.crossFadeEnter(): EnterTransition {
        return fadeIn(tween(300, easing = FastOutSlowInEasing))
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.crossFadeExit(): ExitTransition {
        return fadeOut(tween(250, easing = FastOutSlowInEasing))
    }

    // ========== STYLE 6: Material Slide (with subtle scale) ==========
    private fun AnimatedContentTransitionScope<NavBackStackEntry>.materialSlideEnter(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> (fullWidth * 0.3f).toInt() },
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(300, easing = FastOutSlowInEasing)) + scaleIn(
            initialScale = 0.95f,
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        )
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.materialSlideExit(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> (-fullWidth * 0.3f).toInt() },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(250, easing = FastOutSlowInEasing)) + scaleOut(
            targetScale = 0.95f,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        )
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.materialSlidePopEnter(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> (-fullWidth * 0.3f).toInt() },
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(300, easing = FastOutSlowInEasing)) + scaleIn(
            initialScale = 0.95f,
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        )
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.materialSlidePopExit(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> (fullWidth * 0.3f).toInt() },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(250, easing = FastOutSlowInEasing)) + scaleOut(
            targetScale = 0.95f,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        )
    }
}

