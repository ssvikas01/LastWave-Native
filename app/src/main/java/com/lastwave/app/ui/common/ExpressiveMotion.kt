package com.lastwave.app.ui.common

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Shared, restrained Material 3 Expressive motion language for the app. */
object ExpressiveMotion {
    const val Quick = 120
    const val Standard = 220
    const val Emphasized = 300

    // Spatial springs: snappy & fluid for 120Hz panels
    fun <T> spatialFast() = spring<T>(
        dampingRatio = 0.80f,
        stiffness = Spring.StiffnessMedium,
    )

    fun <T> spatialDefault() = spring<T>(
        dampingRatio = 0.84f,
        stiffness = 850f,
    )

    fun <T> spatialSlow() = spring<T>(
        dampingRatio = 0.86f,
        stiffness = 520f,
    )

    // Legacy alias
    fun <T> spatialSpring() = spatialDefault<T>()

    // Effects springs: no overshoot (for alpha, color)
    fun <T> effectsFast() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    fun <T> effectsDefault() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 900f,
    )

    fun <T> effectsSlow() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 520f,
    )

    // Legacy alias
    fun <T> smoothSpring() = effectsDefault<T>()

    fun forwardEnter(): EnterTransition =
        fadeIn(effectsDefault()) +
            scaleIn(spatialDefault(), initialScale = 0.95f) +
            slideInHorizontally(spatialDefault()) { it / 16 }

    fun forwardExit(): ExitTransition =
        fadeOut(effectsDefault()) +
            scaleOut(effectsDefault(), targetScale = 0.96f)

    fun backEnter(): EnterTransition =
        fadeIn(effectsDefault()) +
            scaleIn(spatialDefault(), initialScale = 0.96f)

    fun backExit(): ExitTransition =
        fadeOut(effectsFast()) +
            scaleOut(effectsDefault(), targetScale = 0.95f) +
            slideOutHorizontally(spatialDefault()) { it / 16 }
}

/**
 * Tactile spring press feedback modifier conforming to Material 3 Expressive.
 * Subtly squishes the component when held and springs back dynamically on release.
 */
@Composable
fun Modifier.expressivePressScale(
    interactionSource: MutableInteractionSource,
    targetScale: Float = 0.96f,
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = ExpressiveMotion.spatialFast(),
        label = "expressivePressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Clickable surface with expressive spring-based bounce feedback.
 */
@Composable
fun Modifier.expressiveBounceClickable(
    enabled: Boolean = true,
    targetScale: Float = 0.96f,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .expressivePressScale(interactionSource, targetScale)
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            onClick = onClick,
        )
}
