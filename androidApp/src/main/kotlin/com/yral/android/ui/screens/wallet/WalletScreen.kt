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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.profile.AccountInfoSection
import com.yral.android.ui.screens.wallet.nav.WalletComponent
import com.yral.shared.features.wallet.domain.models.BtcInInr
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
    LaunchedEffect(Unit) { viewModel.onScreenViewed() }
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WalletHeader()
        Spacer(modifier = Modifier.height(8.dp))
        state.accountInfo?.let { info ->
            AccountInfoSection(accountInfo = info)
        }
        state.yralTokenBalance?.let { coinBalance ->
            YralTokenBalance(coinBalance)
        }
        state.bitcoinBalanceInSats?.let {
            BitCoinBalance(balance = it, btcInInr = state.bitcoinValueInInr)
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
private fun YralTokenBalance(coinBalance: Long) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .border(width = 1.dp, color = YralColors.Neutral700, shape = RoundedCornerShape(size = 8.dp))
                .fillMaxWidth()
                .height(65.dp)
                .background(color = YralColors.Neutral800, shape = RoundedCornerShape(size = 8.dp))
                .background(color = YralColors.ScrimColorBalance, shape = RoundedCornerShape(size = 8.dp))
                .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp),
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
                    contentScale = ContentScale.Crop,
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

@Composable
private fun BitCoinBalance(
    balance: Double,
    btcInInr: Double?,
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
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
                            ).padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.bitcoin),
                            contentDescription = stringResource(R.string.bit_coin),
                            contentScale = ContentScale.Crop,
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
                        text = balance.toBtc(),
                        style = LocalAppTopography.current.xlBold,
                        color = YralColors.NeutralTextPrimary,
                        textAlign = TextAlign.Center,
                    )
                }
                INRBalanceRow(btcBalance = balance, btcInInr = btcInInr)
            }
        }
        BtcInInr(btcInInr)
    }
}

@Suppress("LongMethod")
@Composable
private fun INRBalanceRow(
    btcInInr: Double?,
    btcBalance: Double,
) {
    btcInInr?.let {
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
                    .background(color = YralColors.ScrimColorBalance, shape = shapeForINRBalance)
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.inr_balance),
                    style = LocalAppTopography.current.regRegular,
                    color = YralColors.NeutralTextSecondary,
                    textAlign = TextAlign.Center,
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_rupee),
                    contentDescription = "image description",
                    contentScale = ContentScale.None,
                    modifier = Modifier.size(28.dp),
                )
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
                        text = (btcBalance * btcInInr).toInrString(),
                        style = LocalAppTopography.current.baseSemiBold,
                        textAlign = TextAlign.Right,
                        color = YralColors.Neutral50,
                    )
                }
            }
        }
    }
}

@Composable
private fun BtcInInr(btcInInr: Double?) {
    btcInInr?.let {
        Text(
            text = stringResource(R.string.btc_inr_rate, btcInInr.toInrString()),
            style = LocalAppTopography.current.regRegular,
            color = YralColors.NeutralTextSecondary,
            textAlign = TextAlign.End,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

private fun Double.toInrString() =
    CurrencyFormatter()
        .format(
            amount = this,
            currencyCode = "INR",
            withCurrencySymbol = true,
            minimumFractionDigits = 2,
            maximumFractionDigits = 2,
        )

private fun Double.toBtc() =
    NumberFormatter()
        .format(this)
