package com.yral.shared.features.chat.ui.conversation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.yral.shared.features.chat.domain.models.ChatMessageType
import com.yral.shared.features.chat.domain.models.SendMessageDraft
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.close
import yral_mobile.shared.features.chat.generated.resources.message_optional
import yral_mobile.shared.libs.designsystem.generated.resources.cross
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongMethod")
@Composable
internal fun ImagePreviewOverlay(
    previewSource: ChatImagePreviewSource,
    onSend: ((SendMessageDraft) -> Unit)? = null,
    onDismiss: () -> Unit,
    hasWaitingAssistant: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val draftAttachment = (previewSource as? ChatImagePreviewSource.Draft)?.imageAttachment

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable {},
    ) {
        when (previewSource) {
            is ChatImagePreviewSource.Draft ->
                YralAsyncImage(
                    imageUrl = rememberChatImageModel(previewSource.imageAttachment.filePath),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    shape = RectangleShape,
                )

            is ChatImagePreviewSource.Message -> MessageImagePager(previewSource)
        }
        if (draftAttachment != null && onSend != null) {
            Column(modifier = Modifier.align(BottomCenter)) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                ) {
                    ChatInputArea(
                        input = input,
                        onInputChange = { input = it },
                        onSendClick = {
                            keyboardController?.hide()
                            val text = input.trim()
                            input = ""
                            try {
                                onSend(
                                    SendMessageDraft(
                                        messageType = ChatMessageType.IMAGE,
                                        content = text.takeIf { it.isNotBlank() },
                                        mediaAttachments = listOf(draftAttachment),
                                    ),
                                )
                            } finally {
                                onDismiss()
                            }
                        },
                        showAttachmentMenu = false,
                        hasWaitingAssistant = hasWaitingAssistant,
                        placeholder = stringResource(Res.string.message_optional),
                    )
                }
            }
        }
        CloseButton(onDismiss)
    }
}

/**
 * Full-screen viewer for message images. When the source carries sibling
 * images (a collage), they page horizontally starting at the tapped one,
 * with a position counter; a lone image renders exactly as before.
 */
@Composable
private fun BoxScope.MessageImagePager(source: ChatImagePreviewSource.Message) {
    val images =
        remember(source) {
            source.galleryUrls.ifEmpty { listOf(source.imageUrl) }
        }
    val pagerState =
        rememberPagerState(
            initialPage = images.indexOf(source.imageUrl).coerceAtLeast(0),
            pageCount = { images.size },
        )
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        YralAsyncImage(
            imageUrl = rememberChatImageModel(images[page]),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            shape = RectangleShape,
        )
    }
    if (images.size > 1) {
        Text(
            text = "${pagerState.currentPage + 1}/${images.size}",
            style = LocalAppTopography.current.regSemiBold,
            color = Color.White,
            modifier =
                Modifier
                    .align(BottomCenter)
                    .padding(bottom = 24.dp)
                    .background(YralColors.Neutral600.copy(alpha = PAGE_COUNTER_BG_ALPHA), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

private const val PAGE_COUNTER_BG_ALPHA = 0.7f

@Composable
private fun BoxScope.CloseButton(onDismiss: () -> Unit) {
    Box(
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .width(32.dp)
                .height(32.dp)
                .background(color = YralColors.Neutral600, CircleShape)
                .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(DesignRes.drawable.cross),
            contentDescription = stringResource(Res.string.close),
            modifier = Modifier.size(24.dp),
            contentScale = ContentScale.None,
        )
    }
}
