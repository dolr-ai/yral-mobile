package com.yral.android.ui.screens.account

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.widgets.YralBottomSheet
import com.yral.android.ui.widgets.YralWebView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewBottomSheet(
    link: String,
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
) {
    YralBottomSheet(
        onDismissRequest = onDismissRequest,
        bottomSheetState = bottomSheetState,
        dragHandle = {
            DragHandle(
                color = YralColors.Pink300,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                YralWebView(
                    url = link,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
