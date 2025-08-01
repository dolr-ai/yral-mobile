package com.yral.android.ui.screens.feed.uiComponets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.feed.uiComponets.HowToPlayConstants.ANIMATION_DURATION
import com.yral.android.ui.screens.feed.uiComponets.HowToPlayConstants.AUTO_CLOSE_DELAY
import com.yral.android.ui.screens.feed.uiComponets.HowToPlayConstants.PAGE_SET_DELAY
import kotlinx.coroutines.delay

@Composable
fun HowToPlay(
    modifier: Modifier,
    shouldExpand: Boolean,
    onClick: () -> Unit,
    onAnimationComplete: () -> Unit = {},
) {
    var backgroundVisible by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(shouldExpand) {
        delay(PAGE_SET_DELAY)
        if (shouldExpand) {
            runCatching {
                backgroundVisible = true
                isExpanded = true
                delay(AUTO_CLOSE_DELAY)
                isExpanded = false
                delay(ANIMATION_DURATION.toLong() / 2)
                backgroundVisible = false
                onAnimationComplete()
            }.onFailure {
                isExpanded = false
                backgroundVisible = false
            }
        } else {
            isExpanded = false
            backgroundVisible = false
        }
    }
    Row(
        modifier =
            modifier
                .applyBackground(backgroundVisible)
                .padding(horizontal = 2.dp, vertical = 2.dp)
                .clickable { onClick() },
        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_how_to_play),
            contentDescription = "how to play",
            contentScale = ContentScale.None,
            modifier = Modifier.size(32.dp),
        )
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                (expandHorizontally { 0 } + fadeIn(tween(ANIMATION_DURATION))) togetherWith
                    (shrinkHorizontally { -it } + fadeOut(tween(ANIMATION_DURATION))) using
                    SizeTransform(clip = false)
            },
        ) { isExpanded ->
            if (isExpanded) {
                Text(
                    text = stringResource(R.string.how_to_play),
                    style = LocalAppTopography.current.regMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 6.dp, end = 8.dp),
                )
            }
        }
    }
}

private fun Modifier.applyBackground(backgroundVisible: Boolean) =
    if (backgroundVisible) {
        this.background(
            color = YralColors.HowToPlayBackground,
            shape = RoundedCornerShape(49.dp),
        )
    } else {
        this
    }

object HowToPlayConstants {
    const val PAGE_SET_DELAY = 700L
    const val ANIMATION_DURATION = 700
    const val AUTO_CLOSE_DELAY = 3000L
}
