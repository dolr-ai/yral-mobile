package com.yral.android.installReferrer

import android.app.Application
import android.net.Uri
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.yral.shared.preferences.UTM_CAMPAIGN_PARAM
import com.yral.shared.preferences.UTM_CONTENT_PARAM
import com.yral.shared.preferences.UTM_MEDIUM_PARAM
import com.yral.shared.preferences.UTM_SOURCE_PARAM
import com.yral.shared.preferences.UTM_TERM_PARAM
import com.yral.shared.preferences.UtmParams
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class InstallReferrerAttribution(
    private val application: Application,
) {
    private val logger = AttributionManager.createLogger("InstallReferrer")

    suspend fun fetchReferrer(): String? =
        suspendCancellableCoroutine { continuation ->
            val resumed = AtomicBoolean(false)
            var tempClient: InstallReferrerClient? = null

            fun endConnectionSafely() {
                tempClient?.let { client ->
                    runCatching { client.endConnection() }
                    tempClient = null
                }
            }

            fun resumeOnce(value: String?) {
                if (resumed.compareAndSet(false, true)) {
                    continuation.resume(value)
                    endConnectionSafely()
                }
            }

            continuation.invokeOnCancellation {
                if (resumed.compareAndSet(false, true)) {
                    endConnectionSafely()
                }
            }

            runCatching {
                tempClient =
                    InstallReferrerClient
                        .newBuilder(application)
                        .build()

                tempClient!!.startConnection(
                    object : InstallReferrerStateListener {
                        override fun onInstallReferrerSetupFinished(responseCode: Int) {
                            runCatching {
                                when (responseCode) {
                                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                                        val referrer = tempClient?.installReferrer?.installReferrer
                                        logger.i { "Play InstallReferrer received: $referrer" }
                                        resumeOnce(referrer)
                                    }
                                    else -> {
                                        logger.i { "Play InstallReferrer not available, code=$responseCode" }
                                        resumeOnce(null)
                                    }
                                }
                            }.onFailure { exception ->
                                logger.e(exception) { "Error while fetching install referrer" }
                                resumeOnce(null)
                            }
                        }

                        override fun onInstallReferrerServiceDisconnected() {
                            logger.i { "Play InstallReferrer service disconnected" }
                            resumeOnce(null)
                        }
                    },
                )
            }.onFailure { exception ->
                logger.e(exception) { "Failed to fetch install referrer" }
                resumeOnce(null)
            }
        }

    fun extractUtmParams(rawReferrer: String): UtmParams {
        val lower = rawReferrer.lowercase()

        val uri =
            if (rawReferrer.contains("://")) {
                Uri.parse(rawReferrer)
            } else {
                Uri.parse("https://dummy/?$rawReferrer")
            }

        var utmSource: String? = uri.getQueryParameter(UTM_SOURCE_PARAM)
        val utmMedium: String? = uri.getQueryParameter(UTM_MEDIUM_PARAM)
        val utmCampaign: String? = uri.getQueryParameter(UTM_CAMPAIGN_PARAM)
        val utmTerm: String? = uri.getQueryParameter(UTM_TERM_PARAM)
        val utmContent: String? = uri.getQueryParameter(UTM_CONTENT_PARAM)

        if (utmSource.isNullOrBlank()) {
            utmSource =
                when {
                    "gclid=" in lower -> "google_ads"
                    "fbclid=" in lower -> "meta_ads"
                    else -> null
                }
        }

        return UtmParams(
            source = utmSource,
            medium = utmMedium,
            campaign = utmCampaign,
            term = utmTerm,
            content = utmContent,
        )
    }
}
