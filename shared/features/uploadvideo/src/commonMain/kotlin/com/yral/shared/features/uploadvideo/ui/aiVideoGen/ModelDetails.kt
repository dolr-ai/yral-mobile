package com.yral.shared.features.uploadvideo.ui.aiVideoGen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.features.uploadvideo.presentation.AiVideoGenViewModel.ViewState
import com.yral.shared.features.uploadvideo.ui.aiVideoGen.AiVideoGenScreenConstants.ARROW_ROTATION
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.getSVGImageModel
import com.yral.shared.libs.designsystem.component.shimmer
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.uploadvideo.generated.resources.Res
import yral_mobile.shared.features.uploadvideo.generated.resources.credits
import yral_mobile.shared.features.uploadvideo.generated.resources.credits_required
import yral_mobile.shared.features.uploadvideo.generated.resources.credits_used
import yral_mobile.shared.features.uploadvideo.generated.resources.credits_used_use_token
import yral_mobile.shared.features.uploadvideo.generated.resources.low_balance
import yral_mobile.shared.features.uploadvideo.generated.resources.model
import yral_mobile.shared.features.uploadvideo.generated.resources.monthly_credits_disclaimer
import yral_mobile.shared.features.uploadvideo.generated.resources.monthly_credits_exhausted_disclaimer
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.coins
import yral_mobile.shared.libs.designsystem.generated.resources.current_balance
import yral_mobile.shared.libs.designsystem.generated.resources.ic_gray_info_circle
import yral_mobile.shared.libs.designsystem.generated.resources.yral
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
fun ModelDetails(
    provider: Provider,
    viewState: ViewState,
    onClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(Res.string.model),
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.Neutral300,
            )
            ProviderRow(provider = provider, onClick = onClick)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            CreditsCardWithShimmer(
                usedCredits = viewState.usedCredits,
                totalCredits = viewState.totalCredits,
            )
            viewState.totalCredits?.let { totalCredits ->
                viewState.freeCreditsWindow?.let { window ->
                    CreditsDisclaimer(
                        isCreditsAvailable = viewState.isCreditsAvailable(),
                        totalCredits = totalCredits,
                        window = window,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditsCardWithShimmer(
    usedCredits: Int?,
    totalCredits: Int?,
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (usedCredits != null && totalCredits != null) {
            CreditsCard(
                usedCredits = usedCredits,
                totalCredits = totalCredits,
            )
        }
        AnimatedVisibility(
            visible = usedCredits == null || totalCredits == null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .border(
                            width = 1.dp,
                            color = YralColors.Neutral700,
                            shape = RoundedCornerShape(size = 8.dp),
                        ).background(
                            color = YralColors.Neutral900,
                            shape = RoundedCornerShape(size = 8.dp),
                        ).shimmer(cornerRadius = 8.dp),
            )
        }
    }
}

@Composable
private fun CreditsCard(
    usedCredits: Int,
    totalCredits: Int,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = YralColors.Neutral700,
                    shape = RoundedCornerShape(size = 8.dp),
                ).background(
                    color = YralColors.Neutral900,
                    shape = RoundedCornerShape(size = 8.dp),
                ).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.credits),
            style = LocalAppTopography.current.baseSemiBold,
            color = YralColors.NeutralTextPrimary,
        )
        Text(
            text = "$usedCredits / $totalCredits",
            style = LocalAppTopography.current.lgBold,
            color = if (usedCredits < totalCredits) YralColors.Yellow200 else YralColors.Red300,
        )
    }
}

@Composable
private fun CreditsDisclaimer(
    isCreditsAvailable: Boolean,
    totalCredits: Int,
    window: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(DesignRes.drawable.ic_gray_info_circle),
            contentDescription = "info",
            contentScale = ContentScale.None,
            modifier = Modifier.size(16.dp),
        )
        if (isCreditsAvailable) {
            Text(
                text = stringResource(Res.string.monthly_credits_disclaimer, totalCredits, window),
                style = LocalAppTopography.current.regRegular,
                color = YralColors.NeutralTextSecondary,
            )
        } else {
            Text(
                text = stringResource(Res.string.monthly_credits_exhausted_disclaimer, totalCredits, window),
                style = LocalAppTopography.current.regRegular,
                color = YralColors.Red300,
            )
        }
    }
}

// Unused: previous credits UI (kept in case we need to restore).
// Replaced by CreditsCard above per Figma design.
@Composable
@Suppress("UnusedPrivateMember")
private fun CreditsSectionLegacy(viewState: ViewState) {
    val usedCredits = viewState.usedCredits ?: return
    val totalCredits = viewState.totalCredits ?: return
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(Res.string.credits_required),
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.Neutral300,
        )
        CostRow(
            isCreditsAvailable = viewState.isCreditsAvailable(),
            selectedProvider = viewState.selectedProvider,
        )
        CreditsBalance(
            isCreditsAvailable = viewState.isCreditsAvailable(),
            isBalanceLow = viewState.isBalanceLow(),
            usedCredits = usedCredits,
            totalCredits = totalCredits,
            currentBalance = viewState.currentBalance,
        )
    }
}

@Composable
private fun CostRow(
    isCreditsAvailable: Boolean,
    selectedProvider: Provider?,
) {
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = YralColors.Neutral700,
                    shape = RoundedCornerShape(size = 8.dp),
                ).background(color = YralColors.Neutral900, shape = RoundedCornerShape(size = 8.dp))
                .clickable { }
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Cost(
            selectedProvider = selectedProvider,
            isCreditsAvailable = isCreditsAvailable,
            modifier = Modifier.weight(1f),
        )
        CostToken()
    }
}

@Composable
private fun Cost(
    selectedProvider: Provider?,
    isCreditsAvailable: Boolean,
    modifier: Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier,
    ) {
        selectedProvider?.cost?.sats?.let { cost ->
            Text(
                text = if (isCreditsAvailable) "0" else "$cost",
                style = LocalAppTopography.current.mdSemiBold,
                color = YralColors.Neutral300,
            )
            if (isCreditsAvailable && cost > 0) {
                Text(
                    text = "$cost",
                    style = LocalAppTopography.current.baseMedium,
                    color = YralColors.Neutral600,
                    textDecoration = TextDecoration.LineThrough,
                )
            }
        }
    }
}

@Composable
private fun CostToken() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .height(40.dp)
                .background(
                    color = YralColors.Neutral700,
                    shape = RoundedCornerShape(size = 8.dp),
                ).padding(all = 8.dp),
    ) {
        Image(
            painter = painterResource(DesignRes.drawable.yral),
            contentDescription = "yral",
            contentScale = ContentScale.Inside,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = stringResource(DesignRes.string.coins),
            style = LocalAppTopography.current.baseBold,
            color = YralColors.NeutralTextPrimary,
        )
    }
}

@Composable
private fun CreditsBalance(
    isCreditsAvailable: Boolean,
    isBalanceLow: Boolean,
    usedCredits: Int,
    totalCredits: Int,
    currentBalance: Long?,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        if (!isCreditsAvailable && !isBalanceLow) {
            Text(
                text =
                    stringResource(
                        Res.string.credits_used_use_token,
                        usedCredits,
                        totalCredits,
                        stringResource(DesignRes.string.coins),
                    ),
                style = LocalAppTopography.current.regRegular.copy(fontWeight = FontWeight.Bold),
                color = YralColors.NeutralTextSecondary,
            )
        } else {
            Text(
                text =
                    stringResource(
                        Res.string.credits_used,
                        usedCredits,
                        totalCredits,
                    ),
                style = LocalAppTopography.current.regRegular.copy(fontWeight = FontWeight.Bold),
                color = if (isCreditsAvailable) YralColors.Green300 else YralColors.Red300,
            )
        }
        currentBalance?.let {
            Text(
                text =
                    buildBalanceString(
                        isBalanceLow = isBalanceLow,
                        isCreditsAvailable = isCreditsAvailable,
                        currentBalance = it,
                        token = stringResource(DesignRes.string.coins),
                    ),
            )
        }
    }
}

@Composable
private fun buildBalanceString(
    isBalanceLow: Boolean,
    isCreditsAvailable: Boolean,
    currentBalance: Long,
    token: String,
): AnnotatedString =
    buildAnnotatedString {
        val textStyle = LocalAppTopography.current.regRegular
        val spanStyle =
            SpanStyle(
                fontSize = textStyle.fontSize,
                fontFamily = textStyle.fontFamily,
                fontWeight = textStyle.fontWeight,
                color = if (isBalanceLow) YralColors.Red300 else YralColors.NeutralTextSecondary,
            )
        if (!isBalanceLow || isCreditsAvailable) {
            withStyle(spanStyle) {
                append(stringResource(DesignRes.string.current_balance))
            }
            withStyle(spanStyle.copy(fontWeight = FontWeight.Bold)) {
                append(" $currentBalance $token")
            }
        } else {
            withStyle(spanStyle) {
                append(stringResource(Res.string.low_balance))
            }
            withStyle(spanStyle.copy(fontWeight = FontWeight.Bold)) {
                append(" $currentBalance $token")
            }
        }
    }

@Composable
private fun ProviderRow(
    provider: Provider,
    onClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = YralColors.Neutral700,
                    shape = RoundedCornerShape(size = 8.dp),
                ).background(color = YralColors.Neutral900, shape = RoundedCornerShape(size = 8.dp))
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        provider.modelIcon?.let { url ->
            YralAsyncImage(
                imageUrl = getSVGImageModel(url),
                modifier = Modifier.size(30.dp),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = provider.name,
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextPrimary,
            )
            provider.description?.let { description ->
                Text(
                    text = description,
                    style = LocalAppTopography.current.regRegular,
                    color = YralColors.NeutralTextSecondary,
                )
            }
        }
        Image(
            painter = painterResource(DesignRes.drawable.arrow_left),
            contentDescription = "select model",
            contentScale = ContentScale.None,
            modifier = Modifier.size(24.dp).rotate(ARROW_ROTATION),
        )
    }
}
