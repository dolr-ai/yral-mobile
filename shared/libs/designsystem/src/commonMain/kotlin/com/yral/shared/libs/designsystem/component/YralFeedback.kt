package com.yral.shared.libs.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import co.touchlab.kermit.Logger
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.crashlytics.core.CrashlyticsManager
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import yral_mobile.shared.libs.designsystem.generated.resources.Res
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

@Composable
fun YralFeedback(
    soundUri: String,
    withHapticFeedback: Boolean = false,
    hapticFeedbackType: HapticFeedbackType = HapticFeedbackType.ContextClick,
    onPlayed: () -> Unit = {},
) {
    val crashlyticsManager = koinInject<CrashlyticsManager>()
    SoundFeedback(soundUri, onPlayed, crashlyticsManager)
    if (withHapticFeedback) {
        HapticFeeback(hapticFeedbackType, crashlyticsManager)
    }
}

@Composable
internal expect fun SoundFeedback(
    soundUri: String,
    onPlayed: () -> Unit,
    crashlyticsManager: CrashlyticsManager,
)

private const val HAPTIC_FEEDBACK_DELAY_MS = 30L

@Suppress("TooGenericExceptionCaught")
@Composable
private fun HapticFeeback(
    hapticFeedbackType: HapticFeedbackType,
    crashlyticsManager: CrashlyticsManager,
) {
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        Logger.d("SoundAndHaptics") { "Playing $hapticFeedbackType" }
        try {
            val hapticDuration = hapticFeedbackType.extendedHapticDuration()
            val startTime = TimeSource.Monotonic.markNow()
            while (startTime.elapsedNow() < hapticDuration) {
                haptic.performHapticFeedback(hapticFeedbackType)
                delay(HAPTIC_FEEDBACK_DELAY_MS)
            }
        } catch (e: Exception) {
            crashlyticsManager.recordException(YralException("Error in dispatching haptics $e"))
        }
    }
}

@Suppress("MagicNumber")
private fun HapticFeedbackType.extendedHapticDuration(): Duration =
    when (this) {
        HapticFeedbackType.LongPress -> 500.milliseconds
        else -> HAPTIC_FEEDBACK_DELAY_MS.milliseconds // only 1 loop
    }

fun popPressedSoundUri(): String = Res.getUri("files/audio/pop_pressed.mp3")
