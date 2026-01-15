package com.yral.shared.features.auth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralShimmerImage
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.phonevalidation.countries.Country
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.libs.designsystem.generated.resources.Res
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left

@Composable
fun CountryPickerButton(
    selectedCountry: Country?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(
                    color = YralColors.Neutral900,
                    shape = RoundedCornerShape(8.dp),
                ).border(
                    width = 1.dp,
                    color = YralColors.Neutral700,
                    shape = RoundedCornerShape(8.dp),
                ).clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Flag image with shimmer placeholder
            YralShimmerImage(
                imageUrl = selectedCountry?.flagUrl,
                placeholderImageUrl = "",
                modifier = Modifier.width(31.5.dp).height(22.5.dp),
                contentScale = ContentScale.FillBounds,
                shape = RoundedCornerShape(4.dp),
            )

            // Dropdown arrow (rotated arrow-left icon)
            Icon(
                painter = painterResource(Res.drawable.arrow_left),
                contentDescription = "Select country",
                tint = Color.White,
                modifier =
                    Modifier
                        .size(24.dp)
                        .rotate(LEFT_TO_DOWN_ROTATION),
            )
        }
    }
}

private const val LEFT_TO_DOWN_ROTATION = 270f
