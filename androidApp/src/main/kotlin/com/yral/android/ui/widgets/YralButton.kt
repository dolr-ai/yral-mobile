package com.yral.android.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors

@Composable
fun YralButton(
    text: String,
    textColor: Color = YralColors.NeutralBlack,
    backgroundColor: Color = Color.White,
    icon: Int? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(45.dp)
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(8.dp),
                ).shadow(
                    elevation = 22.5.dp,
                    spotColor = YralColors.shadowSpotColor,
                    ambientColor = YralColors.shadowAmbientColor,
                ).border(
                    width = 0.75.dp,
                    color = YralColors.buttonBorderColor,
                    shape = RoundedCornerShape(size = 8.dp),
                ).padding(
                    start = 69.dp,
                    top = 11.dp,
                    end = 69.dp,
                    bottom = 11.dp,
                ).clickable {
                    onClick()
                },
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Image(
                painter = painterResource(id = icon),
                contentDescription = "image description",
                contentScale = ContentScale.None,
            )
        }
        Text(
            text = text,
            style = LocalAppTopography.current.mdBold,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}
