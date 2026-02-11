package com.yral.shared.features.subscriptions.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.features.subscriptions.ui.components.SubscriptionNudgeGenericBenefits
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.features.subscriptions.generated.resources.Res
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_nudge_cta_dismiss
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_nudge_cta_subscribe
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_nudge_cta_subscribe_with_price
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_nudge_title
import yral_mobile.shared.libs.designsystem.generated.resources.ic_lightning_bolt_gold
import yral_mobile.shared.libs.designsystem.generated.resources.ic_x
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun SubscriptionNudge(
    modifier: Modifier = Modifier,
    priceText: String? = null,
    onSubscribeClicked: () -> Unit,
    onDismissClicked: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(YralColors.ScrimColor),
    ) {
        IconButton(
            onClick = onDismissClicked,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Icon(
                painter = painterResource(DesignRes.drawable.ic_x),
                contentDescription = "Close",
                tint = YralColors.Neutral0,
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(width = 200.dp, height = 200.dp)
                        .background(
                            brush =
                                Brush.radialGradient(
                                    colors =
                                        listOf(
                                            YralColors.YellowGlowShadow,
                                            Color.Transparent,
                                        ),
                                    radius = 200f,
                                ),
                        ),
            ) {
                Image(
                    painter = painterResource(DesignRes.drawable.ic_lightning_bolt_gold),
                    contentDescription = "Subscription Logo",
                    modifier = Modifier.size(width = 74.dp, height = 120.dp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(Res.string.subscription_nudge_title),
                style = LocalAppTopography.current.lgBold,
                color = YralColors.Neutral0,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(28.dp))

            SubscriptionNudgeGenericBenefits(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val ctaText =
                    priceText?.let {
                        stringResource(Res.string.subscription_nudge_cta_subscribe_with_price, it)
                    } ?: stringResource(Res.string.subscription_nudge_cta_subscribe)

                YralGradientButton(
                    text = ctaText,
                    onClick = onSubscribeClicked,
                    buttonHeight = 42.dp,
                )

                YralButton(
                    text = stringResource(Res.string.subscription_nudge_cta_dismiss),
                    onClick = onDismissClicked,
                    backgroundColor = YralColors.Neutral800,
                    borderColor = YralColors.Neutral700,
                    borderWidth = 1.dp,
                    buttonHeight = 42.dp,
                    textStyle =
                        LocalAppTopography.current.mdMedium.copy(
                            color = YralColors.Neutral0,
                        ),
                )
            }
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun SubscriptionNudgePreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        SubscriptionNudge(
            priceText = "₹49",
            onSubscribeClicked = {},
            onDismissClicked = {},
        )
    }
}
