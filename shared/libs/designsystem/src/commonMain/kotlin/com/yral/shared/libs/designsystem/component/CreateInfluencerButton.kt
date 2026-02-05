package com.yral.shared.libs.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.libs.designsystem.generated.resources.Res
import yral_mobile.shared.libs.designsystem.generated.resources.create_influencer
import yral_mobile.shared.libs.designsystem.generated.resources.ic_plus_circle
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
fun CreateInfluencerButton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    alignIconToEnd: Boolean = false,
    onClick: () -> Unit,
) {
    val gradientColors =
        listOf(
            YralColors.ProGradientOrange,
            YralColors.ProGradientPink,
        )
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(brush = Brush.linearGradient(colors = gradientColors))
                .clickable(onClick = onClick)
                .padding(contentPadding),
        horizontalArrangement =
            if (alignIconToEnd) {
                Arrangement.SpaceBetween
            } else {
                Arrangement.spacedBy(6.dp)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.create_influencer),
            style = LocalAppTopography.current.mdMedium,
            color = Color.White,
            modifier = if (alignIconToEnd) Modifier.weight(1f) else Modifier,
        )
        Icon(
            painter = painterResource(DesignRes.drawable.ic_plus_circle),
            contentDescription = "Create Influencer",
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}
