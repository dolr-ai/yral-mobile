package com.yral.shared.app.ui.screens.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.features.subscriptions.nav.SubscriptionNudgeContent
import com.yral.shared.features.subscriptions.ui.components.BoltIcon
import com.yral.shared.features.subscriptions.ui.components.SubscriptionNudgeGenericBenefits
import com.yral.shared.features.subscriptions.ui.components.SubscriptionNudgeGenericTitle
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.subscription_nudge_cta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionNudgeBottomSheet(
    content: SubscriptionNudgeContent,
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
    onSubscribe: () -> Unit,
) {
    YralBottomSheet(
        onDismissRequest = onDismissRequest,
        bottomSheetState = bottomSheetState,
        dragHandle = { DragHandle(color = YralColors.Neutral500) },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        top = 24.dp,
                        end = 16.dp,
                        bottom = 36.dp,
                    ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content.topContent()

            Spacer(modifier = Modifier.height(24.dp))

            content.title?.let { title ->
                Text(
                    text = title,
                    style = LocalAppTopography.current.xlSemiBold,
                    textAlign = TextAlign.Center,
                    color = YralColors.Neutral50,
                    modifier = Modifier.fillMaxWidth(),
                )
            } ?: SubscriptionNudgeGenericTitle(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(12.dp))

            content.description?.let { description ->
                Text(
                    text = description,
                    style = LocalAppTopography.current.regRegular,
                    textAlign = TextAlign.Center,
                    color = YralColors.Neutral300,
                    modifier = Modifier.fillMaxWidth(),
                )
            } ?: SubscriptionNudgeGenericBenefits(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(24.dp))

            YralGradientButton(
                text = stringResource(Res.string.subscription_nudge_cta),
                onClick = onSubscribe,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Suppress("UnusedPrivateMember")
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun SubscriptionNudgeBottomSheetPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val content =
            SubscriptionNudgeContent(
                title = "Upgrade to Pro",
                description = "Get unlimited messages and more with Yral Pro.",
                topContent = { BoltIcon() },
            )
        SubscriptionNudgeBottomSheet(
            content = content,
            bottomSheetState = sheetState,
            onDismissRequest = {},
            onSubscribe = {},
        )
    }
}

@Suppress("UnusedPrivateMember")
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun SubscriptionNudgeBottomSheetGenericPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val content =
            SubscriptionNudgeContent(
                title = null,
                description = null,
                topContent = { BoltIcon() },
            )
        SubscriptionNudgeBottomSheet(
            content = content,
            bottomSheetState = sheetState,
            onDismissRequest = {},
            onSubscribe = {},
        )
    }
}
