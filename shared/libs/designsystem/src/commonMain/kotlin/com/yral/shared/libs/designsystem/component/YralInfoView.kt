package com.yral.shared.libs.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.libs.designsystem.generated.resources.Res
import yral_mobile.shared.libs.designsystem.generated.resources.ic_information_circle

@Composable
fun YralInfoView(
    modifier: Modifier,
    info: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start),
        verticalAlignment = Alignment.Top,
        modifier =
            modifier
                .border(
                    width = 1.dp,
                    color = YralColors.Blue300,
                    shape = RoundedCornerShape(size = 8.dp),
                ).background(
                    color = YralColors.Blue500,
                    shape = RoundedCornerShape(size = 8.dp),
                ).padding(8.dp),
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_information_circle),
            contentDescription = "info",
            contentScale = ContentScale.Inside,
            colorFilter = ColorFilter.tint(YralColors.Blue100),
            modifier =
                Modifier
                    .padding(1.dp)
                    .width(18.dp)
                    .height(18.dp),
        )
        Text(
            text = info,
            style = LocalAppTopography.current.regRegular,
            color = YralColors.Blue100,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
