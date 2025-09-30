package com.yral.shared.libs.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
actual fun YralFeedback(
    sound: Int,
    withHapticFeedback: Boolean,
    hapticFeedbackType: HapticFeedbackType,
    onPlayed: () -> Unit,
) {
    // stub
}
