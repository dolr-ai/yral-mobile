package com.yral.android.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YralConfirmationMessage(
    title: String,
    subTitle: String,
    sheetState: SheetState,
    cancel: String,
    done: String,
    isDoneDismiss: Boolean = true,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    YralBottomSheet(
        onDismissRequest = if (isDoneDismiss) onDone else onCancel,
        bottomSheetState = sheetState,
        dragHandle = { DragHandle(color = YralColors.Neutral500) },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    style = LocalAppTopography.current.lgBold,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = subTitle,
                    style = LocalAppTopography.current.mdMedium,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                verticalAlignment = Alignment.Top,
            ) {
                YralButton(
                    modifier = Modifier.weight(1f),
                    text = cancel,
                    borderColor = YralColors.Neutral700,
                    borderWidth = 1.dp,
                    backgroundColor = YralColors.Neutral800,
                    textStyle = TextStyle(color = YralColors.NeutralTextPrimary),
                    onClick = onCancel,
                )
                YralGradientButton(
                    modifier = Modifier.weight(1f),
                    text = done,
                    onClick = onDone,
                )
            }
        }
    }
}
