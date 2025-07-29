package com.yral.android.ui.screens.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.screens.game.GameIconConstants.ANIMATION_DURATION
import com.yral.android.ui.screens.game.GameIconConstants.NUDGE_ANIMATION_DURATION
import com.yral.android.ui.screens.game.GameIconConstants.ROTATION_DEGREE
import com.yral.android.ui.screens.game.GameIconConstants.SCALING_FACTOR
import com.yral.android.ui.widgets.YralAsyncImage
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.game.domain.models.GameIconNames
import kotlinx.coroutines.delay

private object GameIconConstants {
    const val ROTATION_DEGREE = -15f
    const val SCALING_FACTOR = 1.17f
    const val ANIMATION_DURATION = 200L
    const val NUDGE_ANIMATION_DURATION = 360L
}

@Composable
fun LocalGameIcon(
    modifier: Modifier,
    icon: GameIcon,
    currentIcon: Int? = null,
    animate: Boolean = false,
    onAnimationComplete: () -> Unit,
) {
    val animationDuration = currentIcon?.let { ANIMATION_DURATION } ?: NUDGE_ANIMATION_DURATION
    val rotation by animateFloatAsState(
        targetValue = if (animate) ROTATION_DEGREE else 0f,
        animationSpec = tween(durationMillis = animationDuration.toInt()),
        label = "rotation",
    )
    val scale by animateFloatAsState(
        targetValue = if (animate) SCALING_FACTOR else 1f,
        animationSpec = tween(durationMillis = animationDuration.toInt()),
        label = "scale",
    )
    LaunchedEffect(animate) {
        delay(animationDuration)
        onAnimationComplete()
    }
    Image(
        modifier =
            modifier
                .size(46.dp)
                .graphicsLayer(
                    rotationZ = rotation,
                    scaleX = scale,
                    scaleY = scale,
                ),
        painter = painterResource(id = icon.getResource()),
        contentDescription = "image description",
        contentScale = ContentScale.FillBounds,
    )
}

@Composable
fun AsyncGameIcon(
    modifier: Modifier,
    icon: GameIcon,
    currentIcon: Int? = null,
    animate: Boolean = false,
    onAnimationComplete: () -> Unit,
    loadLocal: () -> Unit,
) {
    val animationDuration = currentIcon?.let { ANIMATION_DURATION } ?: NUDGE_ANIMATION_DURATION
    val rotation by animateFloatAsState(
        targetValue = if (animate) ROTATION_DEGREE else 0f,
        animationSpec = tween(durationMillis = animationDuration.toInt()),
        label = "rotation",
    )
    val scale by animateFloatAsState(
        targetValue = if (animate) SCALING_FACTOR else 1f,
        animationSpec = tween(durationMillis = animationDuration.toInt()),
        label = "scale",
    )
    LaunchedEffect(animate) {
        delay(animationDuration)
        onAnimationComplete()
    }
    YralAsyncImage(
        imageUrl = icon.imageUrl,
        modifier =
            modifier
                .size(46.dp)
                .graphicsLayer(
                    rotationZ = rotation,
                    scaleX = scale,
                    scaleY = scale,
                ),
        onError = { loadLocal() },
    )
}

fun GameIcon.getResource(): Int =
    when (imageName) {
        GameIconNames.LAUGH -> R.drawable.laughing
        GameIconNames.HEART -> R.drawable.heart
        GameIconNames.FIRE -> R.drawable.fire
        GameIconNames.SURPRISE -> R.drawable.surprise
        GameIconNames.ROCKET -> R.drawable.rocket
    }
