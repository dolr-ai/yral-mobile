package com.yral.shared.features.profile.ui.followers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.yral.shared.core.utils.resolveUsername
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.rust.service.domain.models.FollowerItem
import com.yral.shared.rust.service.domain.models.PagedFollowerItem
import com.yral.shared.rust.service.utils.propicFromPrincipal
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.profile.generated.resources.Res
import yral_mobile.shared.features.profile.generated.resources.followers_empty_list
import yral_mobile.shared.features.profile.generated.resources.followers_load_error
import yral_mobile.shared.features.profile.generated.resources.following_empty_list
import yral_mobile.shared.libs.designsystem.generated.resources.follow
import yral_mobile.shared.libs.designsystem.generated.resources.followers
import yral_mobile.shared.libs.designsystem.generated.resources.following
import yral_mobile.shared.libs.designsystem.generated.resources.try_again
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

object FollowersSheetUi {
    const val MIN_HEIGHT_FRACTION = 0.35f
    const val EXPANDED_HEIGHT_FRACTION = 0.9f
    const val TAB_UNSELECTED_ALPHA = 0.6f
    const val TAB_WIDTH_FRACTION = 0.5f
    val DragHandleTopSpacing = 12.dp
    val DragHandleWidth = 32.dp
    val DragHandleHeight = 2.dp
    val DragHandleCornerRadius = 50.dp
    val HorizontalPadding = 16.dp
    val NameTopSpacing = 28.dp
    val TabsTopSpacing = 28.dp
    val SeparatorTopSpacing = 12.dp
    val SeparatorHeight = 1.dp
    val ListTopSpacing = 36.dp
    val ListItemSpacing = 16.dp
    val AvatarSize = 42.dp
    val AvatarInitialFontSize = 16.sp
    val ItemPaddingHorizontal = 16.dp
    val ItemPaddingVertical = 12.dp
    val ActionButtonWidth = 118.dp
    val ActionButtonHeight = 36.dp
    val RowCornerRadius = 12.dp
}

enum class FollowersSheetTab {
    Followers,
    Following,
}

@Suppress("LongMethod")
@Composable
fun FollowersBottomSheet(
    username: String,
    initialTab: FollowersSheetTab,
    followers: LazyPagingItems<PagedFollowerItem>,
    following: LazyPagingItems<PagedFollowerItem>,
    minSheetHeight: Dp,
    maxSheetHeight: Dp,
    followLoading: Map<String, Boolean>,
    onTabSelected: (FollowersSheetTab) -> Unit,
    onFollowToggle: (String, Boolean) -> Unit,
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val pagingItems = if (selectedTab == FollowersSheetTab.Followers) followers else following

    LaunchedEffect(selectedTab) {
        if (selectedTab == FollowersSheetTab.Followers) {
            followers.refresh()
        } else {
            following.refresh()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .heightIn(min = minSheetHeight, max = maxSheetHeight)
                .padding(horizontal = FollowersSheetUi.HorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(FollowersSheetUi.NameTopSpacing))
        Text(
            text = username,
            modifier = Modifier.fillMaxWidth(),
            style =
                LocalAppTopography.current.lgBold.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
            color = YralColors.NeutralTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(FollowersSheetUi.TabsTopSpacing))
        FollowersTabRow(
            selectedTab = selectedTab,
            onTabSelected = {
                selectedTab = it
                onTabSelected(it)
            },
        )
        Spacer(modifier = Modifier.height(FollowersSheetUi.ListTopSpacing))
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(FollowersSheetUi.ListItemSpacing),
        ) {
            when (pagingItems.loadState.refresh) {
                is LoadState.Loading -> item { FollowersLoadingState() }
                is LoadState.Error ->
                    item {
                        FollowersErrorState(onRetry = { pagingItems.retry() })
                    }

                is LoadState.NotLoading -> {
                    if (pagingItems.itemCount == 0) {
                        item { FollowersEmptyState(selectedTab) }
                    } else {
                        for (pageIndex in 0 until pagingItems.itemCount) {
                            val page = pagingItems[pageIndex]
                            if (page != null) {
                                items(
                                    items = page.items,
                                    key = { follower ->
                                        toReadablePrincipalText(follower.principalId.toString()) + "-$pageIndex"
                                    },
                                    contentType = { _ -> "FollowerItem" },
                                ) { follower ->
                                    FollowerRow(
                                        follower = follower,
                                        followLoading = followLoading,
                                        onFollowToggle = onFollowToggle,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                FollowersPagingIndicator(
                    loadState = pagingItems.loadState.append,
                    onRetry = pagingItems::retry,
                )
            }
        }
    }
}

@Composable
private fun FollowersLoadingState() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        YralLoader(size = 32.dp)
    }
}

@Composable
private fun FollowersErrorState(onRetry: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.followers_load_error),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
        )
        YralGradientButton(
            modifier = Modifier.width(160.dp),
            text = stringResource(DesignRes.string.try_again),
            buttonHeight = 36.dp,
            fillMaxWidth = false,
            onClick = onRetry,
        )
    }
}

@Composable
private fun FollowersEmptyState(tab: FollowersSheetTab) {
    val message =
        when (tab) {
            FollowersSheetTab.Followers -> stringResource(Res.string.followers_empty_list)
            FollowersSheetTab.Following -> stringResource(Res.string.following_empty_list)
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FollowersTabRow(
    selectedTab: FollowersSheetTab,
    onTabSelected: (FollowersSheetTab) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FollowersSheetUi.SeparatorTopSpacing),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            FollowersTabItem(
                modifier = Modifier.align(Alignment.CenterStart),
                text = stringResource(DesignRes.string.followers),
                isSelected = selectedTab == FollowersSheetTab.Followers,
                onClick = { onTabSelected(FollowersSheetTab.Followers) },
            )
            FollowersTabItem(
                modifier = Modifier.align(Alignment.CenterEnd),
                text = stringResource(DesignRes.string.following),
                isSelected = selectedTab == FollowersSheetTab.Following,
                onClick = { onTabSelected(FollowersSheetTab.Following) },
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(FollowersSheetUi.SeparatorHeight)
                    .background(YralColors.Neutral700),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(FollowersSheetUi.TAB_WIDTH_FRACTION)
                        .height(FollowersSheetUi.SeparatorHeight)
                        .align(
                            if (selectedTab == FollowersSheetTab.Followers) {
                                Alignment.CenterStart
                            } else {
                                Alignment.CenterEnd
                            },
                        ).background(YralColors.Pink300),
            )
        }
    }
}

@Composable
private fun FollowersTabItem(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth(FollowersSheetUi.TAB_WIDTH_FRACTION)
                .then(modifier)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = LocalAppTopography.current.baseRegular.copy(fontSize = 14.sp),
            color =
                if (isSelected) {
                    YralColors.NeutralTextPrimary
                } else {
                    YralColors.NeutralTextPrimary.copy(alpha = FollowersSheetUi.TAB_UNSELECTED_ALPHA)
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun FollowerRow(
    follower: FollowerItem,
    followLoading: Map<String, Boolean>,
    onFollowToggle: (String, Boolean) -> Unit,
) {
    val principalText =
        remember(follower.principalId) { toReadablePrincipalText(follower.principalId.toString()) }
    val displayName =
        remember(principalText) { resolveUsername(null, principalText) ?: principalText }
    val isFollowing = follower.callerFollows
    val avatarUrl =
        remember(follower.profilePictureUrl, principalText) {
            follower.profilePictureUrl?.takeIf { it.isNotBlank() } ?: propicFromPrincipal(
                principalText,
            )
        }
    val isLoading = followLoading[principalText] == true

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(FollowersSheetUi.RowCornerRadius))
                .background(YralColors.Neutral900)
                .padding(
                    horizontal = FollowersSheetUi.ItemPaddingHorizontal,
                    vertical = FollowersSheetUi.ItemPaddingVertical,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FollowersSheetUi.ItemPaddingVertical),
        ) {
            YralAsyncImage(
                imageUrl = avatarUrl,
                modifier =
                    Modifier
                        .size(FollowersSheetUi.AvatarSize)
                        .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = displayName,
                style = LocalAppTopography.current.baseSemiBold.copy(fontSize = 14.sp),
                color = YralColors.NeutralTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isFollowing) {
            YralButton(
                text = stringResource(DesignRes.string.following),
                modifier =
                    Modifier
                        .width(FollowersSheetUi.ActionButtonWidth)
                        .height(FollowersSheetUi.ActionButtonHeight),
                backgroundColor = YralColors.Neutral800,
                borderColor = YralColors.Neutral700,
                textStyle =
                    LocalAppTopography.current.baseSemiBold.copy(
                        fontSize = 14.sp,
                        color = YralColors.Neutral50,
                    ),
                buttonHeight = FollowersSheetUi.ActionButtonHeight,
                fillMaxWidth = false,
                buttonState = if (isLoading) YralButtonState.Loading else YralButtonState.Enabled,
                onClick = { if (!isLoading) onFollowToggle(principalText, true) },
            )
        } else {
            YralGradientButton(
                modifier =
                    Modifier
                        .width(FollowersSheetUi.ActionButtonWidth)
                        .height(FollowersSheetUi.ActionButtonHeight),
                buttonHeight = FollowersSheetUi.ActionButtonHeight,
                fillMaxWidth = false,
                text = stringResource(DesignRes.string.follow),
                buttonState = if (isLoading) YralButtonState.Loading else YralButtonState.Enabled,
                onClick = { if (!isLoading) onFollowToggle(principalText, false) },
            )
        }
    }
}

@Composable
private fun FollowersPagingIndicator(
    loadState: LoadState,
    onRetry: () -> Unit,
) {
    when (loadState) {
        is LoadState.Loading -> FollowersLoadingState()
        is LoadState.Error -> FollowersErrorState(onRetry = onRetry)
        is LoadState.NotLoading -> Unit
    }
}

private fun toReadablePrincipalText(raw: String): String =
    raw
        .removePrefix("Principal(")
        .removeSuffix(")")
        .removePrefix("text=")
