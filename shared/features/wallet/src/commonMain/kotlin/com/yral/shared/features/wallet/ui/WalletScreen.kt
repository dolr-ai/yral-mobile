@file:Suppress("LongMethod", "MagicNumber")

package com.yral.shared.features.wallet.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.features.wallet.nav.WalletComponent
import com.yral.shared.features.wallet.viewmodel.WalletViewModel
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.rust.service.utils.CanisterData
import com.yral.shared.rust.service.utils.getUserInfoServiceCanister
import com.yral.shared.rust.service.utils.propicFromPrincipal
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.features.wallet.generated.resources.Res
import yral_mobile.shared.features.wallet.generated.resources.create_influencer
import yral_mobile.shared.features.wallet.generated.resources.got_it
import yral_mobile.shared.features.wallet.generated.resources.history
import yral_mobile.shared.features.wallet.generated.resources.how_to_earn
import yral_mobile.shared.features.wallet.generated.resources.ic_lock
import yral_mobile.shared.features.wallet.generated.resources.ic_lock_small
import yral_mobile.shared.features.wallet.generated.resources.ic_rupee_coin
import yral_mobile.shared.features.wallet.generated.resources.min_withdrawal_limit
import yral_mobile.shared.features.wallet.generated.resources.my_wallet
import yral_mobile.shared.features.wallet.generated.resources.step_1_desc
import yral_mobile.shared.features.wallet.generated.resources.step_1_title
import yral_mobile.shared.features.wallet.generated.resources.step_2_desc
import yral_mobile.shared.features.wallet.generated.resources.step_2_title
import yral_mobile.shared.features.wallet.generated.resources.step_3_desc
import yral_mobile.shared.features.wallet.generated.resources.step_3_title
import yral_mobile.shared.features.wallet.generated.resources.step_4_desc
import yral_mobile.shared.features.wallet.generated.resources.step_4_title
import yral_mobile.shared.features.wallet.generated.resources.switch_profile
import yral_mobile.shared.features.wallet.generated.resources.total_earnings_inr
import yral_mobile.shared.features.wallet.generated.resources.transaction_history
import yral_mobile.shared.features.wallet.generated.resources.wallet_locked
import yral_mobile.shared.features.wallet.generated.resources.wallet_locked_description
import yral_mobile.shared.features.wallet.generated.resources.wallet_locked_switch_profile_description
import yral_mobile.shared.libs.designsystem.generated.resources.arrow
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    component: WalletComponent,
    onCreateInfluencerClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onScreenViewed() }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accessState =
        resolveWalletAccessState(
            isSocialSignedIn = state.isSocialSignedIn,
            hasBots = state.hasBots,
            isBotAccount = state.isBotAccount,
        )

    if (state.showTransactionHistory) {
        TransactionHistoryScreen(
            state = state,
            onBack = { viewModel.toggleTransactionHistory(false) },
            onOpenProfile = component.onOpenProfile,
        )
    } else {
        when (accessState) {
            WalletAccessState.Unlocked ->
                WalletUnlockedContent(
                    component = component,
                    state = state,
                    viewModel = viewModel,
                    modifier = modifier,
                )

            is WalletAccessState.Locked ->
                WalletLockedContent(
                    component = component,
                    ctaText =
                        when (accessState.cta) {
                            WalletLockedCta.CreateInfluencer ->
                                stringResource(Res.string.create_influencer)
                            WalletLockedCta.SwitchProfile ->
                                stringResource(Res.string.switch_profile)
                        },
                    onCtaClick =
                        when (accessState.cta) {
                            WalletLockedCta.CreateInfluencer -> onCreateInfluencerClick
                            WalletLockedCta.SwitchProfile -> component.onSwitchProfile
                        },
                    modifier = modifier,
                    descriptionOverride =
                        when (accessState.subtitle) {
                            WalletLockedSubtitle.Default -> null
                            WalletLockedSubtitle.SwitchProfile ->
                                stringResource(Res.string.wallet_locked_switch_profile_description)
                        },
                )
        }
    }

    if (state.howToEarnVisible) {
        YralBottomSheet(
            onDismissRequest = { viewModel.toggleHowToEarnHelp(false) },
            bottomSheetState = bottomSheetState,
            content = {
                HowToEarnSheet(onDismiss = { viewModel.toggleHowToEarnHelp(false) })
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
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = stringResource(Res.string.my_wallet),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
        )
    }
}

@Composable
private fun WalletLockedContent(
    component: WalletComponent,
    ctaText: String?,
    onCtaClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    descriptionOverride: String? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WalletHeader(component = component)
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(Res.drawable.ic_lock),
            contentDescription = stringResource(Res.string.wallet_locked),
            modifier = Modifier.size(120.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.wallet_locked),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = descriptionOverride ?: stringResource(Res.string.wallet_locked_description),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        if (ctaText != null && onCtaClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            YralGradientButton(
                text = ctaText,
                onClick = onCtaClick,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun WalletUnlockedContent(
    component: WalletComponent,
    state: com.yral.shared.features.wallet.viewmodel.WalletState,
    viewModel: WalletViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        WalletHeader(component = component)
        if (state.isLoading) {
            Spacer(modifier = Modifier.weight(1f))
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            EarningsCard(totalEarnings = state.totalEarningsInr)
            Spacer(modifier = Modifier.height(16.dp))
            TransactionHistoryCard(onClick = { viewModel.toggleTransactionHistory(true) })
            Spacer(modifier = Modifier.height(16.dp))
            HowToEarnButton(onClick = { viewModel.toggleHowToEarnHelp(true) })
        }
    }
}

@Composable
private fun EarningsCard(totalEarnings: String) {
    val gradientBrush =
        Brush.linearGradient(
            colorStops =
                arrayOf(
                    0.0f to Color(0xFFFF78C1),
                    0.51f to Color(0xFFE2017B),
                    1.0f to Color(0xFFAD005E),
                ),
            start = Offset(Float.POSITIVE_INFINITY, 0f),
            end = Offset(0f, Float.POSITIVE_INFINITY),
        )
    Column(
        modifier =
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .background(brush = gradientBrush, shape = RoundedCornerShape(12.dp))
                .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.total_earnings_inr),
                style = LocalAppTopography.current.baseSemiBold,
                color = Color.White.copy(alpha = 0.8f),
            )
            Text(
                text = totalEarnings,
                style = LocalAppTopography.current.xxlBold.copy(fontSize = 32.sp),
                color = Color.White,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_lock_small),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(Res.string.min_withdrawal_limit),
                style = LocalAppTopography.current.regRegular,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun TransactionHistoryCard(onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .background(color = YralColors.Neutral800, shape = RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.transaction_history),
            style = LocalAppTopography.current.mdBold,
            color = YralColors.NeutralTextPrimary,
        )
        Icon(
            painter = painterResource(DesignRes.drawable.arrow),
            contentDescription = null,
            tint = YralColors.NeutralTextSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun HowToEarnButton(onClick: () -> Unit) {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        YralButton(
            text = stringResource(Res.string.how_to_earn),
            onClick = onClick,
            modifier = Modifier.width(162.dp).height(40.dp),
            textStyle = LocalAppTopography.current.baseBold,
        )
    }
}

@Composable
private fun HowToEarnSheet(onDismiss: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_rupee_coin),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
        )

        Text(
            text = stringResource(Res.string.how_to_earn),
            style = LocalAppTopography.current.xxlBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(color = YralColors.Neutral800, shape = RoundedCornerShape(8.dp))
                    .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            EarnStep(
                number = "1",
                title = stringResource(Res.string.step_1_title),
                description = stringResource(Res.string.step_1_desc),
            )
            EarnStep(
                number = "2",
                title = stringResource(Res.string.step_2_title),
                description = stringResource(Res.string.step_2_desc),
            )
            EarnStep(
                number = "3",
                title = stringResource(Res.string.step_3_title),
                description = stringResource(Res.string.step_3_desc),
            )
            EarnStep(
                number = "4",
                title = stringResource(Res.string.step_4_title),
                description = stringResource(Res.string.step_4_desc),
            )
        }

        YralGradientButton(
            text = stringResource(Res.string.got_it),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun EarnStep(
    number: String,
    title: String,
    description: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "$number.",
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.Yellow200,
            )
            Text(
                text = title,
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.Yellow200,
            )
        }
        Text(
            text = description,
            style = LocalAppTopography.current.baseRegular,
            color = Color.White,
        )
    }
}

@Composable
private fun TransactionHistoryScreen(
    state: com.yral.shared.features.wallet.viewmodel.WalletState,
    onBack: () -> Unit,
    onOpenProfile: (CanisterData) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(DesignRes.drawable.arrow_left),
                contentDescription = "back",
                tint = Color.White,
                modifier =
                    Modifier
                        .size(24.dp)
                        .clickable(onClick = onBack),
            )
            Text(
                text = stringResource(Res.string.history),
                style = LocalAppTopography.current.xlBold,
                color = YralColors.NeutralTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.size(24.dp))
        }
        if (state.isTransactionsLoading) {
            Spacer(modifier = Modifier.weight(1f))
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(state.transactions) { index, tx ->
                    TransactionItem(
                        title = "@${tx.username ?: tx.userId}",
                        subtitle = "${formatTransactionDate(tx.createdAt)}  \u2022  1 Day Subscription",
                        amount = "+ \u20B9${tx.amountPaise / PAISE_PER_RUPEE}",
                        onClick = {
                            onOpenProfile(
                                CanisterData(
                                    canisterId = getUserInfoServiceCanister(),
                                    userPrincipalId = tx.userId,
                                    profilePic = propicFromPrincipal(tx.userId),
                                    username = tx.username,
                                    isCreatedFromServiceCanister = true,
                                ),
                            )
                        },
                    )
                    if (index < state.transactions.lastIndex) {
                        HorizontalDivider(
                            color = YralColors.Neutral700,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(
    title: String,
    subtitle: String,
    amount: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = LocalAppTopography.current.regRegular,
                color = YralColors.NeutralTextTertiary,
            )
        }
        Text(
            text = amount,
            style = LocalAppTopography.current.baseSemiBold,
            color = YralColors.Green300,
        )
    }
}
