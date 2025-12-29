package com.yral.shared.features.chat.ui.conversation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.yral.shared.features.chat.domain.models.ConversationMessageRole
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralLoadingDots
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
    role: ConversationMessageRole,
    content: String?,
    mediaUrls: List<String>,
    maxWidth: Dp,
    isFailed: Boolean = false,
    isWaiting: Boolean = false,
    onRetry: (() -> Unit)? = null,
) {
    MessageBubble(
        content = content,
        mediaUrls = mediaUrls,
        isUser = role == ConversationMessageRole.USER,
        maxWidth = maxWidth,
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
            RegularBubble(
                content = content,
                mediaUrls = mediaUrls,
                isUser = isUser,
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
            Markdown(
                content = content,
                colors = markdownColors,
                typography = markdownTypography,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
        MessageImages(mediaUrls)
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
    val backgroundColor = if (isUser) YralColors.Pink300 else YralColors.Neutral900
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
private fun MessageImages(mediaUrls: List<String>) {
    if (mediaUrls.isEmpty()) return

    // If you ever need better perf, switch to LazyRow/Column or a grid.
    mediaUrls.forEach { imageUrl ->
        YralAsyncImage(
            imageUrl = imageUrl,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .height(CHAT_MEDIA_IMAGE_SIZE),
            contentScale = ContentScale.Crop,
            shape = RoundedCornerShape(6.dp),
        )
    }
}
