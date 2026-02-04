package com.yral.shared.features.subscriptions.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.subscriptions.generated.resources.Res
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_benefit_ai
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_benefit_chat
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_benefit_global
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_benefit_rewards
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_nudge_benefit_ai
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_nudge_benefit_chat
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_nudge_benefit_global
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_nudge_benefit_rewards

@Composable
fun SubscriptionNudgeGenericBenefits(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SubscriptionBenefitRow(
            iconRes = Res.drawable.subscription_benefit_ai,
            text = stringResource(Res.string.subscription_nudge_benefit_ai),
        )
        SubscriptionBenefitRow(
            iconRes = Res.drawable.subscription_benefit_chat,
            text = stringResource(Res.string.subscription_nudge_benefit_chat),
        )
        SubscriptionBenefitRow(
            iconRes = Res.drawable.subscription_benefit_global,
            text = stringResource(Res.string.subscription_nudge_benefit_global),
        )
        SubscriptionBenefitRow(
            iconRes = Res.drawable.subscription_benefit_rewards,
            text = stringResource(Res.string.subscription_nudge_benefit_rewards),
        )
    }
}
