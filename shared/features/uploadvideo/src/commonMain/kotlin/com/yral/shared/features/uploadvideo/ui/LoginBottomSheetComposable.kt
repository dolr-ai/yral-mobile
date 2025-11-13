package com.yral.shared.features.uploadvideo.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import com.yral.shared.analytics.events.SignupPageName

@OptIn(ExperimentalMaterial3Api::class)
typealias LoginBottomSheetComposable = @Composable (
    pageName: SignupPageName,
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
    termsLink: String,
    openTerms: () -> Unit,
) -> Unit
