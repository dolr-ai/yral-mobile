package com.yral.shared.features.game.ui

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
import com.yral.shared.features.game.ui.SmileyGameConstants.NUDGE_ANIMATION_DURATION
import com.yral.shared.libs.designsystem.component.neonBorder
import com.yral.shared.libs.designsystem.theme.YralColors.SmileyGameCardBackground

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
    Box(
        modifier =
            if (isShowingNudge) {
                modifier.neonBorder(
                    paddingValues = paddingValues,
                    cornerRadius = cornerRadius,
                    containerColor = containerColor,
                    animationDuration = NUDGE_ANIMATION_DURATION,
                )
            } else {
                modifier.padding(paddingValues)
            },
    ) {
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
