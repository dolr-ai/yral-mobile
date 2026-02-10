package com.yral.shared.features.chat.ui.wall

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.yral.shared.analytics.events.InfluencerSource
import com.yral.shared.features.chat.domain.models.ChatError
import com.yral.shared.features.chat.domain.models.Influencer
import com.yral.shared.features.chat.domain.models.InfluencerStatus
import com.yral.shared.features.chat.nav.wall.ChatWallComponent
import com.yral.shared.features.chat.ui.components.ChatErrorBottomSheet
import com.yral.shared.features.chat.viewmodel.ChatWallViewModel
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.chat_wall_coming_soon
import yral_mobile.shared.features.chat.generated.resources.chat_wall_subtitle
import yral_mobile.shared.features.chat.generated.resources.chat_wall_talk_to_me
import yral_mobile.shared.features.chat.generated.resources.chat_wall_title
import yral_mobile.shared.features.chat.generated.resources.error_network_message_influencers
import yral_mobile.shared.features.chat.generated.resources.influencers_error

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatWallScreen(
    component: ChatWallComponent,
    viewModel: ChatWallViewModel,
    modifier: Modifier = Modifier,
) {
    val influencers = viewModel.influencers.collectAsLazyPagingItems()
    var trackedCardsViewed by remember { mutableStateOf(false) }

    var pagingError by remember { mutableStateOf<ChatError?>(null) }
    val errorBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(influencers.loadState) {
        val errorState =
            influencers.loadState.run {
                append as? LoadState.Error
                    ?: prepend as? LoadState.Error
                    ?: refresh as? LoadState.Error
            }
        errorState?.let {
            pagingError =
                ChatError.NetworkError(it.error) {
                    pagingError = null
                    influencers.retry()
                }
        }
    }

    LaunchedEffect(influencers.loadState.refresh, influencers.itemSnapshotList.items.size) {
        val items = influencers.itemSnapshotList.items.filterNotNull()
        if (!trackedCardsViewed && influencers.loadState.refresh is LoadState.NotLoading) {
            trackedCardsViewed = true
            viewModel.trackInfluencerCardsViewed(items)
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
    ) {
        Text(
            text = stringResource(Res.string.chat_wall_title),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.Grey50,
            modifier = Modifier.padding(bottom = 22.dp),
        )
        Text(
            text = stringResource(Res.string.chat_wall_subtitle),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.Grey0,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(influencers.itemCount) { index ->
                influencers[index]?.let { influencer ->
                    InfluencerCard(
                        influencer = influencer,
                        onClick = {
                            viewModel.trackInfluencerCardClicked(influencer, index + 1)
                            component.openConversation(
                                influencer.id,
                                influencer.category,
                                InfluencerSource.CARD,
                            )
                        },
                        style = influencerCardStyles[index % influencerCardStyles.size],
                    )
                }
            }
        }
    }

    pagingError?.let { error ->
        ChatErrorBottomSheet(
            error = error,
            bottomSheetState = errorBottomSheetState,
            onDismissRequest = { pagingError = null },
            description = stringResource(Res.string.error_network_message_influencers),
            errorIllustration = Res.drawable.influencers_error,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun InfluencerCard(
    influencer: Influencer,
    onClick: () -> Unit,
    style: InfluencerCardStyle,
) {
    val cardShape = MaterialTheme.shapes.medium
    val gradientStartTransparent = style.gradientStart.copy(alpha = 0f)
    val isClickable = influencer.status != InfluencerStatus.COMING_SOON

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(ChatWallScreenConstants.CARD_ASPECT_RATIO)
                .clip(cardShape)
                .clickable(
                    enabled = isClickable,
                    onClick = onClick,
                ).background(
                    color = Color.Transparent,
                    shape = cardShape,
                ),
    ) {
        YralAsyncImage(
            imageUrl = influencer.avatarUrl,
            contentScale = ContentScale.Crop,
            shape = cardShape,
            backgroundColor = Color.DarkGray,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(cardShape),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(cardShape),
        ) {
            Spacer(modifier = Modifier.weight(ChatWallScreenConstants.TOP_FILL_WEIGHT))
            Box(
                modifier =
                    Modifier
                        .weight(ChatWallScreenConstants.GRADIENT_WEIGHT)
                        .fillMaxWidth()
                        .background(
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            gradientStartTransparent,
                                            style.gradientEnd,
                                        ),
                                ),
                        ),
            )
            Box(
                modifier =
                    Modifier
                        .weight(ChatWallScreenConstants.SOLID_WEIGHT)
                        .fillMaxWidth()
                        .background(style.solidColor),
            )
        }
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(10.dp),
        ) {
            Text(
                text = influencer.displayName.ifBlank { influencer.name },
                style = LocalAppTopography.current.mdSemiBold,
                color = YralColors.Grey0,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${influencer.name}",
                style = LocalAppTopography.current.smRegular,
                color = YralColors.Grey0,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            Text(
                text = influencer.description,
                style = LocalAppTopography.current.smMedium,
                color = YralColors.Grey0,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (influencer.status == InfluencerStatus.COMING_SOON) {
                Text(
                    text = stringResource(Res.string.chat_wall_coming_soon),
                    style = LocalAppTopography.current.smMedium,
                    color = YralColors.Grey0,
                    maxLines = 1,
                )
            } else {
                YralButton(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    text = stringResource(Res.string.chat_wall_talk_to_me),
                    backgroundColor = YralColors.Grey50,
                    textStyle =
                        LocalAppTopography
                            .current
                            .smSemiBold
                            .copy(
                                color = YralColors.Pink300,
                            ),
                    paddingValues = PaddingValues(vertical = 6.dp),
                    buttonHeight = 26.dp,
                    onClick = onClick,
                )
            }
        }
    }
}

@Suppress("MagicNumber")
object ChatWallScreenConstants {
    const val CARD_ASPECT_RATIO = 0.75f
    const val TOP_FILL_WEIGHT = 40f
    const val GRADIENT_WEIGHT = 40f
    const val SOLID_WEIGHT = 20f
}
