package com.yral.shared.features.feed.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralLoadingDots
import com.yral.shared.libs.designsystem.component.formatAbbreviation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.libs.designsystem.generated.resources.Res
import yral_mobile.shared.libs.designsystem.generated.resources.fire

@Suppress("MagicNumber")
private val StreakChipGradientCenter = Color(0xFF513843)

@Suppress("MagicNumber")
private val StreakChipGradientEdge = Color(0xFF15121A)
private val StreakChipDotsSize = DpSize(30.dp, 20.dp)
private val StreakChipHeight = 38.dp
private val FireIconSize = 24.dp
private val StreakChipHorizontalPadding = 10.dp
private val StreakChipVerticalPadding = 5.dp
private val StreakIconTextSpacing = 4.dp
private const val STREAK_CHIP_CORNER_RADIUS = 50

@Composable
fun FeedStreakChip(
    streakCount: Long?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .height(StreakChipHeight)
                .background(
                    brush =
                        Brush.radialGradient(
                            colors = listOf(StreakChipGradientCenter, StreakChipGradientEdge),
                        ),
                    shape = RoundedCornerShape(STREAK_CHIP_CORNER_RADIUS),
                ).padding(
                    vertical = StreakChipVerticalPadding,
                    horizontal = StreakChipHorizontalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StreakIconTextSpacing),
    ) {
        Image(
            painter = painterResource(Res.drawable.fire),
            contentDescription = null,
            modifier = Modifier.size(FireIconSize),
        )
        if (streakCount == null) {
            YralLoadingDots(size = StreakChipDotsSize)
        } else {
            Text(
                text = formatAbbreviation(streakCount, 1),
                style = LocalAppTopography.current.mdSemiBold,
                color = YralColors.NeutralTextPrimary,
            )
        }
    }
}
