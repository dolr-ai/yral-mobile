package com.yral.shared.features.subscriptions.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.core.session.DEFAULT_TOTAL_CREDITS
import com.yral.shared.libs.designsystem.component.YralAnimatedBounceIcon
import com.yral.shared.libs.designsystem.component.YralButtonType
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.subscriptions.generated.resources.Res
import yral_mobile.shared.features.subscriptions.generated.resources.create_ai_video
import yral_mobile.shared.features.subscriptions.generated.resources.explore_feed
import yral_mobile.shared.features.subscriptions.generated.resources.payment_successful_title
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_success_background
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_success_body
import yral_mobile.shared.libs.designsystem.generated.resources.ic_success
import yral_mobile.shared.libs.designsystem.generated.resources.ic_x
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
fun SubscriptionPaymentSuccessScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onCreateVideo: () -> Unit = {},
    onExploreFeed: () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .paint(
                        painter = painterResource(Res.drawable.subscription_success_background),
                        contentScale = ContentScale.Crop,
                    ).padding(vertical = 24.dp),
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    painter = painterResource(DesignRes.drawable.ic_x),
                    contentDescription = "Close",
                )
            }

            SubscriptionSuccessContent(onCreateVideo = onCreateVideo, onExploreFeed = onExploreFeed)
        }
    }
}

@Composable
private fun SubscriptionSuccessContent(
    onCreateVideo: () -> Unit,
    onExploreFeed: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        YralAnimatedBounceIcon(
            modifier = Modifier.offset(y = (-8).dp),
            imageRes = DesignRes.drawable.ic_success,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(Res.string.payment_successful_title),
            style = LocalAppTopography.current.lgBold,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.subscription_success_body, DEFAULT_TOTAL_CREDITS),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(36.dp))
        YralGradientButton(
            text = stringResource(Res.string.create_ai_video),
            modifier = Modifier.fillMaxWidth(),
            buttonType = YralButtonType.Pink,
            onClick = onCreateVideo,
        )
        Spacer(modifier = Modifier.height(12.dp))
        YralGradientButton(
            text = stringResource(Res.string.explore_feed),
            modifier = Modifier.fillMaxWidth(),
            buttonType = YralButtonType.White,
            onClick = onExploreFeed,
        )
    }
}
