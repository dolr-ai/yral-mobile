package com.yral.android.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yral.android.ui.design.YralColors.SmileyGameCardBackground
import com.yral.android.ui.screens.game.SmileyGameConstants.NUDGE_ANIMATION_DURATION
import com.yral.android.ui.widgets.YralNeonBorder

@Composable
fun GameStripBackground(
    modifier: Modifier,
    isShowingNudge: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceBetween,
    content: @Composable () -> Unit,
) {
    val containerColor = SmileyGameCardBackground
    val cornerRadius = 49.dp
    val paddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp)

    Box {
        if (isShowingNudge) {
            YralNeonBorder(
                paddingValues = paddingValues,
                cornerRadius = cornerRadius,
                containerColor = containerColor,
                animationDuration = NUDGE_ANIMATION_DURATION,
            )
        }
        Row(modifier = modifier.padding(paddingValues)) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = containerColor,
                            shape = RoundedCornerShape(size = cornerRadius),
                        ).padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = horizontalArrangement,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }
        }
    }
}
