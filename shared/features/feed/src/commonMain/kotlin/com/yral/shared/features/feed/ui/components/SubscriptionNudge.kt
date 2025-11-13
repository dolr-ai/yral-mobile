package com.yral.shared.features.feed.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.feed.generated.resources.Res
import yral_mobile.shared.features.feed.generated.resources.subscription_benefit_ai
import yral_mobile.shared.features.feed.generated.resources.subscription_benefit_btc
import yral_mobile.shared.features.feed.generated.resources.subscription_benefit_global
import yral_mobile.shared.features.feed.generated.resources.subscription_logo
import yral_mobile.shared.features.feed.generated.resources.subscription_nudge_benefit_ai
import yral_mobile.shared.features.feed.generated.resources.subscription_nudge_benefit_btc
import yral_mobile.shared.features.feed.generated.resources.subscription_nudge_benefit_global
import yral_mobile.shared.features.feed.generated.resources.subscription_nudge_cta_dismiss
import yral_mobile.shared.features.feed.generated.resources.subscription_nudge_cta_subscribe
import yral_mobile.shared.features.feed.generated.resources.subscription_nudge_title

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun SubscriptionNudge(
    onSubscribeClicked: () -> Unit,
    onDismissClicked: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(YralColors.ScrimColor)
                .padding(vertical = 16.dp, horizontal = 48.dp),
        verticalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(Res.drawable.subscription_logo),
            contentDescription = "Subscription Logo",
            modifier =
                Modifier
                    .width(74.dp)
                    .height(120.dp),
        )

        Text(
            text = stringResource(Res.string.subscription_nudge_title),
            style = LocalAppTopography.current.lgBold,
            color = YralColors.Neutral0,
            textAlign = TextAlign.Center,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.Start,
        ) {
            SubscriptionBenefitRow(
                iconRes = Res.drawable.subscription_benefit_ai,
                text = stringResource(Res.string.subscription_nudge_benefit_ai),
            )

            SubscriptionBenefitRow(
                iconRes = Res.drawable.subscription_benefit_btc,
                text = stringResource(Res.string.subscription_nudge_benefit_btc),
            )

            SubscriptionBenefitRow(
                iconRes = Res.drawable.subscription_benefit_global,
                text = stringResource(Res.string.subscription_nudge_benefit_global),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.Start,
        ) {
            YralGradientButton(
                text = stringResource(Res.string.subscription_nudge_cta_subscribe),
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
                        color = YralColors.Neutral50,
                    ),
            )
        }
    }
}

@Composable
private fun SubscriptionBenefitRow(
    iconRes: DrawableResource,
    text: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = text,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = text,
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.Neutral50,
        )
    }
}
