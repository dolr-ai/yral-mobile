package com.yral.android.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.theme.LocalAppTopography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YralErrorMessage(
    title: String,
    error: String,
    sheetState: SheetState,
    cta: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    YralBottomSheet(
        onDismissRequest = onDismiss,
        bottomSheetState = sheetState,
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
            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    style = LocalAppTopography.current.xlSemiBold,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = error,
                    style = LocalAppTopography.current.regRegular,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                )
            }
            YralGradientButton(
                text = cta,
                onClick = onClick,
            )
        }
    }
}
