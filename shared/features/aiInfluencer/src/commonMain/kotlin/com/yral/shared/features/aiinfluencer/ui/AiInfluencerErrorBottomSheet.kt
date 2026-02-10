package com.yral.shared.features.aiinfluencer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.aiinfluencer.generated.resources.Res
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_error_sheet_retry
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_error_sheet_subtitle
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_error_sheet_title
import yral_mobile.shared.features.aiinfluencer.generated.resources.create_influencer_error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiInfluencerErrorBottomSheet(
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = YralColors.Neutral900,
        dragHandle = null,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            Image(
                painter = painterResource(Res.drawable.create_influencer_error),
                contentDescription = null,
                modifier = Modifier.size(150.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(Res.string.ai_influencer_error_sheet_title),
                style = LocalAppTopography.current.lgBold,
                color = YralColors.Grey50,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.ai_influencer_error_sheet_subtitle),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.Neutral500,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
            YralGradientButton(
                text = stringResource(Res.string.ai_influencer_error_sheet_retry),
                modifier = Modifier.fillMaxWidth(),
                onClick = onRetry,
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
