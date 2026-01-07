package com.yral.shared.analytics.adTracking

import co.touchlab.kermit.Logger
import com.yral.shared.core.exceptions.YralException
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.AdSupport.ASIdentifierManager
import platform.AppTrackingTransparency.ATTrackingManager
import kotlin.coroutines.resume

/**
 * iOS implementation of AdvertisingIdProvider.
 * Automatically requests App Tracking Transparency permission if needed (iOS 14+).
 *
 * Requires NSUserTrackingUsageDescription in Info.plist.
 */
@Suppress("TooGenericExceptionCaught")
@OptIn(ExperimentalForeignApi::class)
class IosAdvertisingIdProvider : AdvertisingIdProvider {
    private val logger = Logger.withTag("IosAdvertisingIdProvider")
    private var permissionRequested = false
    private val restrictedTrackingId = "00000000-0000-0000-0000-000000000000"

    override suspend fun getAdvertisingId(): String? {
        // Request permission on background thread first (iOS version check, status check)
        if (!permissionRequested) {
            requestTrackingPermissionIfNeeded()
            permissionRequested = true
        }

        // Only switch to main thread for the actual API calls that require it
        return withContext(Dispatchers.Main) {
            val manager = ASIdentifierManager.sharedManager()
            if (!manager.advertisingTrackingEnabled) {
                return@withContext null
            }

            val uuidString = manager.advertisingIdentifier.UUIDString
            if (uuidString == restrictedTrackingId) {
                return@withContext null
            }

            uuidString
        }
    }

    private suspend fun requestTrackingPermissionIfNeeded() {
        val majorVersion =
            platform.UIKit.UIDevice.currentDevice.systemVersion
                .split(".")
                .firstOrNull()
                ?.toIntOrNull() ?: 0

        if (majorVersion < MIN_OS_FOR_AD_TRACKING_PERMISSION) return

        try {
            // Check status on main thread (required by iOS)
            val status =
                withContext(Dispatchers.Main) {
                    ATTrackingManager.trackingAuthorizationStatus
                }

            // Only request if status is notDetermined
            if (status == ATTrackingAuthorizationStatus.NOT_DETERMINED) {
                requestTrackingAuthorization()
            }
        } catch (e: Exception) {
            logger.w(e) { "ATT framework not available" }
        }
    }

    private suspend fun requestTrackingAuthorization() =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                try {
                    ATTrackingManager.requestTrackingAuthorizationWithCompletionHandler { status ->
                        continuation.resume(status)
                    }
                } catch (e: Exception) {
                    logger.w(e) { "Error requesting tracking authorization proceeding with current status" }
                    try {
                        continuation.resume(ATTrackingManager.trackingAuthorizationStatus)
                    } catch (fallbackError: Exception) {
                        continuation.cancel(
                            cause =
                                YralException(
                                    message = "Failed to request tracking authorization",
                                    cause = fallbackError,
                                ),
                        )
                    }
                }
            }
        }
}

private object ATTrackingAuthorizationStatus {
    const val NOT_DETERMINED: ULong = 0UL
    const val RESTRICTED: ULong = 1UL
    const val DENIED: ULong = 2UL
    const val AUTHORIZED: ULong = 3UL
}

private const val MIN_OS_FOR_AD_TRACKING_PERMISSION = 14
