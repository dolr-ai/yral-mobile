package com.yral.shared.features.subscriptions.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
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
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_active_credits
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_active_credits_label
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_active_cta
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_active_status_title
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_active_subtitle
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_active_tagline
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_active_terms
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_active_title
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_active_valid_prefix
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_background
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_benefit_ai
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_benefit_chat
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_benefit_global
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_benefit_rewards
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.ic_lightning_bolt
import yral_mobile.shared.libs.designsystem.generated.resources.ic_lightning_bolt_gold
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
fun SubscriptionActiveScreen(
    modifier: Modifier = Modifier,
    validTillText: String,
    creditsReceived: Int = 40,
    onBack: () -> Unit = {},
    onExploreHome: () -> Unit = {},
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
                        painter = painterResource(Res.drawable.subscription_background),
                        contentScale = ContentScale.Crop,
                    ),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Header(onBack = onBack)
                    HeroLogo(modifier = Modifier.align(Alignment.BottomCenter))
                }
                ActiveContent(
                    validTillText = validTillText,
                    creditsReceived = creditsReceived,
                    onExploreHome = onExploreHome,
                )
            }
        }
    }
}

@Composable
private fun ActiveContent(
    validTillText: String,
    creditsReceived: Int,
    onExploreHome: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text =
                buildAnnotatedString {
                    append("Welcome to ")
                    withStyle(SpanStyle(color = YralColors.Yellow200)) {
                        append("Yral Pro!")
                    }
                },
            style = LocalAppTopography.current.xxlBold,
            color = YralColors.Neutral50,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(Res.string.subscription_active_subtitle),
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.Neutral300,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        StatusCard(validTillText = validTillText, creditsReceived = creditsReceived)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(Res.string.subscription_active_tagline),
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.Neutral50,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        BenefitList()
        Spacer(modifier = Modifier.height(20.dp))
        YralGradientButton(
            text = stringResource(Res.string.subscription_active_cta),
            onClick = onExploreHome,
            buttonHeight = 46.dp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(Res.string.subscription_active_terms),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.Neutral300,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Image(
                painter = painterResource(DesignRes.drawable.arrow_left),
                contentDescription = "Back",
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = stringResource(Res.string.subscription_active_title),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.Neutral0,
        )
    }
}

@Composable
private fun HeroLogo(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(width = 200.dp, height = 180.dp)
                .padding(top = 28.dp)
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
}

@Composable
private fun StatusCard(
    validTillText: String,
    creditsReceived: Int,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    shape =
                        androidx.compose.foundation.shape
                            .RoundedCornerShape(8.dp),
                ).background(YralColors.Neutral900)
                .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.subscription_active_status_title),
            style = LocalAppTopography.current.baseSemiBold,
            color = YralColors.Neutral50,
        )
        Text(
            text = "${stringResource(Res.string.subscription_active_valid_prefix)} $validTillText",
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.Neutral500,
        )
        CreditsRow(creditsReceived)
    }
}

@Composable
private fun CreditsRow(creditsReceived: Int) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    shape =
                        androidx.compose.foundation.shape
                            .RoundedCornerShape(8.dp),
                ).background(YralColors.Neutral950)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(Res.string.subscription_active_credits_label),
            style = LocalAppTopography.current.baseSemiBold,
            color = YralColors.Neutral50,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(Res.string.subscription_active_credits, creditsReceived),
                style = LocalAppTopography.current.lgBold,
                color = YralColors.Yellow200,
            )
            Image(
                painter = painterResource(DesignRes.drawable.ic_lightning_bolt),
                contentDescription = "Credits",
                modifier = Modifier.size(width = 10.dp, height = 16.dp),
            )
        }
    }
}

@Composable
private fun BenefitList() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SubscriptionBenefitRow(
            iconRes = Res.drawable.subscription_benefit_ai,
            text = stringResource(Res.string.subscription_active_benefit_ai),
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

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun SubscriptionActiveScreenPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        SubscriptionActiveScreen(
            validTillText = "31 Dec 2024",
            creditsReceived = DEFAULT_TOTAL_CREDITS,
            onBack = {},
            onExploreHome = {},
        )
    }
}
