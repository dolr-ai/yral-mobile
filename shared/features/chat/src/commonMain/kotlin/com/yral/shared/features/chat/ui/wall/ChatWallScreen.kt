package com.yral.shared.features.chat.ui.wall

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.pullToRefreshIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.yral.shared.data.domain.models.ConversationInfluencerSource
import com.yral.shared.data.domain.models.OpenConversationParams
import com.yral.shared.features.chat.domain.models.ChatError
import com.yral.shared.features.chat.domain.models.Influencer
import com.yral.shared.features.chat.domain.models.InfluencerStatus
import com.yral.shared.features.chat.nav.wall.ChatWallComponent
import com.yral.shared.features.chat.ui.components.ChatErrorBottomSheet
import com.yral.shared.features.chat.viewmodel.ChatWallViewModel
import com.yral.shared.libs.designsystem.component.YralGridImage
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.component.formatAbbreviation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.error_network_message_influencers
import yral_mobile.shared.features.chat.generated.resources.ic_chat_bubble
import yral_mobile.shared.features.chat.generated.resources.influencers_error

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
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

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        when (influencers.loadState.refresh) {
            is LoadState.Loading ->
                if (influencers.itemCount == 0) {
                    ChatWallLoadingState()
                } else {
                    ChatWallContentWithPullToRefresh(
                        influencers = influencers,
                        component = component,
                        viewModel = viewModel,
                    )
                }
            is LoadState.Error ->
                ChatWallContentWithPullToRefresh(
                    influencers = influencers,
                    component = component,
                    viewModel = viewModel,
                )
            is LoadState.NotLoading ->
                ChatWallContentWithPullToRefresh(
                    influencers = influencers,
                    component = component,
                    viewModel = viewModel,
                )
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
) {
    val cardShape = MaterialTheme.shapes.medium
    val isClickable = influencer.status != InfluencerStatus.COMING_SOON

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(ChatWallScreenConstants.CARD_ASPECT_RATIO)
                .clip(cardShape)
                .background(color = YralColors.Neutral800, shape = cardShape)
                .clickable(
                    enabled = isClickable,
                    onClick = onClick,
                ),
    ) {
        // Using YralGridImage - optimized for scrolling performance
        YralGridImage(
            imageUrl = influencer.avatarUrl,
            contentScale = ContentScale.Crop,
            shape = cardShape,
            backgroundColor = Color.DarkGray,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(6.dp),
        )
        InfluencerCardContent(
            influencer = influencer,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .clickable(onClick = onClick),
        )
    }
}

@Composable
private fun InfluencerCardContent(
    influencer: Influencer,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val displayNameText =
            remember(influencer.displayName, influencer.name) {
                influencer.displayName.ifBlank { influencer.name }
            }
        val messageCountText = influencer.messageCount?.let { formatAbbreviation(it, 1) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = displayNameText,
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.YellowTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (messageCountText != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = messageCountText,
                        style = LocalAppTopography.current.regMedium,
                        color = YralColors.NeutralTextPrimary,
                    )
                    Icon(
                        painter = painterResource(Res.drawable.ic_chat_bubble),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Text(
            text = influencer.description,
            style = LocalAppTopography.current.regRegular,
            color = YralColors.Neutral300,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ChatWallLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        YralLoader(size = 60.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatWallContentWithPullToRefresh(
    influencers: LazyPagingItems<Influencer>,
    component: ChatWallComponent,
    viewModel: ChatWallViewModel,
) {
    val pullRefreshState = rememberPullToRefreshState()
    val offset =
        pullRefreshState.distanceFraction *
            ChatWallScreenConstants.PULL_TO_REFRESH_INDICATOR_SIZE *
            ChatWallScreenConstants.PULL_TO_REFRESH_OFFSET_MULTIPLIER
    val isRefreshing = influencers.loadState.refresh is LoadState.Loading

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { influencers.refresh() },
        state = pullRefreshState,
        indicator = {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .pullToRefreshIndicator(
                            state = pullRefreshState,
                            isRefreshing = isRefreshing,
                            containerColor = Color.Transparent,
                            threshold = ChatWallScreenConstants.PULL_TO_REFRESH_THRESHOLD.dp,
                            elevation = 0.dp,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                YralLoader(size = ChatWallScreenConstants.PULL_TO_REFRESH_INDICATOR_SIZE.dp)
            }
        },
    ) {
        ChatWallGridContent(
            influencers = influencers,
            component = component,
            viewModel = viewModel,
            contentOffsetY = offset.dp,
        )
    }
}

@Composable
private fun ChatWallGridContent(
    influencers: LazyPagingItems<Influencer>,
    component: ChatWallComponent,
    viewModel: ChatWallViewModel,
    contentOffsetY: Dp,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .offset(y = contentOffsetY)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(
                count = influencers.itemCount,
                key = { index -> influencers.peek(index)?.id ?: "placeholder-$index" },
                contentType = { "influencer_card" },
            ) { index ->
                influencers[index]?.let { influencer ->
                    InfluencerCard(
                        influencer = influencer,
                        onClick = {
                            viewModel.trackInfluencerCardClicked(influencer, index + 1)
                            component.openConversation(
                                OpenConversationParams(
                                    influencerId = influencer.id,
                                    influencerCategory = influencer.category,
                                    influencerSource = ConversationInfluencerSource.CARD,
                                ),
                            )
                        },
                    )
                }
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
    const val PULL_TO_REFRESH_INDICATOR_SIZE = 34f
    const val PULL_TO_REFRESH_THRESHOLD = 36f
    const val PULL_TO_REFRESH_OFFSET_MULTIPLIER = 1.5f
}
