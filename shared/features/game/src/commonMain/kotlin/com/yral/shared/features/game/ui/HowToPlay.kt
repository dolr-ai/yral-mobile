package com.yral.shared.features.game.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.game.generated.resources.Res
import yral_mobile.shared.features.game.generated.resources.how_to_play
import yral_mobile.shared.libs.designsystem.generated.resources.ic_how_to_play
import kotlin.coroutines.cancellation.CancellationException
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
fun HowToPlay(
    modifier: Modifier,
    shouldExpand: Boolean,
    pageNo: Int,
    onClick: () -> Unit,
    onAnimationComplete: () -> Unit = {},
) {
    var backgroundVisible by remember { mutableStateOf(false) }
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
            painter = painterResource(DesignRes.drawable.ic_how_to_play),
            contentDescription = "how to play",
            contentScale = ContentScale.None,
            modifier = Modifier.size(32.dp),
        )
        AnimatedBackground(
            pageNo = pageNo,
            shouldExpand = shouldExpand,
            setBackGroundVisible = { backgroundVisible = it },
            onAnimationComplete = onAnimationComplete,
        )
    }
}

@Composable
private fun AnimatedBackground(
    pageNo: Int,
    shouldExpand: Boolean,
    setBackGroundVisible: (Boolean) -> Unit,
    onAnimationComplete: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(shouldExpand) {
        try {
            if (shouldExpand) {
                delay(HowToPlayConstants.PAGE_SET_DELAY)
                setBackGroundVisible(true)
                isExpanded = true
                delay(HowToPlayConstants.AUTO_CLOSE_DELAY)
                isExpanded = false
                delay(HowToPlayConstants.ANIMATION_DURATION.toLong() / 2)
                setBackGroundVisible(false)
                onAnimationComplete()
            } else {
                // Instantly collapse and hide background
                isExpanded = false
                setBackGroundVisible(false)
            }
        } catch (e: CancellationException) {
            // Reset state for safety on coroutine cancel
            isExpanded = false
            setBackGroundVisible(false)
            throw e
        } catch (_: Exception) {
            isExpanded = false
            setBackGroundVisible(false)
        }
    }
    AnimatedContent(
        label = "HowToPlay $pageNo",
        targetState = isExpanded,
        transitionSpec = {
            (expandHorizontally { 0 } + fadeIn(tween(HowToPlayConstants.ANIMATION_DURATION))) togetherWith
                (shrinkHorizontally { -it } + fadeOut(tween(HowToPlayConstants.ANIMATION_DURATION))) using
                SizeTransform(clip = false)
        },
    ) { isExpanded ->
        if (isExpanded) {
            Text(
                text = stringResource(Res.string.how_to_play),
                style = LocalAppTopography.current.regMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(start = 6.dp, end = 8.dp),
            )
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
