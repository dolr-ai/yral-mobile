package com.yral.shared.features.subscriptions.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yral.shared.features.subscriptions.ui.components.SubscriptionBenefitRow
import com.yral.shared.features.subscriptions.ui.components.SubscriptionTermsText
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.features.subscriptions.generated.resources.Res
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_active_benefit_ai
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_active_benefit_chat
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_active_benefit_global
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_active_benefit_rewards
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_benefit_ai
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_benefit_chat
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_benefit_global
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_benefit_rewards
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_girl
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_inactive_cta
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_inactive_go_pro
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_inactive_headline
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_inactive_offer_title
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_inactive_plan
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_inactive_subtitle
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_inactive_title
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_new_price
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.ic_lightning_bolt
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

private const val HERO_HEIGHT_DP = 360
private const val HERO_GRADIENT_START_Y = 200f
private const val HERO_FADE_COLOR_HEX = 0xFF000000
private val HERO_FADE_COLOR = Color(HERO_FADE_COLOR_HEX)

@Composable
fun SubscriptionInactiveScreen(
    modifier: Modifier = Modifier,
    creditsReceived: Int,
    oldPrice: String?,
    newPrice: String?,
    tncUrl: String,
    privacyPolicyUrl: String,
    onBack: () -> Unit = {},
    onSubscribe: () -> Unit = {},
    onOpenLink: (String) -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
        ) {
            HeroBackdrop()
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(bottom = 16.dp),
            ) {
                InactiveHeader(onBack = onBack)
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(200.dp))
                    InactiveHeroSection()
                    Spacer(modifier = Modifier.height(24.dp))
                    InactiveBenefitList(creditsReceived)
                    Spacer(modifier = Modifier.height(24.dp))
                    InactiveOfferCard(oldPrice, newPrice)
                    Spacer(modifier = Modifier.height(24.dp))
                    YralGradientButton(
                        text = stringResource(Res.string.subscription_inactive_cta),
                        onClick = onSubscribe,
                        buttonHeight = 46.dp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    SubscriptionTermsText(
                        tncUrl = tncUrl,
                        privacyPolicyUrl = privacyPolicyUrl,
                        onOpenLink = onOpenLink,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBackdrop() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(HERO_HEIGHT_DP.dp),
    ) {
        Image(
            painter = painterResource(Res.drawable.subscription_girl),
            contentDescription = "Subscription hero",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, HERO_FADE_COLOR),
                            startY = HERO_GRADIENT_START_Y,
                            endY = Float.POSITIVE_INFINITY,
                        ),
                    ),
        )
    }
}

@Composable
private fun InactiveHeader(onBack: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(DesignRes.drawable.arrow_left),
            contentDescription = "Back",
            modifier =
                Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically)
                    .padding(end = 8.dp)
                    .clickable(onClick = onBack),
        )
        Text(
            text = stringResource(Res.string.subscription_inactive_title),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.Neutral0,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
        )
    }
}

@Composable
private fun InactiveHeroSection() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.subscription_inactive_go_pro),
                style = LocalAppTopography.current.lgBold,
                color = YralColors.Yellow200,
            )
            Image(
                painter = painterResource(DesignRes.drawable.ic_lightning_bolt),
                contentDescription = "Pro badge",
                modifier = Modifier.size(width = 12.dp, height = 20.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.subscription_inactive_headline),
            style = LocalAppTopography.current.xxlBold,
            color = YralColors.Neutral50,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.subscription_inactive_subtitle),
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.Neutral300,
        )
    }
}

@Composable
private fun InactiveBenefitList(creditsReceived: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SubscriptionBenefitRow(
            iconRes = Res.drawable.subscription_benefit_ai,
            text = stringResource(Res.string.subscription_active_benefit_ai, creditsReceived),
        )
        SubscriptionBenefitRow(
            iconRes = Res.drawable.subscription_benefit_chat,
            text = stringResource(Res.string.subscription_active_benefit_chat),
        )
        SubscriptionBenefitRow(
            iconRes = Res.drawable.subscription_benefit_global,
            text = stringResource(Res.string.subscription_active_benefit_global),
        )
        SubscriptionBenefitRow(
            iconRes = Res.drawable.subscription_benefit_rewards,
            text = stringResource(Res.string.subscription_active_benefit_rewards),
        )
    }
}

@Composable
private fun InactiveOfferCard(
    oldPrice: String?,
    newPrice: String?,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(YralColors.Neutral900),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(Res.string.subscription_inactive_offer_title),
            style = LocalAppTopography.current.regSemiBold,
            color = YralColors.Neutral50,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
        OfferPriceRow(oldPrice, newPrice)
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun OfferPriceRow(
    oldPrice: String?,
    newPrice: String?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(YralColors.Neutral950),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(Res.string.subscription_inactive_plan),
            style = LocalAppTopography.current.baseSemiBold,
            color = YralColors.Yellow200,
            modifier = Modifier.padding(all = 16.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(end = 12.dp),
        ) {
            oldPrice?.let {
                Text(
                    text =
                        buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = YralColors.Neutral600,
                                    textDecoration = TextDecoration.LineThrough,
                                ),
                            ) {
                                append(oldPrice)
                            }
                        },
                    style = LocalAppTopography.current.baseMedium,
                )
            }
            newPrice?.let {
                Text(
                    text = stringResource(Res.string.subscription_new_price, newPrice),
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
private fun SubscriptionInactiveScreenPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        SubscriptionInactiveScreen(
            creditsReceived = 40,
            oldPrice = "₹499",
            newPrice = "₹49",
            tncUrl = "https://yral.com/terms",
            privacyPolicyUrl = "https://yral.com/privacy-policy",
            onBack = {},
            onSubscribe = {},
            onOpenLink = {},
        )
    }
}
