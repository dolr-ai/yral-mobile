package com.yral.shared.libs.designsystem.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.yral.shared.libs.designsystem.theme.YralColors

@Composable
expect fun YralWebView(
    url: String,
    modifier: Modifier = Modifier,
    maxRetries: Int = 3,
    retryDelayMillis: Long = 1000,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YralWebViewBottomSheet(
    link: String,
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
) {
    LaunchedEffect(link) {
        if (link.isEmpty()) {
            bottomSheetState.hide()
        } else {
            bottomSheetState.show()
        }
    }
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
