package com.yral.android.ui.components

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * match Android Toast duration
 */
@Suppress("MagicNumber")
enum class ToastDuration(
    val durationMs: Long,
) {
    SHORT(2000L),
    LONG(3500L),
}

data class ToastMessage
    @OptIn(ExperimentalUuidApi::class)
    constructor(
        val id: String = Uuid.random().toString(),
        val type: ToastType,
        val status: ToastStatus = ToastStatus.Info,
        val cta: ToastCTA? = null,
        val duration: ToastDuration = ToastDuration.LONG,
    )

/**
 * ToastManager to manage toast messages throughout the app with queue support
 */
object ToastManager {
    private val _toastQueue = MutableStateFlow<List<ToastMessage>>(emptyList())
    val toastQueue: StateFlow<List<ToastMessage>> = _toastQueue.asStateFlow()

    fun showToast(
        type: ToastType,
        status: ToastStatus = ToastStatus.Info,
        cta: ToastCTA? = null,
        duration: ToastDuration = defaultDuration(type),
    ) {
        val toast =
            ToastMessage(
                type = type,
                status = status,
                cta = cta,
                duration = duration,
            )

        // Add toast to the end of the queue
        _toastQueue.value = _toastQueue.value + toast
    }

    internal fun defaultDuration(type: ToastType): ToastDuration =
        when (type) {
            is ToastType.Big -> ToastDuration.LONG
            is ToastType.Small -> ToastDuration.SHORT
        }

    fun dismissCurrent() {
        val currentQueue = _toastQueue.value
        if (currentQueue.isNotEmpty()) {
            _toastQueue.value = currentQueue.drop(1)
        }
    }

    fun dismissToast(toastId: String) {
        _toastQueue.value = _toastQueue.value.filter { it.id != toastId }
    }

    fun clear() {
        _toastQueue.value = emptyList()
    }

    fun getQueueSize(): Int = _toastQueue.value.size

    fun isEmpty(): Boolean = _toastQueue.value.isEmpty()
}

fun ToastManager.showSuccess(
    type: ToastType,
    cta: ToastCTA? = null,
    duration: ToastDuration = defaultDuration(type),
) {
    showToast(type, ToastStatus.Success, cta, duration)
}

fun ToastManager.showWarning(
    type: ToastType,
    cta: ToastCTA? = null,
    duration: ToastDuration = defaultDuration(type),
) {
    showToast(type, ToastStatus.Warning, cta, duration)
}

fun ToastManager.showError(
    type: ToastType,
    cta: ToastCTA? = null,
    duration: ToastDuration = defaultDuration(type),
) {
    showToast(type, ToastStatus.Error, cta, duration)
}

fun ToastManager.showInfo(
    type: ToastType,
    cta: ToastCTA? = null,
    duration: ToastDuration = defaultDuration(type),
) {
    showToast(type, ToastStatus.Info, cta, duration)
}

/**
 * Composable that handles displaying toasts from ToastManager with queue support
 * This should be placed at the top level of your screen hierarchy
 */
@Composable
fun ToastHost(modifier: Modifier = Modifier) {
    val toastQueue by ToastManager.toastQueue.collectAsState()
    val currentToast = toastQueue.firstOrNull()
    val visibleState = remember { MutableTransitionState(false) }

    // Handle toast appearance/disappearance
    LaunchedEffect(currentToast?.id) {
        // React to toast ID changes
        if (currentToast != null) {
            visibleState.targetState = true
        } else {
            visibleState.targetState = false
        }
    }

    // Show toast if there is one
    currentToast?.let { toast ->
        Toast(
            visibleState = visibleState,
            type = toast.type,
            status = toast.status,
            cta = toast.cta,
            onDismiss = {
                visibleState.targetState = false
            },
            modifier = modifier,
        )

        // Auto-dismiss after duration
        LaunchedEffect(toast.id) {
            delay(toast.duration.durationMs)
            // Check the actual current state of the queue, not the captured value
            if (ToastManager.toastQueue.value
                    .firstOrNull()
                    ?.id == toast.id
            ) {
                visibleState.targetState = false
            }
        }

        // Remove toast from queue after exit animation completes
        LaunchedEffect(visibleState.targetState, visibleState.isIdle) {
            if (!visibleState.targetState && visibleState.isIdle && !visibleState.currentState) {
                ToastManager.dismissCurrent()
            }
        }
    }
}
