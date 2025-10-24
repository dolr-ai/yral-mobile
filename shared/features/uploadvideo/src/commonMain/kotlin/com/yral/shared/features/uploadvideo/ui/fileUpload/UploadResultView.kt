package com.yral.shared.features.uploadvideo.ui.fileUpload

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralButtonType
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.uploadvideo.generated.resources.Res
import yral_mobile.shared.features.uploadvideo.generated.resources.go_to_home
import yral_mobile.shared.features.uploadvideo.generated.resources.upload_completed
import yral_mobile.shared.features.uploadvideo.generated.resources.upload_completed_message
import yral_mobile.shared.features.uploadvideo.generated.resources.upload_video_error
import yral_mobile.shared.libs.designsystem.generated.resources.done
import yral_mobile.shared.libs.designsystem.generated.resources.ic_error
import yral_mobile.shared.libs.designsystem.generated.resources.ic_success
import yral_mobile.shared.libs.designsystem.generated.resources.my_profile
import yral_mobile.shared.libs.designsystem.generated.resources.something_went_wrong
import yral_mobile.shared.libs.designsystem.generated.resources.try_again
import yral_mobile.shared.libs.designsystem.generated.resources.your_videos
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
fun UploadVideoSuccess(onDone: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AnimatedBounceIcon(
                modifier = Modifier.offset(y = (-8).dp),
                imageRes = DesignRes.drawable.ic_success,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.upload_completed),
                    style = LocalAppTopography.current.lgBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = buildUploadCompletedMessage(),
                    style = LocalAppTopography.current.mdRegular,
                    color = YralColors.Neutral300,
                    textAlign = TextAlign.Center,
                )
            }
            YralGradientButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(DesignRes.string.done),
                buttonState = YralButtonState.Enabled,
                buttonType = YralButtonType.Transparent,
                onClick = onDone,
            )
        }
    }
}

@Composable
private fun buildUploadCompletedMessage(): AnnotatedString {
    val fullMessage = stringResource(Res.string.upload_completed_message)
    val yourVideos = stringResource(DesignRes.string.your_videos)
    val myProfile = stringResource(DesignRes.string.my_profile)
    val firstPart = fullMessage.substringBefore(yourVideos)
    val middlePart = fullMessage.substringAfter(yourVideos).substringBefore(myProfile)
    val endPart = fullMessage.substringAfter(myProfile)
    return buildAnnotatedString {
        append(firstPart)
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(DesignRes.string.your_videos))
        }
        append(middlePart)
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(DesignRes.string.my_profile))
        }
        append(endPart)
    }
}

@Suppress("LongMethod")
@Composable
fun UploadVideoFailure(
    onTryAgain: () -> Unit,
    onGotoHome: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AnimatedBounceIcon(
                modifier = Modifier.offset(y = (-8).dp),
                imageRes = DesignRes.drawable.ic_error,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(DesignRes.string.something_went_wrong),
                    style = LocalAppTopography.current.lgBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text =
                        stringResource(
                            Res.string.upload_video_error,
                        ),
                    style = LocalAppTopography.current.mdRegular,
                    color = YralColors.Neutral300,
                    textAlign = TextAlign.Center,
                )
            }
            YralGradientButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(DesignRes.string.try_again),
                buttonState = YralButtonState.Enabled,
                buttonType = YralButtonType.Pink,
                onClick = onTryAgain,
            )
            YralGradientButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.go_to_home),
                buttonState = YralButtonState.Enabled,
                buttonType = YralButtonType.Transparent,
                onClick = onGotoHome,
            )
        }
    }
}
