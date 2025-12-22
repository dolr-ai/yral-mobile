package com.yral.shared.features.chat.ui.conversation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yral.shared.features.chat.attachments.FilePathChatAttachment
import com.yral.shared.features.chat.domain.models.ChatMessageType
import com.yral.shared.features.chat.domain.models.SendMessageDraft
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.getLocalImageModel
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
fun ImagePreviewOverlay(
    imageAttachment: FilePathChatAttachment,
    onSend: (SendMessageDraft) -> Unit,
    onDismiss: () -> Unit,
    bottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeBottomDp = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    val keyboardAwareBottomPadding = (imeBottomDp - bottomPadding).coerceAtLeast(0.dp)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable {},
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = keyboardAwareBottomPadding),
        ) {
            YralAsyncImage(
                imageUrl = getLocalImageModel(imageAttachment.filePath),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentScale = ContentScale.Fit,
                shape = RectangleShape,
            )

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
                        onSend(
                            SendMessageDraft(
                                messageType = ChatMessageType.IMAGE,
                                content = text.takeIf { it.isNotBlank() },
                                mediaAttachments = listOf(imageAttachment),
                            ),
                        )
                        onDismiss()
                    },
                    showAttachmentMenu = false,
                    placeholder = stringResource(Res.string.message_optional),
                )
            }
        }
        CloseButton(onDismiss)
    }
}

@Composable
private fun BoxScope.CloseButton(onDismiss: () -> Unit) {
    Box(
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 85.dp, end = 16.dp)
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
