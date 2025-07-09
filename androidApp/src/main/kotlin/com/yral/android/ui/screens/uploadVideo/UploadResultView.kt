package com.yral.android.ui.screens.uploadVideo

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.components.AnimatedBounceIcon
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.widgets.YralButtonState
import com.yral.android.ui.widgets.YralButtonType
import com.yral.android.ui.widgets.YralGradientButton

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
                imageRes = R.drawable.upload_success,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.upload_completed),
                    style = LocalAppTopography.current.lgBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.upload_completed_message),
                    style = LocalAppTopography.current.mdRegular,
                    color = YralColors.Neutral300,
                    textAlign = TextAlign.Center,
                )
            }
            YralGradientButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.done),
                buttonState = YralButtonState.Enabled,
                buttonType = YralButtonType.Transparent,
                onClick = onDone,
            )
        }
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
                imageRes = R.drawable.upload_error,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.something_went_wrong),
                    style = LocalAppTopography.current.lgBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text =
                        stringResource(
                            R.string.upload_video_error,
                        ),
                    style = LocalAppTopography.current.mdRegular,
                    color = YralColors.Neutral300,
                    textAlign = TextAlign.Center,
                )
            }
            YralGradientButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.try_again),
                buttonState = YralButtonState.Enabled,
                buttonType = YralButtonType.Pink,
                onClick = onTryAgain,
            )
            YralGradientButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.go_to_home),
                buttonState = YralButtonState.Enabled,
                buttonType = YralButtonType.Transparent,
                onClick = onGotoHome,
            )
        }
    }
}
