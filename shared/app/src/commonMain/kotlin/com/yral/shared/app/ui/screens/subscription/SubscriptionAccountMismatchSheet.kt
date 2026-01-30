package com.yral.shared.app.ui.screens.subscription

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.subscription_account_mismatch_button
import yral_mobile.shared.app.generated.resources.subscription_account_mismatch_message
import yral_mobile.shared.app.generated.resources.subscription_account_mismatch_title
import yral_mobile.shared.libs.designsystem.generated.resources.warning
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionAccountMismatchSheet(
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
    onUseAnotherAccount: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Warning icon
            Image(
                painter = painterResource(DesignRes.drawable.warning),
                contentDescription = "Warning",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(80.dp),
            )

            // Title
            Text(
                text = stringResource(Res.string.subscription_account_mismatch_title),
                style = LocalAppTopography.current.xlSemiBold,
                textAlign = TextAlign.Center,
                color = YralColors.Neutral50,
                modifier = Modifier.fillMaxWidth(),
            )

            // Message
            Text(
                text = stringResource(Res.string.subscription_account_mismatch_message),
                style = LocalAppTopography.current.regRegular,
                textAlign = TextAlign.Center,
                color = YralColors.Neutral300,
                modifier = Modifier.fillMaxWidth(),
            )

            // Button
            YralGradientButton(
                text = stringResource(Res.string.subscription_account_mismatch_button),
                onClick = onUseAnotherAccount,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
