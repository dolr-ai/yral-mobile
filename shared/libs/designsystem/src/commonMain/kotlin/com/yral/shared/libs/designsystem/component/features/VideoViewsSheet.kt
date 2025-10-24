package com.yral.shared.libs.designsystem.component.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralInfoView
import com.yral.shared.libs.designsystem.component.formatAbbreviation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.libs.designsystem.generated.resources.Res
import yral_mobile.shared.libs.designsystem.generated.resources.engaged_views_description
import yral_mobile.shared.libs.designsystem.generated.resources.total_engaged_views
import yral_mobile.shared.libs.designsystem.generated.resources.total_views
import yral_mobile.shared.libs.designsystem.generated.resources.video_insights

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoViewsSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    thumbnailUrl: String,
    totalViews: ULong?,
    totalEngagedViews: ULong?,
) {
    YralBottomSheet(
        bottomSheetState = sheetState,
        onDismissRequest = onDismissRequest,
        dragHandle = { DragHandle(color = YralColors.Neutral500) },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 36.dp),
        ) {
            Text(
                text = stringResource(Res.string.video_insights),
                style = LocalAppTopography.current.lgBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (thumbnailUrl.isNotEmpty()) {
                YralAsyncImage(
                    imageUrl = thumbnailUrl,
                    modifier = Modifier.heightIn(max = 228.dp).widthIn(max = 119.dp),
                    contentScale = ContentScale.Crop,
                    shape = RoundedCornerShape(8.dp),
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                horizontalAlignment = Alignment.Start,
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(Res.string.total_views),
                        style = LocalAppTopography.current.mdMedium,
                        color = YralColors.NeutralTextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    totalViews?.let {
                        Text(
                            text = formatAbbreviation(totalViews.toLong(), 0),
                            style = LocalAppTopography.current.mdMedium,
                            color = YralColors.NeutralIconsActive,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(Res.string.total_engaged_views),
                        style = LocalAppTopography.current.mdMedium,
                        color = YralColors.NeutralTextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    totalEngagedViews?.let {
                        Text(
                            text = formatAbbreviation(totalEngagedViews.toLong(), 0),
                            style = LocalAppTopography.current.mdMedium,
                            color = YralColors.NeutralIconsActive,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                YralInfoView(
                    modifier = Modifier.fillMaxWidth(),
                    info = stringResource(Res.string.engaged_views_description),
                )
            }
        }
    }
}
