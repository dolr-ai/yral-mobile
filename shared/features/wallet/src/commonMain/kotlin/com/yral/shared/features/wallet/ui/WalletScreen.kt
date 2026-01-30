package com.yral.shared.features.wallet.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.features.wallet.nav.WalletComponent
import com.yral.shared.features.wallet.viewmodel.WalletViewModel
import com.yral.shared.libs.CurrencyFormatter
import com.yral.shared.libs.NumberFormatter
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.component.features.AccountInfoView
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.features.wallet.generated.resources.Res
import yral_mobile.shared.features.wallet.generated.resources.bit_coin
import yral_mobile.shared.features.wallet.generated.resources.bitcoin
import yral_mobile.shared.features.wallet.generated.resources.currency_balance
import yral_mobile.shared.features.wallet.generated.resources.dolr
import yral_mobile.shared.features.wallet.generated.resources.engaged_view_equals_dolr
import yral_mobile.shared.features.wallet.generated.resources.engaged_views_description
import yral_mobile.shared.features.wallet.generated.resources.get_engaged_views
import yral_mobile.shared.features.wallet.generated.resources.got_it
import yral_mobile.shared.features.wallet.generated.resources.how_to_get_dolr
import yral_mobile.shared.features.wallet.generated.resources.how_to_get_dolr_title
import yral_mobile.shared.features.wallet.generated.resources.ic_dolr
import yral_mobile.shared.features.wallet.generated.resources.ic_dolr_stack
import yral_mobile.shared.features.wallet.generated.resources.my_wallet
import yral_mobile.shared.features.wallet.generated.resources.what_are_engaged_views
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.coins
import yral_mobile.shared.libs.designsystem.generated.resources.yral
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    component: WalletComponent,
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val countryCode = Locale.current.region
    LaunchedEffect(Unit) { viewModel.onScreenViewed() }
    LaunchedEffect(state.isFirebaseLoggedIn) {
        viewModel.refresh(countryCode)
    }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Column(modifier = modifier.fillMaxSize()) {
        WalletHeader(component = component)
        Spacer(modifier = Modifier.height(4.dp))
        state.accountInfo?.let { info ->
            // Defaults since login not required on wallet
            AccountInfoView(
                accountInfo = info,
                isSocialSignIn = true,
                onLoginClicked = {},
                isProUser = state.isProUser,
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
        state.dolrBalance?.let {
            DolrBalance(
                balance = it,
                dolrConversionRate = state.dolrConversionRate,
                currencyCode = state.btcConversionCurrency,
            )
        }
        state.rewardConfig?.let {
            YralButton(
                text = stringResource(Res.string.how_to_get_dolr),
                onClick = { viewModel.toggleHowToGetDolrHelp(true) },
                modifier =
                    Modifier
                        .padding(start = 16.dp, top = 24.dp)
                        .width(162.dp)
                        .height(40.dp),
                textStyle = LocalAppTopography.current.baseBold,
            )
        }
    }
    if (state.howToGetDolrHelpVisible) {
        YralBottomSheet(
            onDismissRequest = {
                viewModel.toggleHowToGetDolrHelp(false)
                component.showAlertsOnDialog(AlertsRequestType.DEFAULT)
            },
            bottomSheetState = bottomSheetState,
            dragHandle = null,
            content = {
                HowToGetDolrSheet(
                    onDismiss = {
                        viewModel.toggleHowToGetDolrHelp(false)
                        component.showAlertsOnDialog(AlertsRequestType.DEFAULT)
                    },
                )
            },
        )
    }
}

@Composable
private fun WalletHeader(component: WalletComponent) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (component.showBackIcon) {
            Icon(
                painter = painterResource(DesignRes.drawable.arrow_left),
                contentDescription = "back",
                tint = Color.White,
                modifier =
                    Modifier
                        .size(24.dp)
                        .clickable { component.onBack() },
            )
        }
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
                    .padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
                    .border(
                        width = 1.dp,
                        color = YralColors.Neutral700,
                        shape = RoundedCornerShape(size = 8.dp),
                    ).fillMaxWidth()
                    .height(65.dp)
                    .background(
                        color = YralColors.Neutral800,
                        shape = RoundedCornerShape(size = 8.dp),
                    ).background(
                        color = YralColors.ScrimColorBalance,
                        shape = RoundedCornerShape(size = 8.dp),
                    ).padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically,
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
                style = LocalAppTopography.current.xxlBold,
                color = YralColors.NeutralTextPrimary,
                textAlign = TextAlign.Center,
            )
        }
        BtcConversionRow(
            btcBalance = balance,
            btcConversionRate = btcConversionRate,
            currencyCode = currencyCode,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun BtcConversionRow(
    btcBalance: Double,
    btcConversionRate: Double?,
    currencyCode: String?,
) {
    if (btcConversionRate == null || currencyCode == null) return

    val currencySymbol = if (currencyCode == "INR") "₹" else "$"
    val formattedRate =
        NumberFormatter().format(
            value = btcConversionRate,
            maximumFractionDigits = 2,
        )

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
    ) {
        // Left: "1 Bitcoin = ₹ X INR" (conversion rate with bold amount)
        Row {
            Text(
                text = "1 Bitcoin = $currencySymbol ",
                style = LocalAppTopography.current.regRegular,
                color = YralColors.NeutralTextSecondary,
            )
            Text(
                text = formattedRate,
                style = LocalAppTopography.current.regBold,
                color = YralColors.NeutralTextSecondary,
            )
            Text(
                text = " $currencyCode",
                style = LocalAppTopography.current.regRegular,
                color = YralColors.NeutralTextSecondary,
            )
        }
        // Right: "INR Balance: ₹X" (btcBalance * conversion rate)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.currency_balance, currencyCode),
                style = LocalAppTopography.current.regRegular,
                color = YralColors.NeutralTextPrimary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .background(
                            color = YralColors.Green400,
                            shape = RoundedCornerShape(size = 6.dp),
                        ).padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = (btcBalance * btcConversionRate).toCurrencyString(currencyCode),
                    style = LocalAppTopography.current.regSemiBold,
                    color = YralColors.Neutral50,
                )
            }
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

@Composable
private fun DolrBalance(
    balance: Double,
    dolrConversionRate: Double?,
    currencyCode: String?,
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
                    .border(
                        width = 1.dp,
                        color = YralColors.Neutral700,
                        shape = RoundedCornerShape(size = 8.dp),
                    ).fillMaxWidth()
                    .height(65.dp)
                    .background(
                        color = YralColors.Neutral800,
                        shape = RoundedCornerShape(size = 8.dp),
                    ).background(
                        color = YralColors.ScrimColorBalance,
                        shape = RoundedCornerShape(size = 8.dp),
                    ).padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_dolr),
                    contentDescription = stringResource(Res.string.dolr),
                    contentScale = ContentScale.Inside,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = stringResource(Res.string.dolr),
                    style = LocalAppTopography.current.xlBold,
                    color = YralColors.NeutralTextPrimary,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = NumberFormatter().format(value = balance, maximumFractionDigits = 2),
                style = LocalAppTopography.current.xxlBold,
                color = YralColors.NeutralTextPrimary,
                textAlign = TextAlign.Center,
            )
        }
        DolrConversionRow(
            dolrBalance = balance,
            dolrConversionRate = dolrConversionRate,
            currencyCode = currencyCode,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun DolrConversionRow(
    dolrBalance: Double,
    dolrConversionRate: Double?,
    currencyCode: String?,
) {
    if (dolrConversionRate == null || currencyCode == null) return

    val currencySymbol = if (currencyCode == "INR") "₹" else "$"
    val formattedRate =
        NumberFormatter().format(
            value = dolrConversionRate,
            maximumFractionDigits = 2,
        )

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
    ) {
        // Left: "1 DOLR = ₹ X INR" (conversion rate with bold amount)
        Row {
            Text(
                text = "1 DOLR = $currencySymbol ",
                style = LocalAppTopography.current.regRegular,
                color = YralColors.NeutralTextSecondary,
            )
            Text(
                text = formattedRate,
                style = LocalAppTopography.current.regBold,
                color = YralColors.NeutralTextSecondary,
            )
            Text(
                text = " $currencyCode",
                style = LocalAppTopography.current.regRegular,
                color = YralColors.NeutralTextSecondary,
            )
        }
        // Right: "INR Balance: ₹X" (dolrBalance * conversion rate)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.currency_balance, currencyCode),
                style = LocalAppTopography.current.regRegular,
                color = YralColors.NeutralTextPrimary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .background(
                            color = YralColors.Green400,
                            shape = RoundedCornerShape(size = 6.dp),
                        ).padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = (dolrBalance * dolrConversionRate).toCurrencyString(currencyCode),
                    style = LocalAppTopography.current.regSemiBold,
                    color = YralColors.Neutral50,
                )
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun HowToGetDolrSheet(onDismiss: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
    ) {
        // Stacked coins image
        Image(
            painter = painterResource(Res.drawable.ic_dolr_stack),
            contentDescription = stringResource(Res.string.dolr),
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(width = 100.dp, height = 90.dp),
        )

        // Text content section
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // "How to get DOLR?" - yellow title
            Text(
                text = stringResource(Res.string.how_to_get_dolr_title),
                style = LocalAppTopography.current.mdBold,
                color = YralColors.Yellow200,
                textAlign = TextAlign.Center,
            )

            // "Get engaged views on your video."
            Text(
                text = stringResource(Res.string.get_engaged_views),
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.NeutralTextPrimary,
                textAlign = TextAlign.Center,
            )

            // "1 Engaged View = 1 DOLR" with metallic gradient and coin icon
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                @Suppress("MagicNumber")
                val metallicGradient =
                    Brush.horizontalGradient(
                        colors =
                            listOf(
                                Color(0xFF5A5A5A),
                                Color(0xFF838383),
                                Color(0xFFACACAC),
                                Color(0xFFD6D6D6),
                                Color(0xFFFFFFFF),
                                Color(0xFFDDDDDD),
                                Color(0xFFBBBBBB),
                                Color(0xFF989898),
                                Color(0xFF767676),
                            ),
                    )
                Text(
                    text = stringResource(Res.string.engaged_view_equals_dolr),
                    style = LocalAppTopography.current.baseMedium.copy(brush = metallicGradient),
                    textAlign = TextAlign.Center,
                )
                Image(
                    painter = painterResource(Res.drawable.ic_dolr),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // "What are Engaged Views?" card
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = YralColors.Neutral800,
                        shape = RoundedCornerShape(8.dp),
                    ).padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.what_are_engaged_views),
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.Neutral50,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.engaged_views_description),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextSecondary,
                textAlign = TextAlign.Center,
            )
        }

        // "Got It!" button with pink gradient
        YralGradientButton(
            text = stringResource(Res.string.got_it),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
