package com.yral.shared.features.chat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.features.chat.domain.models.ChatError
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.chat_error
import yral_mobile.shared.features.chat.generated.resources.error_auth_message
import yral_mobile.shared.features.chat.generated.resources.error_auth_title
import yral_mobile.shared.features.chat.generated.resources.error_dismiss_action
import yral_mobile.shared.features.chat.generated.resources.error_network_message
import yral_mobile.shared.features.chat.generated.resources.error_network_title
import yral_mobile.shared.features.chat.generated.resources.error_retry_action
import yral_mobile.shared.features.chat.generated.resources.error_server_message
import yral_mobile.shared.features.chat.generated.resources.error_server_title
import yral_mobile.shared.features.chat.generated.resources.error_unknown_message
import yral_mobile.shared.features.chat.generated.resources.error_unknown_title

/**
 * Bottom sheet component for displaying chat errors.
 * Used for conversation-level and list-level errors only.
 * Message-level errors use the existing inline retry UI.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatErrorBottomSheet(
    error: ChatError,
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
    title: String? = null,
    description: String? = null,
    errorIllustration: DrawableResource? = null,
) {
    YralBottomSheet(
        onDismissRequest = onDismissRequest,
        bottomSheetState = bottomSheetState,
        dragHandle = { DragHandle(color = YralColors.Neutral500) },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Error illustration
            Image(
                painter = painterResource(errorIllustration ?: Res.drawable.chat_error),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error title
            Text(
                text =
                    title
                        ?: stringResource(
                            when (error) {
                                is ChatError.NetworkError -> Res.string.error_network_title
                                is ChatError.AuthenticationError -> Res.string.error_auth_title
                                is ChatError.ServerError -> Res.string.error_server_title
                                is ChatError.UnknownError -> Res.string.error_unknown_title
                            },
                        ),
                style = LocalAppTopography.current.xlSemiBold,
                textAlign = TextAlign.Center,
                color = YralColors.Neutral50,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Error message
            Text(
                text =
                    description
                        ?: stringResource(
                            when (error) {
                                is ChatError.NetworkError -> Res.string.error_network_message
                                is ChatError.AuthenticationError -> Res.string.error_auth_message
                                is ChatError.ServerError -> Res.string.error_server_message
                                is ChatError.UnknownError -> Res.string.error_unknown_message
                            },
                        ),
                style = LocalAppTopography.current.regRegular,
                textAlign = TextAlign.Center,
                color = YralColors.Neutral300,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action button
            if (error.isRetryable) {
                YralGradientButton(
                    text = stringResource(Res.string.error_retry_action),
                    onClick = error.retry,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                YralGradientButton(
                    text = stringResource(Res.string.error_dismiss_action),
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Suppress("UnusedPrivateMember")
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun ChatErrorBottomSheetNetworkPreview() {
    ChatErrorBottomSheet(
        error = ChatError.NetworkError(retry = {}),
        bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = {},
    )
}

@Suppress("UnusedPrivateMember")
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun ChatErrorBottomSheetAuthPreview() {
    ChatErrorBottomSheet(
        error = ChatError.AuthenticationError(),
        bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = {},
    )
}

@Suppress("UnusedPrivateMember")
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun ChatErrorBottomSheetServerPreview() {
    ChatErrorBottomSheet(
        error = ChatError.ServerError(retry = {}),
        bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = {},
    )
}

@Suppress("UnusedPrivateMember")
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun ChatErrorBottomSheetUnknownPreview() {
    ChatErrorBottomSheet(
        error = ChatError.UnknownError(retry = {}),
        bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = {},
    )
}
