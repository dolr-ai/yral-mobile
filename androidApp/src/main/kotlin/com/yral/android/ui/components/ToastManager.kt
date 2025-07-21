package com.yral.android.ui.components

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Data class representing a toast message with its properties
 */
data class ToastMessage
    @OptIn(ExperimentalUuidApi::class)
    constructor(
        val id: String = Uuid.random().toString(),
        val type: ToastType,
        val status: ToastStatus = ToastStatus.Info,
        val cta: ToastCTA? = null,
        val duration: Long = 5000L, // Duration in milliseconds
    )

/**
 * ToastManager to manage toast messages throughout the app
 */
object ToastManager {
    private val _currentToast = MutableStateFlow<ToastMessage?>(null)
    val currentToast: StateFlow<ToastMessage?> = _currentToast.asStateFlow()

    fun showSuccess(
        type: ToastType,
        cta: ToastCTA? = null,
        duration: Long = 5000L,
    ) {
        showToast(type, ToastStatus.Success, cta, duration)
    }

    fun showWarning(
        type: ToastType,
        cta: ToastCTA? = null,
        duration: Long = 5000L,
    ) {
        showToast(type, ToastStatus.Warning, cta, duration)
    }

    fun showError(
        type: ToastType,
        cta: ToastCTA? = null,
        duration: Long = 5000L,
    ) {
        showToast(type, ToastStatus.Error, cta, duration)
    }

    fun showInfo(
        type: ToastType,
        cta: ToastCTA? = null,
        duration: Long = 5000L,
    ) {
        showToast(type, ToastStatus.Info, cta, duration)
    }

    fun showToast(
        type: ToastType,
        status: ToastStatus = ToastStatus.Info,
        cta: ToastCTA? = null,
        duration: Long = 5000L,
    ) {
        val toast =
            ToastMessage(
                type = type,
                status = status,
                cta = cta,
                duration = duration,
            )
        _currentToast.value = toast
    }

    fun dismiss() {
        _currentToast.value = null
    }

    fun clear() {
        dismiss()
    }
}

/**
 * Composable that handles displaying toasts from ToastManager
 * This should be placed at the top level of your screen hierarchy
 */
@Composable
fun ToastHost(modifier: Modifier = Modifier) {
    val currentToast by ToastManager.currentToast.collectAsState()
    val visibleState = remember { MutableTransitionState(false) }

    // Handle toast appearance/disappearance
    LaunchedEffect(currentToast) {
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
                // Clear the toast after animation completes
                if (!visibleState.targetState && !visibleState.currentState) {
                    ToastManager.dismiss()
                }
            },
            modifier = modifier,
        )

        // Auto-dismiss after duration
        LaunchedEffect(toast.id) {
            kotlinx.coroutines.delay(toast.duration)
            visibleState.targetState = false
        }

        // Clear toast from state after exit animation completes
        LaunchedEffect(visibleState.targetState, visibleState.isIdle) {
            if (!visibleState.targetState && visibleState.isIdle && !visibleState.currentState) {
                ToastManager.dismiss()
            }
        }
    }
}
