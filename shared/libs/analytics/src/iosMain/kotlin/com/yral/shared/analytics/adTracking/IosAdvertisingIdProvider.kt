package com.yral.shared.analytics.adTracking

import co.touchlab.kermit.Logger
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.AdSupport.ASIdentifierManager
import platform.AppTrackingTransparency.ATTrackingManager
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

/**
 * iOS implementation of AdvertisingIdProvider.
 * Automatically requests App Tracking Transparency permission if needed (iOS 14+).
 *
 * Requires NSUserTrackingUsageDescription in Info.plist.
 */
@Suppress("TooGenericExceptionCaught")
@OptIn(ExperimentalForeignApi::class)
class IosAdvertisingIdProvider(
    private val appDispatchers: AppDispatchers,
) : AdvertisingIdProvider {
    private val logger = Logger.withTag("AdTracking")
    private var permissionRequested = false
    private val restrictedTrackingId = "00000000-0000-0000-0000-000000000000"

    override suspend fun getAdvertisingId(): String? =
        withContext(appDispatchers.main) {
            delay(1.seconds)
            if (!permissionRequested) {
                logger.d { "Requesting tracking permission" }
                requestTrackingPermissionIfNeeded()
                permissionRequested = true
            }

            val status = ATTrackingManager.trackingAuthorizationStatus
            logger.d { "Tracking authorization status: $status" }

            if (status != ATTrackingAuthorizationStatus.AUTHORIZED) {
                logger.d { "Tracking not authorized (status: $status)" }
                return@withContext null
            }

            val uuidString =
                ASIdentifierManager.sharedManager().advertisingIdentifier.UUIDString
            if (uuidString == restrictedTrackingId) {
                logger.d { "Advertising ID is restricted" }
                return@withContext null
            }

            uuidString
        }

    private suspend fun requestTrackingPermissionIfNeeded() {
        try {
            val status = ATTrackingManager.trackingAuthorizationStatus
            logger.d { "Tracking authorization status: $status" }
            if (status == ATTrackingAuthorizationStatus.NOT_DETERMINED) {
                logger.d { "Status is NOT_DETERMINED, requesting tracking authorization dialog" }
                val newStatus = requestTrackingAuthorization()
                logger.d { "Tracking authorization dialog completed with status: $newStatus" }
            } else {
                logger.d { "Status is not NOT_DETERMINED ($status), skipping permission request" }
            }
        } catch (e: Exception) {
            logger.e(e) { "ATT framework not available or error checking status" }
        }
    }

    private suspend fun requestTrackingAuthorization(): ULong =
        suspendCancellableCoroutine { continuation ->
            try {
                logger.d { "Calling requestTrackingAuthorizationWithCompletionHandler" }
                ATTrackingManager.requestTrackingAuthorizationWithCompletionHandler { status ->
                    logger.d { "Tracking authorization completion handler called with status: $status" }
                    continuation.resume(status)
                }
            } catch (e: Exception) {
                logger.e(e) { "Error requesting tracking authorization: ${e.message}" }
                try {
                    val currentStatus = ATTrackingManager.trackingAuthorizationStatus
                    logger.w { "Resuming with current status: $currentStatus" }
                    continuation.resume(currentStatus)
                } catch (fallbackError: Exception) {
                    logger.e(fallbackError) { "Failed to get current status, cancelling" }
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

    override fun getAdvertisingIdKey(): String = "IDFA"
}

private object ATTrackingAuthorizationStatus {
    const val NOT_DETERMINED: ULong = 0UL
    const val RESTRICTED: ULong = 1UL
    const val DENIED: ULong = 2UL
    const val AUTHORIZED: ULong = 3UL
}
