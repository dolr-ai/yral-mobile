package com.yral.android.update

/**
 * Represents the different states of an in-app update
 */
sealed interface UpdateState {
    /**
     * No update is available or no check has been performed
     */
    data object Idle : UpdateState

    /**
     * Checking for an update is in progress
     */
    data object Checking : UpdateState

    /**
     * A flexible update is available and can be started
     */
    data object FlexibleAvailable : UpdateState

    /**
     * A flexible update is downloading in the background
     */
    data class FlexibleDownloading(
        val progress: Int,
    ) : UpdateState

    /**
     * A flexible update has been downloaded and is ready to install
     */
    data object FlexibleDownloaded : UpdateState

    /**
     * An immediate update is required and will be started
     */
    data object ImmediateRequired : UpdateState

    data object ImmediateStarted : UpdateState

    /**
     * An immediate update was required but the user canceled it.
     */
    data object ImmediateUpdateCancelled : UpdateState

    /**
     * An update check or download failed
     */
    data class Failed(
        val error: String,
    ) : UpdateState
}

/**
 * Represents the type of update to be performed
 */
enum class UpdateType {
    FLEXIBLE,
    IMMEDIATE,
}
