package com.yral.android.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.feed.FeedScreenConstants.VIDEO_REPORT_SHEET_MAX_HEIGHT
import com.yral.android.ui.widgets.YralBottomSheet
import com.yral.shared.features.game.domain.models.AboutGameBodyType
import com.yral.shared.features.game.domain.models.AboutGameItem
import com.yral.shared.features.game.domain.models.AboutGameItemBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutGameSheet(
    gameRules: List<AboutGameItem>,
    onDismissRequest: () -> Unit,
) {
    val bottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    YralBottomSheet(
        onDismissRequest = onDismissRequest,
        bottomSheetState = bottomSheetState,
        dragHandle = {
            DragHandle(
                color = YralColors.Neutral500,
            )
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxHeight(VIDEO_REPORT_SHEET_MAX_HEIGHT)
                    .padding(
                        horizontal = 16.dp,
                        vertical = 28.dp,
                    ),
        ) {
            Text(
                text = stringResource(id = R.string.about_game),
                style = LocalAppTopography.current.lgBold,
            )
            LazyColumn {
                items(gameRules.size) { index ->
                    AboutGameItem(gameRules[index])
                }
            }
        }
    }
}

@Composable
private fun AboutGameItem(rule: AboutGameItem) {
    Column(
        modifier =
            Modifier
                .background(
                    color = YralColors.NeutralBackgroundCardBackground,
                    shape = RoundedCornerShape(size = 8.dp),
                ).padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.Start,
    ) {
        AboutGameItemTitle(rule)
        AboutGameItemRule(rule)
    }
}

@Composable
private fun AboutGameItemTitle(rule: AboutGameItem) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = rule.thumbnailUrl,
            modifier =
                Modifier
                    .padding(0.75.dp)
                    .size(36.dp),
            contentDescription = "rule icon",
        )
        Text(
            text = rule.name,
            style = LocalAppTopography.current.mdMedium,
        )
    }
}

@Composable
private fun AboutGameItemRule(rule: AboutGameItem) {
    Column(
        modifier =
            Modifier
                .background(
                    color = YralColors.NeutralBackgroundCardBackground,
                    shape = RoundedCornerShape(size = 8.dp),
                ).padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        rule.body.forEach { bodyItem ->
            if (bodyItem.type == AboutGameBodyType.TEXT) {
                Text(
                    text = getAnnotatedString(bodyItem),
                )
            } else if (bodyItem.type == AboutGameBodyType.IMAGES) {
                Row(
                    modifier =
                        Modifier
                            .padding(start = 6.dp, top = 2.dp, end = 6.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    bodyItem.imageUrls?.forEach { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            modifier =
                                Modifier
                                    .size(23.33.dp),
                            contentDescription = "game icon",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getAnnotatedString(body: AboutGameItemBody) =
    buildAnnotatedString {
        val textStyle = LocalAppTopography.current.baseMedium
        val spanStyle =
            SpanStyle(
                fontSize = textStyle.fontSize,
                fontFamily = textStyle.fontFamily,
                fontWeight = textStyle.fontWeight,
            )
        body.content?.forEachIndexed { index, text ->
            val color =
                body.colors?.get(index)?.let {
                    YralColors.getColorFromHex(it)
                } ?: YralColors.Grey50
            withStyle(
                style = spanStyle.copy(color = color),
            ) {
                append(text)
            }
        }
    }
