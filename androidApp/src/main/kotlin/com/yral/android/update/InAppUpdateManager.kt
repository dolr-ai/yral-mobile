package com.yral.android.update

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import co.touchlab.kermit.Logger
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.russhwolf.settings.Settings
import com.yral.shared.app.UpdateState
import com.yral.shared.app.UpdateType
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.preferences.PrefKeys

/**
 * Manages in-app updates using Google Play Core library with lifecycle awareness.
 *
 * This class implements DefaultLifecycleObserver to automatically:
 * - Check for updates on resume
 * - Handle interrupted updates on resume
 * - Clean up listeners on stop to prevent memory leaks
 *
 * Usage:
 * 1. Create instance in onCreate()
 * 2. Set ActivityResultLauncher for immediate updates
 * 3. Register with lifecycle: lifecycle.addObserver(inAppUpdateManager)
 *
 * Note: No need to manually remove the observer - it's automatically cleaned up
 * when the Activity is destroyed since they share the same lifecycle scope.
 */
class InAppUpdateManager(
    private val context: Context,
    private val settings: Settings,
    private val crashlyticsManager: CrashlyticsManager,
    private val onStateChanged: (UpdateState) -> Unit,
) : DefaultLifecycleObserver {
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
    private var updateResultLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    companion object {
        private const val TAG = "InAppUpdateManager"

//        private const val UPDATE_CHECK_INTERVAL_MS = 3 * 60 * 60 * 1000L // 3 hours
        private const val UPDATE_CHECK_INTERVAL_MS = 1 * 60 * 1000L // 1 minute
        private const val REQUEST_CODE_FLEXIBLE_UPDATE = 1001
        private const val PRIORITY_IMMEDIATE = 4
        private const val STALENESS_DAYS_IMMEDIATE = 7
        private const val PRIORITY_FLEXIBLE = 2
        private const val STALENESS_DAYS_FLEXIBLE = 1
    }

    private val installStateUpdatedListener =
        InstallStateUpdatedListener { installState ->
            handleInstallStateUpdate(installState)
        }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Logger.d(TAG) { "onResume - checking for existing and new updates" }
        checkForUpdate()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    /**
     * Sets the ActivityResultLauncher for handling immediate update results
     */
    fun setUpdateResultLauncher(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        updateResultLauncher = launcher
    }

    fun checkForUpdate() {
        Logger.d(TAG) { "Checking for app updates via API call" }
        onStateChanged(UpdateState.Checking)

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                handleUpdateInfo(appUpdateInfo)
            }.addOnFailureListener { exception ->
                Logger.e(TAG, exception) { "Failed to check for updates" }
                crashlyticsManager.recordException(exception)
                onStateChanged(UpdateState.Failed(exception.message ?: "Unknown error"))
            }
    }

    /**
     * Completes a flexible update by restarting the app
     */
    fun completeUpdate() {
        Logger.d(TAG) { "Completing flexible update" }
        appUpdateManager.completeUpdate()
    }

    /**
     * Handles the result of an immediate update
     */
    fun handleImmediateUpdateResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                Logger.d(TAG) { "Immediate update completed successfully" }
                // Update the timestamp now that the critical update is complete
                markUpdateChecked()
                onStateChanged(UpdateState.Idle)
            }
            Activity.RESULT_CANCELED -> {
                Logger.w(TAG) { "Immediate update was canceled by user. App exit is required." }
                onStateChanged(UpdateState.ImmediateUpdateCancelled)
            }
            else -> {
                Logger.e(TAG) { "Immediate update failed with result code: $resultCode" }
                onStateChanged(UpdateState.Failed("Update failed"))
            }
        }
    }

    private fun handleUpdateInfo(appUpdateInfo: AppUpdateInfo) {
        val updateAvailability = appUpdateInfo.updateAvailability()
        Logger.d(TAG) { "Update availability: $updateAvailability" }

        if (
            updateAvailability == UpdateAvailability.UNKNOWN ||
            updateAvailability == UpdateAvailability.UPDATE_NOT_AVAILABLE
        ) {
            Logger.d(TAG) { "No update available" }
            onStateChanged(UpdateState.Idle)
            return
        }

        val updateType = determineUpdateType(appUpdateInfo)

        when (updateType) {
            UpdateType.IMMEDIATE -> {
                Logger.d(TAG) { "Triggering immediate update - NOT updating check timestamp" }
                // DO NOT update the timestamp for immediate updates!
                // This ensures the app will check again if the user bypasses the update
                onStateChanged(UpdateState.ImmediateRequired)
                startImmediateUpdate(appUpdateInfo)
            }
            UpdateType.FLEXIBLE -> {
                Logger.d(TAG) { "Flexible update available" }
                onStateChanged(UpdateState.FlexibleAvailable)
                startFlexibleUpdate(appUpdateInfo)
            }
            null -> {
                Logger.d(TAG) { "Update available but not triggered based on policy" }
                // Update timestamp since no action is needed
                markUpdateChecked()
                onStateChanged(UpdateState.Idle)
            }
        }
    }

    private fun determineUpdateType(appUpdateInfo: AppUpdateInfo): UpdateType? {
        val priority = appUpdateInfo.updatePriority()
        val stalenessDays = appUpdateInfo.clientVersionStalenessDays() ?: 0

        Logger.d(TAG) { "Update available - Priority: $priority, Staleness: $stalenessDays days" }

        return when {
            // Priority 4 or 5 = Immediate update
            priority >= PRIORITY_IMMEDIATE -> UpdateType.IMMEDIATE
            // 30+ days old = Immediate update
            stalenessDays >= STALENESS_DAYS_IMMEDIATE -> UpdateType.IMMEDIATE
            // Priority 2 or 3 = Flexible update
            priority >= PRIORITY_FLEXIBLE -> UpdateType.FLEXIBLE
            // 7+ days old = Flexible update
            stalenessDays >= STALENESS_DAYS_FLEXIBLE -> UpdateType.FLEXIBLE
            // Otherwise, no update
            else -> null
        }
    }

    fun startImmediateUpdate(updateInfo: AppUpdateInfo) {
        val launcher = updateResultLauncher ?: return

        if (!updateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            Logger.w(TAG) { "Immediate update not allowed" }
            onStateChanged(UpdateState.Failed("Immediate update not allowed"))
            return
        }

        Logger.d(TAG) { "Starting immediate update" }

        try {
            val updateOptions = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()

            appUpdateManager.startUpdateFlowForResult(
                updateInfo,
                launcher,
                updateOptions,
            )
            onStateChanged(UpdateState.ImmediateStarted)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Logger.e(TAG, e) { "Failed to start immediate update" }
            crashlyticsManager.recordException(e)
            onStateChanged(UpdateState.Failed(e.message ?: "Failed to start update"))
        }
    }

    @Suppress("ReturnCount")
    private fun startFlexibleUpdate(updateInfo: AppUpdateInfo) {
        if (shouldSkipFlexibleUpdateCheck()) {
            Logger.d(TAG) { "Skipping update check - interval has not passed" }
            onStateChanged(UpdateState.Idle)
            return
        }

        if (updateInfo.installStatus() == InstallStatus.DOWNLOADED) {
            Logger.d(TAG) { "Flexible update already downloaded, ready to install" }
            onStateChanged(UpdateState.FlexibleDownloaded)
            markUpdateChecked()
            return
        }

        if (!updateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            Logger.w(TAG) { "Flexible update not allowed" }
            onStateChanged(UpdateState.Failed("Flexible update not allowed"))
            return
        }

        // Update timestamp for flexible updates to respect the interval
        markUpdateChecked()

        Logger.d(TAG) { "Starting flexible update" }

        // Register listener for flexible update progress
        appUpdateManager.registerListener(installStateUpdatedListener)

        appUpdateManager.startUpdateFlowForResult(
            updateInfo,
            context as Activity,
            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
            REQUEST_CODE_FLEXIBLE_UPDATE,
        )
    }

    private fun markUpdateChecked() {
        settings.putLong(PrefKeys.IAP_LAST_UPDATE_CHECK_TIME.name, System.currentTimeMillis())
    }

    /**
     * Checks if an update check should be skipped based on the time interval
     * @return true if the check should be skipped, false otherwise
     */
    private fun shouldSkipFlexibleUpdateCheck(): Boolean {
        val lastCheckTime = settings.getLong(PrefKeys.IAP_LAST_UPDATE_CHECK_TIME.name, 0)
        return System.currentTimeMillis() - lastCheckTime < UPDATE_CHECK_INTERVAL_MS
    }

    private fun handleInstallStateUpdate(installState: InstallState) {
        when (installState.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = installState.bytesDownloaded()
                val totalBytesToDownload = installState.totalBytesToDownload()
                val progress =
                    if (totalBytesToDownload > 0) {
                        @Suppress("MagicNumber")
                        ((bytesDownloaded * 100) / totalBytesToDownload).toInt()
                    } else {
                        0
                    }

                Logger.d(TAG) { "Flexible update downloading: $progress%" }
                onStateChanged(UpdateState.FlexibleDownloading(progress))
            }
            InstallStatus.DOWNLOADED -> {
                Logger.d(TAG) { "Flexible update downloaded, ready to install" }
                onStateChanged(UpdateState.FlexibleDownloaded)
                // Unregister listener as we no longer need download updates
                appUpdateManager.unregisterListener(installStateUpdatedListener)
            }
            InstallStatus.FAILED -> {
                Logger.e(TAG) { "Flexible update download failed" }
                onStateChanged(UpdateState.Failed("Download failed"))
                appUpdateManager.unregisterListener(installStateUpdatedListener)
            }
            InstallStatus.CANCELED -> {
                Logger.w(TAG) { "Flexible update download canceled" }
                onStateChanged(UpdateState.Idle)
                appUpdateManager.unregisterListener(installStateUpdatedListener)
            }
            else -> {
                Logger.d(TAG) { "Install status: ${installState.installStatus()}" }
            }
        }
    }
}
