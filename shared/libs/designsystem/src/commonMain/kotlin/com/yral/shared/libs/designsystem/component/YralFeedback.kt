package com.yral.shared.libs.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
expect fun YralFeedback(
    sound: Int,
    withHapticFeedback: Boolean = false,
    hapticFeedbackType: HapticFeedbackType = HapticFeedbackType.ContextClick,
    onPlayed: () -> Unit = {},
)

expect fun popPressedSoundId(): Int
