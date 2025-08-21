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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.widgets.YralAsyncImage
import com.yral.android.ui.widgets.YralBottomSheet
import com.yral.android.ui.widgets.getSVGImageModel
import com.yral.shared.features.uploadvideo.domain.models.Provider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelection(
    bottomSheetState: SheetState,
    providers: List<Provider>,
    selectedProvider: Provider?,
    dismissSheet: () -> Unit,
    setSelectedProvider: (Provider) -> Unit,
) {
    YralBottomSheet(
        onDismissRequest = dismissSheet,
        bottomSheetState = bottomSheetState,
        dragHandle = { DragHandle(color = YralColors.Neutral500) },
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(providers) { provider ->
                ProviderSelectionRow(
                    provider = provider,
                    isSelected = provider.id == selectedProvider?.id,
                    onClick = {
                        setSelectedProvider(provider)
                        dismissSheet()
                    },
                )
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun ProviderSelectionRow(
    provider: Provider,
    isSelected: Boolean,
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Image(
            painter =
                painterResource(
                    id = if (isSelected) R.drawable.ic_radio_selected else R.drawable.ic_radio_unselected,
                ),
            contentDescription = "select provider",
            contentScale = ContentScale.None,
            modifier = Modifier.size(24.dp),
        )
        provider.modelIcon?.let { url ->
            YralAsyncImage(
                imageUrl = getSVGImageModel(url),
                modifier = Modifier.size(36.dp),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
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
            provider.defaultDuration?.let { duration ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .border(
                                width = 1.dp,
                                color = YralColors.Neutral700,
                                shape = RoundedCornerShape(size = 8.dp),
                            ).background(color = YralColors.Neutral900, shape = RoundedCornerShape(size = 8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_timer),
                        contentDescription = null,
                        contentScale = ContentScale.None,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "$duration Sec",
                        style = LocalAppTopography.current.baseRegular,
                        color = YralColors.NeutralTextSecondary,
                    )
                }
            }
        }
    }
}
