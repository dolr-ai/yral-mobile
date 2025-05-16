package com.yral.android.ui.design

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YralBottomSheet(
    onDismissRequest: () -> Unit,
    bottomSheetState: SheetState,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        modifier = Modifier.safeDrawingPadding(),
        onDismissRequest = onDismissRequest,
        sheetState = bottomSheetState,
        containerColor = YralColors.Neutral900,
        dragHandle = null,
        content = content,
    )
}
