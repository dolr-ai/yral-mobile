package com.yral.android.ui.screens.wallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.wallet.nav.WalletComponent
import com.yral.android.ui.widgets.YralAsyncImage
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.features.wallet.viewmodel.WalletViewModel
import com.yral.shared.libs.CurrencyFormatter
import com.yral.shared.libs.NumberFormatter
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WalletScreen(
    @Suppress("UnusedParameter")
    component: WalletComponent,
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val countryCode = Locale.current.region
    LaunchedEffect(Unit) {
        viewModel.refresh(countryCode)
        viewModel.onScreenViewed()
    }
    Column(modifier = modifier.fillMaxSize()) {
        WalletHeader()
        Spacer(modifier = Modifier.height(16.dp))
        state.accountInfo?.let { info ->
            AccountInfoSection(accountInfo = info)
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
            text = stringResource(R.string.my_wallet),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
        )
    }
}

@Composable
private fun AccountInfoSection(accountInfo: AccountInfo) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            YralAsyncImage(
                imageUrl = accountInfo.profilePic,
                modifier = Modifier.size(60.dp),
            )
            Text(
                text = accountInfo.userPrincipal,
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.NeutralTextSecondary,
            )
        }
        Spacer(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(YralColors.Divider),
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
                    painter = painterResource(id = R.drawable.yral),
                    contentDescription = stringResource(R.string.coins),
                    contentScale = ContentScale.Inside,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = stringResource(R.string.coins),
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
                            painter = painterResource(id = R.drawable.bitcoin),
                            contentDescription = stringResource(R.string.bit_coin),
                            contentScale = ContentScale.Inside,
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            text = stringResource(R.string.bit_coin),
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
                        text = stringResource(R.string.current_balance),
                        style = LocalAppTopography.current.regRegular,
                        color = YralColors.NeutralTextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    if (currencyCode == "INR") {
                        Image(
                            painter = painterResource(id = R.drawable.ic_rupee),
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
                text = stringResource(R.string.btc_inr_rate, btcConversionRate.toCurrencyString(currencyCode)),
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
