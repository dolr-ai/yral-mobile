@file:Suppress("MagicNumber")

package com.yral.shared.features.chat.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.influencer_subscription_cta
import yral_mobile.shared.features.chat.generated.resources.influencer_subscription_limited_time_offer
import yral_mobile.shared.features.chat.generated.resources.influencer_subscription_plan_description
import yral_mobile.shared.features.chat.generated.resources.influencer_subscription_plan_title

private val CARD_RADIUS = 8.dp
private val CARD_BODY_PADDING_HORIZONTAL = 16.dp
private val CARD_BODY_PADDING_VERTICAL = 12.dp
private val GAP_CARD_BUTTON = 16.dp
private val GAP_HEADER_CARD = 8.dp
private val GRADIENT_BORDER_PADDING = 2.dp
private val HEADER_TOP_PADDING = 10.dp
private val HEADER_BOTTOM_PADDING = 2.dp
private val TITLE_DESCRIPTION_GAP = 6.dp
private val CTA_BUTTON_HEIGHT = 48.dp

private val INFLUENCER_SUBSCRIPTION_HEADER_GRADIENT_START = Color(0xFF7C0143)
private val INFLUENCER_SUBSCRIPTION_HEADER_GRADIENT_MID = Color(0xFFE2017B)
private val INFLUENCER_SUBSCRIPTION_CARD_BACKGROUND = Color(0xFF1A0410)

private const val GRADIENT_STOP_START = 0f
private const val GRADIENT_STOP_MID = 0.528f
private const val GRADIENT_STOP_END = 1f

private fun influencerSubscriptionHeaderGradientBrush() =
    Brush.linearGradient(
        colorStops =
            arrayOf(
                GRADIENT_STOP_START to INFLUENCER_SUBSCRIPTION_HEADER_GRADIENT_START,
                GRADIENT_STOP_MID to INFLUENCER_SUBSCRIPTION_HEADER_GRADIENT_MID,
                GRADIENT_STOP_END to INFLUENCER_SUBSCRIPTION_HEADER_GRADIENT_START,
            ),
        start = Offset.Zero,
        end = Offset(Float.POSITIVE_INFINITY, 0f),
    )

@Composable
fun InfluencerSubscriptionCard(
    onSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
    formattedPrice: String? = null,
    isPurchaseInProgress: Boolean = false,
) {
    Column(
        modifier = modifier.padding(horizontal = CARD_BODY_PADDING_HORIZONTAL),
        verticalArrangement = Arrangement.spacedBy(GAP_CARD_BUTTON, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        InfluencerSubscriptionCardContent(
            modifier = Modifier,
            formattedPrice = formattedPrice,
        )
        YralGradientButton(
            text = stringResource(Res.string.influencer_subscription_cta),
            onClick = onSubscribe,
            modifier = Modifier.fillMaxWidth(),
            buttonState = if (isPurchaseInProgress) YralButtonState.Loading else YralButtonState.Enabled,
            buttonHeight = CTA_BUTTON_HEIGHT,
        )
    }
}

@Composable
private fun InfluencerSubscriptionCardContent(
    modifier: Modifier,
    formattedPrice: String?,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CARD_RADIUS))
                .background(brush = influencerSubscriptionHeaderGradientBrush())
                .padding(
                    start = GRADIENT_BORDER_PADDING,
                    end = GRADIENT_BORDER_PADDING,
                    top = HEADER_TOP_PADDING,
                    bottom = HEADER_BOTTOM_PADDING,
                ),
        verticalArrangement = Arrangement.spacedBy(GAP_HEADER_CARD, Alignment.Top),
    ) {
        Text(
            text = stringResource(Res.string.influencer_subscription_limited_time_offer),
            style = LocalAppTopography.current.regBold,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        InfluencerPlanCardBody(formattedPrice = formattedPrice)
    }
}

@Composable
private fun InfluencerPlanCardBody(formattedPrice: String?) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CARD_RADIUS))
                .background(INFLUENCER_SUBSCRIPTION_CARD_BACKGROUND)
                .padding(
                    horizontal = CARD_BODY_PADDING_HORIZONTAL,
                    vertical = CARD_BODY_PADDING_VERTICAL,
                ),
        verticalArrangement = Arrangement.spacedBy(TITLE_DESCRIPTION_GAP, Alignment.Top),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(TITLE_DESCRIPTION_GAP, Alignment.Top),
            ) {
                Text(
                    text = stringResource(Res.string.influencer_subscription_plan_title),
                    style = LocalAppTopography.current.baseSemiBold,
                    color = YralColors.NeutralTextPrimary,
                )
                Text(
                    text = stringResource(Res.string.influencer_subscription_plan_description),
                    style = LocalAppTopography.current.smRegular,
                    color = YralColors.NeutralTextSecondary,
                )
            }
            if (formattedPrice != null) {
                Text(
                    text = formattedPrice,
                    style = LocalAppTopography.current.lgBold,
                    color = YralColors.Yellow200,
                )
            }
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun InfluencerSubscriptionCardPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        InfluencerSubscriptionCard(
            onSubscribe = {},
            modifier = Modifier,
            formattedPrice = "₹9",
        )
    }
}
