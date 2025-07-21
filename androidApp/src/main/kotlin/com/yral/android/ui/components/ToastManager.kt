package com.yral.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
        val visible: Boolean = true,
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

    currentToast?.let { toast ->
        Toast(
            visible = toast.visible,
            type = toast.type,
            status = toast.status,
            cta = toast.cta,
            onDismiss = { ToastManager.dismiss() },
            modifier = modifier,
        )

        // Auto-dismiss after duration
        LaunchedEffect(toast.id) {
            kotlinx.coroutines.delay(toast.duration)
            ToastManager.dismiss()
        }
    }
}
