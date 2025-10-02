package com.yral.shared.features.wallet.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.features.wallet.nav.WalletComponent
import com.yral.shared.features.wallet.viewmodel.WalletViewModel
import com.yral.shared.libs.CurrencyFormatter
import com.yral.shared.libs.NumberFormatter
import com.yral.shared.libs.designsystem.component.AccountInfoView
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.PreloadLottieAnimations
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
import com.yral.shared.libs.designsystem.component.lottie.YralRemoteLottieAnimation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralBrushes
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.features.wallet.generated.resources.Res
import yral_mobile.shared.features.wallet.generated.resources.bit_coin
import yral_mobile.shared.features.wallet.generated.resources.bitcoin
import yral_mobile.shared.features.wallet.generated.resources.btc_inr_rate
import yral_mobile.shared.features.wallet.generated.resources.get_bitcoin
import yral_mobile.shared.features.wallet.generated.resources.how_to_earn_bitcoin
import yral_mobile.shared.features.wallet.generated.resources.how_to_earn_bitcoins
import yral_mobile.shared.features.wallet.generated.resources.ic_rupee
import yral_mobile.shared.features.wallet.generated.resources.my_wallet
import yral_mobile.shared.libs.designsystem.generated.resources.coins
import yral_mobile.shared.libs.designsystem.generated.resources.current_balance
import yral_mobile.shared.libs.designsystem.generated.resources.yral
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("UnusedParameter", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    component: WalletComponent,
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isFirebaseLoggedIn by viewModel.firebaseLogin.collectAsStateWithLifecycle(false)
    val countryCode = Locale.current.region
    LaunchedEffect(Unit) { viewModel.onScreenViewed() }
    LaunchedEffect(isFirebaseLoggedIn) {
        viewModel.refresh(countryCode, isFirebaseLoggedIn)
    }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Column(modifier = modifier.fillMaxSize()) {
        WalletHeader()
        Spacer(modifier = Modifier.height(4.dp))
        state.accountInfo?.let { info ->
            AccountInfoView(
                accountInfo = info,
                isSocialSignIn = true,
                showEditProfile = false,
                onLoginClicked = {},
                onEditProfileClicked = {},
            )
        }
        state.yralTokenBalance?.let { coinBalance ->
            YralTokenBalance(coinBalance)
        }
        state.bitcoinBalance?.let {
            BitCoinBalance(
                balance = it,
                btcConversionRate = state.btcConversionRate,
                currencyCode = state.btcConversionCurrency,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        state.bitcoinBalance?.let {
            state.rewardConfig?.let {
                YralButton(
                    text = stringResource(Res.string.how_to_earn_bitcoin),
                    onClick = { viewModel.toggleHowToEarnHelp(true) },
                    modifier =
                        Modifier
                            .wrapContentWidth()
                            .height(40.dp)
                            .padding(horizontal = 16.dp),
                    textStyle = LocalAppTopography.current.baseBold,
                )
            }
        }
    }
    if (state.howToEarnHelpVisible) {
        state.rewardConfig?.let { rewardConfig ->
            YralBottomSheet(
                onDismissRequest = { viewModel.toggleHowToEarnHelp(false) },
                bottomSheetState = bottomSheetState,
                dragHandle = null,
                content = { HowToEarnBitcoinSheet(state.rewardAnimationUrl) },
            )
        }
    }
    state.rewardAnimationUrl?.let {
        PreloadLottieAnimations(
            urls = listOf(it),
        )
    }
}

@Composable
private fun WalletHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.my_wallet),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
        )
    }
}

@Composable
private fun YralTokenBalance(coinBalance: Long) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .border(width = 1.dp, color = YralColors.Neutral700, shape = RoundedCornerShape(size = 8.dp))
                .fillMaxWidth()
                .background(color = YralColors.Neutral800, shape = RoundedCornerShape(size = 8.dp))
                .background(color = YralColors.ScrimColorBalance, shape = RoundedCornerShape(size = 8.dp))
                .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Image(
                    painter = painterResource(DesignRes.drawable.yral),
                    contentDescription = stringResource(DesignRes.string.coins),
                    contentScale = ContentScale.Inside,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = stringResource(DesignRes.string.coins),
                    style = LocalAppTopography.current.xlBold,
                    color = YralColors.NeutralTextPrimary,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = coinBalance.toString(),
                style = LocalAppTopography.current.xlBold,
                color = YralColors.NeutralTextPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun BitCoinBalance(
    balance: Double,
    btcConversionRate: Double?,
    currencyCode: String?,
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    .border(
                        width = 1.dp,
                        color = YralColors.Neutral700,
                        shape = RoundedCornerShape(size = 8.dp),
                    ).fillMaxWidth(),
        ) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .background(
                                color = YralColors.Neutral800,
                                shape = RoundedCornerShape(size = 8.dp),
                            ).background(
                                color = YralColors.ScrimColorBalance,
                                shape = RoundedCornerShape(size = 8.dp),
                            ).padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.bitcoin),
                            contentDescription = stringResource(Res.string.bit_coin),
                            contentScale = ContentScale.Inside,
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            text = stringResource(Res.string.bit_coin),
                            style = LocalAppTopography.current.xlBold,
                            color = YralColors.NeutralTextPrimary,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        text = NumberFormatter().format(value = balance, maximumFractionDigits = 8),
                        style = LocalAppTopography.current.xlBold,
                        color = YralColors.NeutralTextPrimary,
                        textAlign = TextAlign.Center,
                    )
                }
                CurrencyBalanceRow(
                    btcBalance = balance,
                    btcConversionRate = btcConversionRate,
                    currencyCode = currencyCode,
                )
            }
        }
        BtcInCurrency(btcConversionRate, currencyCode)
    }
}

@Suppress("LongMethod")
@Composable
private fun CurrencyBalanceRow(
    btcConversionRate: Double?,
    currencyCode: String?,
    btcBalance: Double,
) {
    btcConversionRate?.let {
        currencyCode?.let {
            val shapeForINRBalance =
                RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp,
                )
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .height(44.dp)
                        .fillMaxWidth()
                        .background(color = YralColors.Neutral700, shape = shapeForINRBalance)
                        .background(
                            color = YralColors.ScrimColorBalance,
                            shape = shapeForINRBalance,
                        ).padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(DesignRes.string.current_balance),
                        style = LocalAppTopography.current.regRegular,
                        color = YralColors.NeutralTextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    if (currencyCode == "INR") {
                        Image(
                            painter = painterResource(Res.drawable.ic_rupee),
                            contentDescription = "image description",
                            contentScale = ContentScale.None,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .height(28.dp)
                                .background(
                                    color = YralColors.Green400,
                                    shape = RoundedCornerShape(size = 8.dp),
                                ).padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp),
                    ) {
                        Text(
                            text = (btcBalance * btcConversionRate).toCurrencyString(currencyCode),
                            style = LocalAppTopography.current.baseSemiBold,
                            textAlign = TextAlign.Right,
                            color = YralColors.Neutral50,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BtcInCurrency(
    btcConversionRate: Double?,
    currencyCode: String?,
) {
    btcConversionRate?.let {
        currencyCode?.let {
            Text(
                text = stringResource(Res.string.btc_inr_rate, btcConversionRate.toCurrencyString(currencyCode)),
                style = LocalAppTopography.current.regRegular,
                color = YralColors.NeutralTextSecondary,
                textAlign = TextAlign.End,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            )
        }
    }
}

private fun Double.toCurrencyString(currencyCode: String) =
    CurrencyFormatter()
        .format(
            amount = this,
            currencyCode = currencyCode,
            withCurrencySymbol = true,
            minimumFractionDigits = 2,
            maximumFractionDigits = 2,
        )

@Suppress("LongMethod", "UnusedParameter")
@Composable
private fun HowToEarnBitcoinSheet(rewardAnimationUrl: String?) {
    Column(
        verticalArrangement = Arrangement.spacedBy(52.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 40.dp),
    ) {
        Text(
            text = stringResource(Res.string.how_to_earn_bitcoins),
            style = LocalAppTopography.current.lgBold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.White,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.width(358.dp).height(161.dp)) {
                rewardAnimationUrl?.let {
                    YralRemoteLottieAnimation(
                        url = rewardAnimationUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                    ?: YralLottieAnimation(
                        rawRes = LottieRes.BTC_REWARDS_VIEWS_ANIMATION,
                        modifier = Modifier.fillMaxSize(),
                    )
            }
            Text(
                text = buildAnnotatedGetText(),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun buildAnnotatedGetText(): AnnotatedString =
    buildAnnotatedString {
        val fullText = stringResource(Res.string.get_bitcoin)
        val maskedText = stringResource(Res.string.bit_coin)
        val maskedStart = fullText.indexOf(maskedText)
        val maskedEnd = maskedStart + maskedText.length
        val textStyle = LocalAppTopography.current.baseRegular
        val spanStyle =
            SpanStyle(
                fontSize = textStyle.fontSize,
                fontFamily = textStyle.fontFamily,
                fontWeight = textStyle.fontWeight,
            )
        if (maskedStart >= 0) {
            withStyle(
                style = spanStyle.copy(color = YralColors.Neutral300),
            ) { append(fullText.substring(0, maskedStart)) }

            withStyle(
                style =
                    spanStyle.copy(
                        brush = YralBrushes.GoldenTextBrush,
                        fontWeight = FontWeight.Bold,
                    ),
            ) { append(fullText.substring(maskedStart, maskedEnd)) }

            if (maskedEnd < fullText.length) {
                withStyle(
                    style = spanStyle.copy(color = YralColors.Neutral300),
                ) { append(fullText.substring(maskedEnd)) }
            }
        } else {
            withStyle(
                style = spanStyle.copy(color = YralColors.Neutral300),
            ) {
                append(fullText)
            }
        }
    }
