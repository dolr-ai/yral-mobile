package com.yral.shared.libs.videoPlayer.cardstack

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yral.shared.libs.videoPlayer.model.PlayerData
import com.yral.shared.libs.videoplayback.PlaybackCoordinator
import com.yral.shared.libs.videoplayback.ui.VideoSurfaceSlot
import com.yral.shared.libs.videoplayback.ui.VideoSurfaceType
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.libs.videoplayer.generated.resources.Res
import yral_mobile.shared.libs.videoplayer.generated.resources.bakwaas
import yral_mobile.shared.libs.videoplayer.generated.resources.mast

/**
 * Reel card content rendered inside the generic card stack.
 *
 * @param playerData Data for the video player.
 * @param coordinator Playback coordinator for video surfaces.
 * @param mediaIndex Index of this media item in the coordinator feed.
 * @param isFrontCard Whether this card is the front card.
 * @param swipeDirection Current swipe direction for feedback overlays.
 * @param swipeProgress Current swipe progress (0..1+).
 * @param modifier Modifier for the card.
 * @param overlayContent Content to overlay on the video (UI controls, etc.).
 */
@Composable
internal fun ReelCardContent(
    playerData: PlayerData,
    coordinator: PlaybackCoordinator,
    mediaIndex: Int,
    isFrontCard: Boolean,
    swipeDirection: SwipeDirection,
    swipeProgress: Float,
    suppressShutter: Boolean,
    showPlaceholderOverlay: Boolean,
    showSwipeOverlay: Boolean,
    modifier: Modifier = Modifier,
    overlayContent: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize(),
        contentAlignment = Alignment.TopStart,
    ) {
        VideoSurfaceSlot(
            index = mediaIndex,
            coordinator = coordinator,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            surfaceType = VideoSurfaceType.TextureView,
            shutter = {
                if (!suppressShutter) {
                    AsyncImage(
                        model = playerData.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            },
        )

        if (showPlaceholderOverlay) {
            AsyncImage(
                model = playerData.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // Swipe feedback overlay for front card only
        if (isFrontCard && showSwipeOverlay) {
            SwipeFeedbackOverlay(
                direction = swipeDirection,
                progress = swipeProgress,
                modifier = Modifier.fillMaxSize(),
            )

            // Swipe animation icons - show based on swipe direction
            val iconAlpha = (swipeProgress * 2f).coerceIn(0f, 1f) // Fade in as swipe progresses

            // MAST animation icon on TOP LEFT when swiping RIGHT (24dp below trophy icon)
            if (swipeDirection == SwipeDirection.RIGHT) {
                Image(
                    painter = painterResource(Res.drawable.mast),
                    contentDescription = "Mast Animation",
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 40.dp, top = 88.dp)
                            .size(width = 171.dp, height = 165.dp)
                            .alpha(iconAlpha),
                )
            }

            // BAKWAAS animation icon on TOP RIGHT when swiping LEFT (24dp below wallet icon)
            if (swipeDirection == SwipeDirection.LEFT) {
                Image(
                    painter = painterResource(Res.drawable.bakwaas),
                    contentDescription = "Bakwaas Animation",
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 40.dp, top = 88.dp)
                            .size(width = 206.dp, height = 115.dp)
                            .alpha(iconAlpha),
                )
            }
        }

        // UI overlay content - show on all cards for pre-loading
        overlayContent()
    }
}
