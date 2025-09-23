package com.yral.android.ui.screens.uploadVideo.aiVideoGen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.screens.uploadVideo.aiVideoGen.AiVideoGenScreenConstants.ARROW_ROTATION
import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.features.uploadvideo.presentation.AiVideoGenViewModel.ViewState
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.getSVGImageModel
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
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
                text = stringResource(R.string.model),
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.Neutral300,
            )
            ProviderRow(provider = provider, onClick = onClick)
        }
        viewState.usedCredits?.let { usedCredits ->
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = stringResource(R.string.credits_required),
                    style = LocalAppTopography.current.baseMedium,
                    color = YralColors.Neutral300,
                )
                CostRow(
                    isCreditsAvailable = viewState.isCreditsAvailable(),
                    isBalanceLow = viewState.isBalanceLow(),
                    selectedProvider = viewState.selectedProvider,
                )
                CreditsBalance(
                    isCreditsAvailable = viewState.isCreditsAvailable(),
                    isBalanceLow = viewState.isBalanceLow(),
                    usedCredits = usedCredits,
                    totalCredits = viewState.totalCredits,
                    currentBalance = viewState.currentBalance,
                )
            }
        }
    }
}

@Composable
private fun CostRow(
    isCreditsAvailable: Boolean,
    isBalanceLow: Boolean,
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
            isBalanceLow = isBalanceLow,
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
    isBalanceLow: Boolean,
    modifier: Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier,
    ) {
        selectedProvider?.cost?.sats?.let { cost ->
            Text(
                text = if (isCreditsAvailable || isBalanceLow) "0" else "$cost",
                style = LocalAppTopography.current.mdSemiBold,
                color = YralColors.Neutral300,
            )
            if (isCreditsAvailable || isBalanceLow) {
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
            painter = painterResource(id = R.drawable.yral),
            contentDescription = "yral",
            contentScale = ContentScale.Inside,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = stringResource(R.string.coins),
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
                        R.string.credits_used_use_token,
                        usedCredits,
                        totalCredits,
                        stringResource(R.string.coins),
                    ),
                style = LocalAppTopography.current.regRegular.copy(fontWeight = FontWeight.Bold),
                color = YralColors.NeutralTextSecondary,
            )
        } else {
            Text(
                text =
                    stringResource(
                        R.string.credits_used,
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
                        token = stringResource(R.string.coins),
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
                append(stringResource(R.string.current_balance))
            }
            withStyle(spanStyle.copy(fontWeight = FontWeight.Bold)) {
                append(" $currentBalance $token")
            }
        } else {
            withStyle(spanStyle) {
                append(stringResource(R.string.low_balance))
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
