package com.yral.android.ui.screens.uploadVideo.aiVideoGen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.uploadVideo.aiVideoGen.AiVideoGenScreenConstants.ARROW_ROTATION
import com.yral.android.ui.widgets.YralAsyncImage
import com.yral.android.ui.widgets.getSVGImageModel
import com.yral.shared.features.uploadvideo.domain.models.Provider

@Composable
fun ModelDetails(
    provider: Provider,
    usedCredits: Int?,
    totalCredits: Int,
    onClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.model),
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.Neutral300,
        )
        ProviderRow(provider = provider, onClick = onClick)
        usedCredits?.let { usedCredits ->
            Text(
                text = stringResource(R.string.credits_used, usedCredits, totalCredits),
                style = LocalAppTopography.current.baseSemiBold,
                color = if (usedCredits < totalCredits) YralColors.Green300 else YralColors.Red300,
            )
        }
    }
}

@Composable
private fun ProviderRow(
    provider: Provider,
    onClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = YralColors.Neutral700, shape = RoundedCornerShape(size = 8.dp))
                .background(color = YralColors.Neutral900, shape = RoundedCornerShape(size = 8.dp))
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        provider.modelIcon?.let { url ->
            YralAsyncImage(
                imageUrl = getSVGImageModel(url),
                modifier = Modifier.size(30.dp),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = provider.name,
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextPrimary,
            )
            provider.description?.let { description ->
                Text(
                    text = description,
                    style = LocalAppTopography.current.regRegular,
                    color = YralColors.NeutralTextSecondary,
                )
            }
        }
        Image(
            painter = painterResource(id = R.drawable.arrow_left),
            contentDescription = "select model",
            contentScale = ContentScale.None,
            modifier = Modifier.size(24.dp).rotate(ARROW_ROTATION),
        )
    }
}
