package com.yral.shared.libs.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.yral.shared.libs.designsystem.theme.YralColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YralBottomSheet(
    onDismissRequest: () -> Unit,
    bottomSheetState: SheetState,
    shouldDismissOnBackPress: Boolean = true,
    dragHandle: @Composable (() -> Unit)? = null,
    containerColor: Color = YralColors.Neutral900,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // A bg color for ios liquid glass keyboard
        val imeHeightPx = WindowInsets.ime.getBottom(LocalDensity.current)
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(with(LocalDensity.current) { imeHeightPx.toDp() })
                    .background(YralColors.Neutral900)
                    .align(Alignment.BottomCenter),
        )
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = bottomSheetState,
            scrimColor = YralColors.ScrimColor,
            containerColor = containerColor,
            dragHandle = dragHandle,
            content = content,
            modifier = Modifier.padding(WindowInsets.statusBars.only(WindowInsetsSides.Top).asPaddingValues()),
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = shouldDismissOnBackPress),
        )
    }
}
