package com.yral.android.ui.components.signup

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.yral.android.ui.screens.account.WebViewBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraLinkSheet(
    extraSheetLink: String,
    onDismissRequest: () -> Unit,
) {
    val extraSheetState = rememberModalBottomSheetState()
    LaunchedEffect(extraSheetLink) {
        if (extraSheetLink.isEmpty()) {
            extraSheetState.hide()
        } else {
            extraSheetState.show()
        }
    }
    WebViewBottomSheet(
        link = extraSheetLink,
        bottomSheetState = extraSheetState,
        onDismissRequest = onDismissRequest,
    )
}
