package com.yral.android.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.yral.android.ui.design.LocalAppTopography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YralErrorMessage(
    modifier: Modifier = Modifier,
    sheetHorizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    title: String = "",
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
            horizontalAlignment = sheetHorizontalAlignment,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.padding(16.dp),
        ) {
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    style = LocalAppTopography.current.mdMedium,
                    textAlign = TextAlign.Start,
                    color = Color.White,
                )
            }
            Text(
                text = error,
                style = LocalAppTopography.current.mdMedium,
                textAlign = TextAlign.Start,
                color = Color.White,
            )
            YralGradientButton(
                text = cta,
                onClick = onClick,
            )
        }
    }
}
