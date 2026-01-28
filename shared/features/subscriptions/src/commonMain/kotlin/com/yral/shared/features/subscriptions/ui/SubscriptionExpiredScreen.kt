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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yral.shared.core.session.DEFAULT_TOTAL_CREDITS
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.component.buildHighlightedText
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
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_active_terms
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_active_title
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_background
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_benefit_ai
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_benefit_chat
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_benefit_global
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_benefit_rewards
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_expired_benefits_title
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_expired_card_button
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_expired_card_title
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_expired_cta
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_expired_subtitle
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_expired_title
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_expired_title_highlight
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_new_price
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.ic_lightning_bolt_silver
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
fun SubscriptionExpiredScreen(
    modifier: Modifier = Modifier,
    validTillText: String,
    oldPrice: String?,
    newPrice: String?,
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
                ExpiredContent(
                    validTillText = validTillText,
                    oldPrice = oldPrice,
                    newPrice = newPrice,
                    onBack = onBack,
                    onExploreHome = onExploreHome,
                )
            }
        }
    }
}

@Composable
private fun ExpiredContent(
    validTillText: String,
    oldPrice: String?,
    newPrice: String?,
    onBack: () -> Unit,
    onExploreHome: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        ExpiredHeader(onBack = onBack)
        ExpiredHeroLogo(modifier = Modifier.align(Alignment.BottomCenter))
    }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ExpiredHeroSection()
        Spacer(modifier = Modifier.height(20.dp))
        ExpiredStatusCard(validTillText, oldPrice, newPrice)
        Spacer(modifier = Modifier.height(20.dp))
        ExpiredBenefitsSection(onExploreHome = onExploreHome)
    }
}

@Composable
private fun ExpiredHeroSection() {
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text =
            buildHighlightedText(
                fullText = stringResource(Res.string.subscription_expired_title),
                highlightedText = stringResource(Res.string.subscription_expired_title_highlight),
                baseTextStyle = LocalAppTopography.current.xxlBold,
            ),
        style = LocalAppTopography.current.xxlBold,
        color = YralColors.Neutral50,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = stringResource(Res.string.subscription_expired_subtitle),
        style = LocalAppTopography.current.baseMedium,
        color = YralColors.Neutral300,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ExpiredBenefitsSection(onExploreHome: () -> Unit) {
    Text(
        text = stringResource(Res.string.subscription_expired_benefits_title),
        style = LocalAppTopography.current.baseMedium,
        color = YralColors.Neutral50,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(16.dp))
    ExpiredBenefitList()
    Spacer(modifier = Modifier.height(20.dp))
    YralGradientButton(
        text = stringResource(Res.string.subscription_expired_cta),
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

@Composable
private fun ExpiredHeader(onBack: () -> Unit) {
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
private fun ExpiredHeroLogo(modifier: Modifier) {
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
                                    YralColors.SilverGlowShadow,
                                    Color.Transparent,
                                ),
                            radius = 200f,
                        ),
                ),
    ) {
        Image(
            painter = painterResource(DesignRes.drawable.ic_lightning_bolt_silver),
            contentDescription = "Subscription Logo",
            modifier = Modifier.size(width = 74.dp, height = 120.dp),
        )
    }
}

@Composable
private fun ExpiredStatusCard(
    validTillText: String,
    oldPrice: String?,
    newPrice: String?,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.subscription_expired_card_title),
                style = LocalAppTopography.current.regMedium,
                color = YralColors.Neutral50,
            )
            Text(
                text = validTillText,
                style = LocalAppTopography.current.regMedium,
                color = YralColors.Neutral500,
            )
        }
        ReactivateRow(oldPrice, newPrice)
    }
}

@Composable
private fun ReactivateRow(
    oldPrice: String?,
    newPrice: String?,
) {
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
            text = stringResource(Res.string.subscription_expired_card_button),
            style = LocalAppTopography.current.baseSemiBold,
            color = YralColors.Yellow200,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                            ) { append(oldPrice) }
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

@Composable
private fun ExpiredBenefitList() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SubscriptionBenefitRow(
            iconRes = Res.drawable.subscription_benefit_ai,
            text = stringResource(Res.string.subscription_active_benefit_ai, DEFAULT_TOTAL_CREDITS),
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
private fun SubscriptionExpiredScreenPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        SubscriptionExpiredScreen(
            validTillText = "31 Dec 2024",
            oldPrice = "₹499",
            newPrice = "₹49",
            onBack = {},
            onExploreHome = {},
        )
    }
}
