package com.yral.android.installReferrer

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.yral.shared.preferences.stores.UTM_CAMPAIGN_PARAM
import com.yral.shared.preferences.stores.UTM_CONTENT_PARAM
import com.yral.shared.preferences.stores.UTM_MEDIUM_PARAM
import com.yral.shared.preferences.stores.UTM_SOURCE_PARAM
import com.yral.shared.preferences.stores.UTM_TERM_PARAM
import com.yral.shared.preferences.stores.UtmParams
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

            fun resumeOnce(value: String?) {
                if (resumed.compareAndSet(false, true)) {
                    continuation.resume(value)
                }
            }

            runCatching {
                val client = InstallReferrerClient.newBuilder(application).build()

                fun endConnectionSafely() {
                    runCatching { client.endConnection() }.onFailure {
                        logger.e { "Error in endConnection to referrer client: $it" }
                    }
                }

                continuation.invokeOnCancellation {
                    endConnectionSafely()
                }

                client.startConnection(
                    object : InstallReferrerStateListener {
                        override fun onInstallReferrerSetupFinished(responseCode: Int) {
                            runCatching {
                                when (responseCode) {
                                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                                        val referrer = client.installReferrer.installReferrer
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
                            }.also {
                                endConnectionSafely()
                            }
                        }

                        override fun onInstallReferrerServiceDisconnected() {
                            logger.i { "Play InstallReferrer service disconnected" }
                            resumeOnce(null)
                            endConnectionSafely()
                        }
                    },
                )
            }.onFailure { exception ->
                logger.e(exception) { "Failed to fetch install referrer" }
                resumeOnce(null)
            }
        }

    fun extractUtmParams(rawReferrer: String): UtmParams {
        val uri = rawReferrer.toSafeUri()
        var utmSource: String? = uri.getQueryParameter(UTM_SOURCE_PARAM)
        val utmMedium: String? = uri.getQueryParameter(UTM_MEDIUM_PARAM)
        val utmCampaign: String? = uri.getQueryParameter(UTM_CAMPAIGN_PARAM)
        val utmTerm: String? = uri.getQueryParameter(UTM_TERM_PARAM)
        val utmContent: String? = uri.getQueryParameter(UTM_CONTENT_PARAM)

        if (utmSource.isNullOrBlank() || utmSource.isNullOrNotSet()) {
            utmSource =
                when {
                    "gclid=" in rawReferrer.lowercase() -> "google_ads"
                    "fbclid=" in rawReferrer.lowercase() -> "meta_ads"
                    else -> utmSource
                }
        }

        return UtmParams(
            raw = rawReferrer,
            source = utmSource,
            medium = utmMedium,
            campaign = utmCampaign,
            term = utmTerm,
            content = utmContent,
        )
    }

    private fun String.toSafeUri(): Uri =
        if (this.contains("://")) {
            this.toUri()
        } else {
            "https://dummy/?$this".toUri()
        }

    private fun String?.isNullOrNotSet(): Boolean {
        val v = this ?: return true
        val normalized = v.replace("%20", " ").lowercase()
        return normalized.isBlank() || "not set" in normalized
    }
}
