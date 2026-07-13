package com.yral.shared.features.chat.ui.conversation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import coil3.compose.AsyncImage
import com.yral.shared.features.chat.viewmodel.CollageUiState
import com.yral.shared.libs.designsystem.component.YralLoadingDots
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.collage_generating_message
import yral_mobile.shared.features.chat.generated.resources.collage_subscribe_cta
import yral_mobile.shared.features.chat.generated.resources.collage_unavailable
import yral_mobile.shared.libs.designsystem.generated.resources.ic_thunder
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

/**
 * Shimmer bubble shown while POST /request-images is generating (rare cold
 * path takes 45–65 s; the pre-gen'd common case replaces it near-instantly).
 */
@Composable
internal fun CollageGeneratingBubble(influencerName: String) {
    MessageInBubble {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.collage_generating_message, influencerName),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextPrimary,
            )
            YralLoadingDots()
        }
    }
}

/**
 * A collage message. The message itself carries only a reference (collageId
 * when present, plus botId + date — never image URLs) — this
 * bubble asks the ViewModel to resolve fresh image URLs at render time
 * ([onLoad]), so what's shown always matches the CURRENT subscription state:
 * pre-blurred URLs + subscribe CTA for non-subscribers, clear otherwise.
 */
@Composable
internal fun CollageBubble(
    botId: String,
    date: String,
    state: CollageUiState?,
    influencerName: String,
    onLoad: () -> Unit,
    onImageClick: (String) -> Unit,
    onSubscribeClick: () -> Unit,
    maxWidth: Dp,
) {
    LaunchedEffect(botId, date) { onLoad() }
    when (state) {
        is CollageUiState.Ready ->
            CollageGrid(
                images = state.collage.images,
                isBlurred = state.collage.isBlurred,
                influencerName = influencerName,
                onImageClick = onImageClick,
                onSubscribeClick = onSubscribeClick,
                maxWidth = maxWidth,
            )

        CollageUiState.Unavailable ->
            MessageInBubble {
                Text(
                    text = stringResource(Res.string.collage_unavailable),
                    style = LocalAppTopography.current.baseRegular,
                    color = YralColors.NeutralTextSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

        CollageUiState.Loading, null -> CollageLoadingGrid(maxWidth = maxWidth)
    }
}

@Composable
private fun CollageLoadingGrid(maxWidth: Dp) {
    val cellSize = collageCellSize(maxWidth)
    Column(verticalArrangement = Arrangement.spacedBy(COLLAGE_GRID_SPACING)) {
        repeat(COLLAGE_PLACEHOLDER_ROWS) {
            Row(horizontalArrangement = Arrangement.spacedBy(COLLAGE_GRID_SPACING)) {
                repeat(COLLAGE_COLUMNS) {
                    Box(
                        modifier =
                            Modifier
                                .size(cellSize)
                                .clip(RoundedCornerShape(6.dp))
                                .background(YralColors.Neutral900),
                    )
                }
            }
        }
    }
}

@Composable
private fun CollageGrid(
    images: List<String>,
    isBlurred: Boolean,
    influencerName: String,
    onImageClick: (String) -> Unit,
    onSubscribeClick: () -> Unit,
    maxWidth: Dp,
) {
    if (images.isEmpty()) return
    val cellSize = collageCellSize(maxWidth)
    Box {
        Column(verticalArrangement = Arrangement.spacedBy(COLLAGE_GRID_SPACING)) {
            images.chunked(COLLAGE_COLUMNS).forEach { rowImages ->
                Row(horizontalArrangement = Arrangement.spacedBy(COLLAGE_GRID_SPACING)) {
                    rowImages.forEach { imageUrl ->
                        CollageCell(
                            imageUrl = imageUrl,
                            cellSize = cellSize,
                            // Locked grid: taps route to the subscribe CTA, not the preview.
                            onClick = if (isBlurred) null else ({ onImageClick(imageUrl) }),
                        )
                    }
                }
            }
        }
        if (isBlurred) {
            // Images arrive pre-blurred from the server — the scrim just gives
            // the subscribe pill contrast and swallows cell taps.
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = COLLAGE_SCRIM_ALPHA))
                        .clickable(onClick = onSubscribeClick),
                contentAlignment = Alignment.Center,
            ) {
                CollageSubscribePill(
                    influencerName = influencerName,
                    onClick = onSubscribeClick,
                )
            }
        }
    }
}

@Composable
private fun CollageCell(
    imageUrl: String,
    cellSize: Dp,
    onClick: (() -> Unit)?,
) {
    AsyncImage(
        model = rememberChatImageModel(imageUrl),
        contentDescription = "collage image",
        contentScale = ContentScale.Crop,
        onSuccess = { Logger.d("CollageX") { "tile image loaded url=$imageUrl" } },
        onError = { state ->
            Logger.e("CollageX", state.result.throwable) { "tile image FAILED url=$imageUrl" }
        },
        modifier =
            Modifier
                .size(cellSize)
                .clip(RoundedCornerShape(6.dp))
                .background(YralColors.Neutral900)
                .let { base -> if (onClick != null) base.clickable(onClick = onClick) else base },
    )
}

// Mirrors UnlockImagePill (ConversationMessageBubble.kt) — same Yellow400
// pill + thunder icon, but routing to the influencer subscription purchase.
@Composable
private fun CollageSubscribePill(
    influencerName: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.height(SUBSCRIBE_PILL_HEIGHT),
        shape = RoundedCornerShape(4.dp),
        color = YralColors.Yellow400,
        border = BorderStroke(1.dp, YralColors.Yellow200),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = SUBSCRIBE_PILL_HORIZONTAL_PADDING),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.collage_subscribe_cta, influencerName),
                style = LocalAppTopography.current.regSemiBold,
                color = YralColors.Yellow200,
            )
            Image(
                painter = painterResource(DesignRes.drawable.ic_thunder),
                contentDescription = null,
                contentScale = ContentScale.Inside,
                modifier = Modifier.size(SUBSCRIBE_PILL_ICON_SIZE),
            )
        }
    }
}

private fun collageCellSize(maxWidth: Dp): Dp = (maxWidth - COLLAGE_GRID_SPACING) / COLLAGE_COLUMNS

private const val COLLAGE_COLUMNS = 2
private const val COLLAGE_PLACEHOLDER_ROWS = 2
private val COLLAGE_GRID_SPACING = 4.dp
private const val COLLAGE_SCRIM_ALPHA = 0.4f
private val SUBSCRIBE_PILL_HEIGHT = 40.dp
private val SUBSCRIBE_PILL_HORIZONTAL_PADDING = 12.dp
private val SUBSCRIBE_PILL_ICON_SIZE = 14.dp
