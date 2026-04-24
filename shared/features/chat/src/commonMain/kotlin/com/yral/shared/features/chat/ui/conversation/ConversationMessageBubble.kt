package com.yral.shared.features.chat.ui.conversation

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Modifier
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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.message_failed_tap_to_resend
import yral_mobile.shared.libs.designsystem.generated.resources.ic_bubble_tip_black
import yral_mobile.shared.libs.designsystem.generated.resources.ic_bubble_tip_pink
import yral_mobile.shared.libs.designsystem.generated.resources.ic_exclamation_circle
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
    onRetry: (() -> Unit)? = null,
) {
    MessageBubble(
        content = content,
        mediaUrls = mediaUrls,
        isUser = isUser,
        maxWidth = maxWidth,
        onImageClick = onImageClick,
        isFailed = isFailed,
        isWaiting = isWaiting,
        onRetry = onRetry,
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
    onRetry: (() -> Unit)?,
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
) {
    val onlyMedia = content.isNullOrBlank()
    val appTypography = LocalAppTopography.current
    val textColor = YralColors.NeutralTextPrimary

    val markdownColors = markDownColors(textColor)
    val markdownTypography = markdownTypography(appTypography)

    MessageInBubble(
        isUser = isUser,
        isFailed = isFailed,
        isOnlyMedia = onlyMedia,
    ) {
        if (!content.isNullOrBlank()) {
            // The intellij-markdown parser crashes with StringIndexOutOfBoundsException when
            // content contains supplementary Unicode chars (emoji/surrogate pairs) that cause
            // AST node offsets to exceed the actual string length. Strip them before rendering.
            val safeContent =
                remember(content) {
                    if (content.any { it.isHighSurrogate() }) {
                        content.filter { !it.isHighSurrogate() && !it.isLowSurrogate() }
                    } else {
                        content
                    }
                }
            Markdown(
                content = safeContent,
                colors = markdownColors,
                typography = markdownTypography,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
        MessageImages(
            mediaUrls = mediaUrls,
            onImageClick = onImageClick,
            maxWidth = maxWidth,
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
) {
    if (mediaUrls.isEmpty()) return

    // If you ever need better perf, switch to LazyRow/Column or a grid.
    mediaUrls.forEach { imageUrl ->
        ChatMessageImage(
            imageUrl = imageUrl,
            onImageClick = onImageClick,
            maxWidth = maxWidth,
        )
    }
}

@Composable
private fun ChatMessageImage(
    imageUrl: String,
    onImageClick: ((String) -> Unit)?,
    maxWidth: Dp,
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
                if (onImageClick != null) {
                    baseModifier.clickable { onImageClick(imageUrl) }
                } else {
                    baseModifier
                }
            }

    Box(
        modifier = imageModifier,
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = "image",
            modifier = Modifier.fillMaxSize(),
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

private const val FILE_URL_PREFIX = "file://"
private const val ABSOLUTE_PATH_PREFIX = "/"
