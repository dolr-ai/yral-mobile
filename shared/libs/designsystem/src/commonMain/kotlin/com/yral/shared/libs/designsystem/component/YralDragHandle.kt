package com.yral.shared.libs.designsystem.component

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.theme.YralColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YralDragHandle() {
    DragHandle(modifier = Modifier.offset(y = -10.dp), height = 2.dp, width = 32.dp, color = YralColors.Neutral500)
}
