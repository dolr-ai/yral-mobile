package com.yral.shared.features.chat.ui.conversation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.component.YralLoadingDots
import com.yral.shared.libs.designsystem.component.getLocalImageModel
import com.yral.shared.libs.designsystem.theme.AppTopography
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.blurred_image_unlock_cta
import yral_mobile.shared.features.chat.generated.resources.message_failed_tap_to_resend
import yral_mobile.shared.libs.designsystem.generated.resources.ic_bubble_tip_black
import yral_mobile.shared.libs.designsystem.generated.resources.ic_bubble_tip_pink
import yral_mobile.shared.libs.designsystem.generated.resources.ic_exclamation_circle
import yral_mobile.shared.libs.designsystem.generated.resources.ic_thunder
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
internal fun MessageContent(
    isUser: Boolean,
    content: String?,
    mediaUrls: List<String>,
    maxWidth: Dp,
    onImageClick: ((String) -> Unit)? = null,
    isFailed: Boolean = false,
    isWaiting: Boolean = false,
    isStreaming: Boolean = false,
    markdownLockedOverride: Boolean? = null,
    onRetry: (() -> Unit)? = null,
    isBlurred: Boolean = false,
    onUnlockClick: (() -> Unit)? = null,
) {
    MessageBubble(
        content = content,
        mediaUrls = mediaUrls,
        isUser = isUser,
        maxWidth = maxWidth,
        onImageClick = onImageClick,
        isFailed = isFailed,
        isWaiting = isWaiting,
        isStreaming = isStreaming,
        markdownLockedOverride = markdownLockedOverride,
        onRetry = onRetry,
        isBlurred = isBlurred,
        onUnlockClick = onUnlockClick,
    )
}

@Composable
internal fun MessageBubble(
    content: String?,
    mediaUrls: List<String>,
    isUser: Boolean,
    maxWidth: Dp,
    onImageClick: ((String) -> Unit)?,
    isFailed: Boolean,
    isWaiting: Boolean,
    isStreaming: Boolean,
    markdownLockedOverride: Boolean?,
    onRetry: (() -> Unit)?,
    isBlurred: Boolean,
    onUnlockClick: (() -> Unit)?,
) {
    val baseModifier = Modifier.widthIn(max = maxWidth)
    val clickableModifier =
        if (onRetry != null) {
            baseModifier.clickable(onClick = onRetry)
        } else {
            baseModifier
        }

    Box(modifier = clickableModifier) {
        if (isWaiting && !isUser) {
            WaitingBubble()
        } else {
            val messageImageClickHandler = if (onRetry == null) onImageClick else null
            RegularBubble(
                content = content,
                mediaUrls = mediaUrls,
                isUser = isUser,
                onImageClick = messageImageClickHandler,
                maxWidth = maxWidth,
                isFailed = isFailed,
                isStreaming = isStreaming,
                markdownLockedOverride = markdownLockedOverride,
                isBlurred = isBlurred,
                onUnlockClick = onUnlockClick,
            )
        }
    }
}

@Composable
private fun WaitingBubble() {
    MessageInBubble {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            YralLoadingDots()
        }
    }
}

@Composable
private fun RegularBubble(
    content: String?,
    mediaUrls: List<String>,
    isUser: Boolean,
    onImageClick: ((String) -> Unit)?,
    maxWidth: Dp,
    isFailed: Boolean,
    isStreaming: Boolean = false,
    markdownLockedOverride: Boolean? = null,
    isBlurred: Boolean = false,
    onUnlockClick: (() -> Unit)? = null,
) {
    // Phase 5c rendering contract (do not regress):
    //   1. Path lock — `markdownLockedOverride` pins Markdown vs Text for the full
    //      lifetime of a streamed message. Streaming uses the locked path and the
    //      post-`done` Remote uses the same path. No mid-stream or done-time swap.
    //   2. Waiting state — when isStreaming is true but no content has arrived
    //      yet, show the same YralLoadingDots used by WaitingBubble. Once content
    //      starts streaming in, NO trailing cursor or indicator is rendered — the
    //      live token text alone is the signal that the reply is still arriving.
    //   3. Coalescing — the VM batches tokens that arrive within ~250ms before
    //      pushing them to the streaming buffer, so this composable's content
    //      changes ~1-3 times per reply instead of once per network token.
    val displayContent = content.orEmpty()
    val onlyMedia = content.isNullOrBlank() && !isStreaming
    val appTypography = LocalAppTopography.current
    val textColor = YralColors.NeutralTextPrimary

    val markdownColors = markDownColors(textColor)
    val markdownTypography = markdownTypography(appTypography)

    val useMarkdown =
        when {
            markdownLockedOverride != null -> markdownLockedOverride
            !content.isNullOrBlank() && content.shouldRenderAsMarkdown() -> true
            else -> false
        }

    MessageInBubble(
        isUser = isUser,
        isFailed = isFailed,
        isOnlyMedia = onlyMedia,
    ) {
        if (displayContent.isNotBlank()) {
            if (useMarkdown) {
                Markdown(
                    content = displayContent,
                    colors = markdownColors,
                    typography = markdownTypography,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            } else {
                Text(
                    text = displayContent,
                    style = appTypography.baseRegular,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
        }
        if (isStreaming && displayContent.isBlank()) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                YralLoadingDots()
            }
        }
        MessageImages(
            mediaUrls = mediaUrls,
            onImageClick = onImageClick,
            maxWidth = maxWidth,
            isBlurred = isBlurred,
            onUnlockClick = onUnlockClick,
        )
    }
}

@Composable
private fun markdownTypography(appTypography: AppTopography): DefaultMarkdownTypography =
    remember(appTypography) {
        DefaultMarkdownTypography(
            h1 = appTypography.xxlBold,
            h2 = appTypography.xlBold,
            h3 = appTypography.lgBold,
            h4 = appTypography.mdBold,
            h5 = appTypography.baseBold,
            h6 = appTypography.smSemiBold,
            text = appTypography.baseRegular,
            code = appTypography.smRegular,
            inlineCode = appTypography.smRegular,
            quote = appTypography.baseRegular,
            paragraph = appTypography.baseRegular,
            ordered = appTypography.baseRegular,
            bullet = appTypography.baseRegular,
            list = appTypography.baseRegular,
            textLink =
                TextLinkStyles(
                    style =
                        SpanStyle(
                            fontSize = appTypography.baseRegular.fontSize,
                            fontFamily = appTypography.baseRegular.fontFamily,
                            fontWeight = appTypography.baseRegular.fontWeight,
                            color = YralColors.BlueTextPrimary,
                        ),
                ),
            table = appTypography.baseRegular,
        )
    }

@Composable
private fun markDownColors(textColor: Color): DefaultMarkdownColors =
    remember(textColor) {
        DefaultMarkdownColors(
            text = textColor,
            codeBackground = YralColors.Neutral900,
            inlineCodeBackground = textColor,
            dividerColor = YralColors.Divider,
            tableBackground = YralColors.Neutral900,
        )
    }

@Composable
private fun MessageInBubble(
    isUser: Boolean = false,
    isFailed: Boolean = false,
    isOnlyMedia: Boolean = false,
    content: @Composable () -> Unit,
) {
    val backgroundColor =
        when {
            isOnlyMedia -> Color.Transparent
            isUser -> YralColors.Pink300
            else -> YralColors.Neutral900
        }
    val bubbleTipRes = if (isUser) DesignRes.drawable.ic_bubble_tip_pink else DesignRes.drawable.ic_bubble_tip_black
    val bubbleTipAlignment = if (isUser) BottomEnd else BottomStart
    val bubblePadding: PaddingValues =
        when {
            isOnlyMedia -> PaddingValues(0.dp)
            isUser -> PaddingValues(end = 9.dp)
            else -> PaddingValues(start = 9.dp)
        }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.End,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isFailed) {
                Image(
                    painter = painterResource(DesignRes.drawable.ic_exclamation_circle),
                    modifier = Modifier.size(24.dp),
                    contentDescription = stringResource(Res.string.message_failed_tap_to_resend),
                )
            }
            Box {
                if (!isOnlyMedia) {
                    Image(
                        painter = painterResource(bubbleTipRes),
                        modifier = Modifier.align(bubbleTipAlignment),
                        contentDescription = null,
                    )
                }
                Column(
                    modifier =
                        Modifier
                            .padding(bubblePadding)
                            .background(
                                color = backgroundColor,
                                shape = RoundedCornerShape(6.dp),
                            ),
                ) {
                    content()
                }
            }
        }
        if (isFailed) {
            Text(
                text = stringResource(Res.string.message_failed_tap_to_resend),
                style = LocalAppTopography.current.regRegular,
                color = YralColors.RedButtonPrimary,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun MessageImages(
    mediaUrls: List<String>,
    onImageClick: ((String) -> Unit)?,
    maxWidth: Dp,
    isBlurred: Boolean = false,
    onUnlockClick: (() -> Unit)? = null,
) {
    if (mediaUrls.isEmpty()) return

    // If you ever need better perf, switch to LazyRow/Column or a grid.
    mediaUrls.forEach { imageUrl ->
        ChatMessageImage(
            imageUrl = imageUrl,
            onImageClick = onImageClick,
            maxWidth = maxWidth,
            isBlurred = isBlurred,
            onUnlockClick = onUnlockClick,
        )
    }
}

@Composable
private fun ChatMessageImage(
    imageUrl: String,
    onImageClick: ((String) -> Unit)?,
    maxWidth: Dp,
    isBlurred: Boolean = false,
    onUnlockClick: (() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val imageModel = rememberChatImageModel(imageUrl)
    var imageAspectRatio by remember(imageUrl) { mutableStateOf(CHAT_MEDIA_DEFAULT_ASPECT_RATIO) }
    var isLoading by remember(imageUrl) { mutableStateOf(true) }
    val imageContainerSize =
        with(density) {
            resolveChatMediaContainerSize(
                maxWidthPx = maxWidth.toPx(),
                imageAspectRatio = imageAspectRatio,
                minHeightPx = CHAT_MEDIA_MIN_HEIGHT.toPx(),
                maxHeightPx = CHAT_MEDIA_MAX_HEIGHT.toPx(),
            )
        }
    val imageModifier =
        with(density) {
            Modifier
                .width(imageContainerSize.widthPx.toDp())
                .height(imageContainerSize.heightPx.toDp())
        }.clip(RoundedCornerShape(6.dp))
            .let { baseModifier ->
                when {
                    // Locked image: no full-screen preview — tapping anywhere
                    // routes to the unlock flow instead.
                    isBlurred -> baseModifier.clickable { onUnlockClick?.invoke() }
                    onImageClick != null -> baseModifier.clickable { onImageClick(imageUrl) }
                    else -> baseModifier
                }
            }

    Box(
        modifier = imageModifier,
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = "image",
            modifier =
                Modifier.fillMaxSize().let { baseModifier ->
                    if (isBlurred) baseModifier.blur(BLURRED_IMAGE_BLUR_RADIUS) else baseModifier
                },
            contentScale = ContentScale.Fit,
            onState = { state ->
                isLoading = state !is AsyncImagePainter.State.Success && state !is AsyncImagePainter.State.Error
                if (state is AsyncImagePainter.State.Success) {
                    resolveChatMediaAspectRatio(
                        imageWidthPx = state.painter.intrinsicSize.width,
                        imageHeightPx = state.painter.intrinsicSize.height,
                    )?.let { imageAspectRatio = it }
                }
            },
        )

        if (isLoading) {
            YralLoader()
        }

        if (isBlurred) {
            // Scrim doubles as the hide layer on platforms where Modifier.blur
            // is a no-op (Android < 12) and gives the pill contrast elsewhere.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = BLURRED_IMAGE_SCRIM_ALPHA)),
                contentAlignment = Alignment.Center,
            ) {
                UnlockImagePill(onClick = { onUnlockClick?.invoke() })
            }
        }
    }
}

// Mirrors the SubscribeButton pill in the design system (AccountInfoView.kt):
// Yellow400 fill, Yellow200 border/text, thunder icon.
@Composable
private fun UnlockImagePill(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.height(UNLOCK_PILL_HEIGHT),
        shape = RoundedCornerShape(4.dp),
        color = YralColors.Yellow400,
        border = BorderStroke(1.dp, YralColors.Yellow200),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = UNLOCK_PILL_HORIZONTAL_PADDING),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.blurred_image_unlock_cta),
                style = LocalAppTopography.current.regSemiBold,
                color = YralColors.Yellow200,
            )
            Image(
                painter = painterResource(DesignRes.drawable.ic_thunder),
                contentDescription = null,
                contentScale = ContentScale.Inside,
                modifier = Modifier.size(UNLOCK_PILL_ICON_SIZE),
            )
        }
    }
}

@Composable
internal fun rememberChatImageModel(imageUrl: String): Any {
    val localFilePath = localChatImageFilePathOrNull(imageUrl)
    return localFilePath?.let { getLocalImageModel(it) } ?: imageUrl
}

internal fun localChatImageFilePathOrNull(imageUrl: String): String? =
    when {
        imageUrl.startsWith(FILE_URL_PREFIX) -> imageUrl.removePrefix(FILE_URL_PREFIX).takeIf { it.isNotBlank() }
        imageUrl.startsWith(ABSOLUTE_PATH_PREFIX) -> imageUrl
        else -> null
    }

internal fun String.shouldRenderAsMarkdown(): Boolean = all { it.code <= ASCII_MAX_CODE }

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun BlurredChatMessageImagePreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        ChatMessageImage(
            imageUrl = "https://example.com/image.jpg",
            onImageClick = null,
            maxWidth = 280.dp,
            isBlurred = true,
            onUnlockClick = {},
        )
    }
}

private const val FILE_URL_PREFIX = "file://"
private const val ABSOLUTE_PATH_PREFIX = "/"
private const val ASCII_MAX_CODE = 0x7F
private val BLURRED_IMAGE_BLUR_RADIUS = 16.dp
private const val BLURRED_IMAGE_SCRIM_ALPHA = 0.4f
private val UNLOCK_PILL_HEIGHT = 40.dp
private val UNLOCK_PILL_HORIZONTAL_PADDING = 12.dp
private val UNLOCK_PILL_ICON_SIZE = 14.dp
