package com.yral.android.installReferrer

import android.app.Application
import android.net.Uri
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.ReferralReceivedEventData
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.koin.koinInstance
import com.yral.shared.preferences.UTM_CAMPAIGN_PARAM
import com.yral.shared.preferences.UTM_CONTENT_PARAM
import com.yral.shared.preferences.UTM_MEDIUM_PARAM
import com.yral.shared.preferences.UTM_SOURCE_PARAM
import com.yral.shared.preferences.UTM_TERM_PARAM
import com.yral.shared.preferences.UtmAttributionStore
import com.yral.shared.preferences.UtmParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.qualifier.named

/**
 * Handles SDK-level install attribution using the Play Install Referrer API.
 * This runs once on app start and stores UTM parameters in [UtmAttributionStore].
 */
@Suppress("TooGenericExceptionCaught")
class InstallReferrerAttribution(
    private val application: Application,
    private val scope: CoroutineScope,
) {
    private var client: InstallReferrerClient? = null
    private val crashlyticsManager: CrashlyticsManager by lazy { koinInstance.get<CrashlyticsManager>() }
    private val utmAttributionStore: UtmAttributionStore by lazy { koinInstance.get<UtmAttributionStore>() }
    private val analyticsManager: AnalyticsManager by lazy { koinInstance.get<AnalyticsManager>() }
    private val metaAttribution by lazy { MetaInstallReferrerAttribution(scope, analyticsManager) }
    private val logger: Logger by lazy {
        val baseLogger = koinInstance.get<YralLogger>()
        val sentryLogWriter = koinInstance.get<LogWriter>(named("installReferrerLogWriter"))
        baseLogger.withAdditionalLogWriter(sentryLogWriter).withTag("InstallReferrer")
    }

    fun setup() {
        if (utmAttributionStore.isInstallReferrerCompleted()) {
            logger.i { "Install referrer attribution already completed, skipping." }
            return
        }
        scope.launch(Dispatchers.IO) {
            runCatching {
                client =
                    InstallReferrerClient
                        .newBuilder(application)
                        .build()
                client?.startConnection(installReferrerStateListener)
            }.onFailure { exception ->
                logger.e(exception) { "Failed to initialise InstallReferrerClient" }
                crashlyticsManager.recordException(
                    exception as? Exception ?: Exception(exception),
                    ExceptionType.INSTALL_REFERRER,
                )
                endConnectionSafely()
            }
        }
    }

    private val installReferrerStateListener =
        object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                try {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            runCatching {
                                val response = client?.installReferrer
                                val referrer = response?.installReferrer
                                logger.i { "Play InstallReferrer received: $referrer" }
                                if (utmAttributionStore.isInstallReferrerCompleted()) return
                                handleInstallReferrer(referrer)
                            }.onFailure { exception ->
                                logger.e(exception) { "Error while handling install referrer" }
                                crashlyticsManager.recordException(
                                    exception as? Exception ?: Exception(exception),
                                    ExceptionType.INSTALL_REFERRER,
                                )
                            }
                            endConnectionSafely()
                        }

                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED,
                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE,
                        InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR,
                        InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED,
                        -> {
                            logger.i { "Play InstallReferrer not available, code=$responseCode" }
                            endConnectionSafely()
                        }

                        else -> {
                            logger.i { "Unknown Play InstallReferrer response code=$responseCode" }
                            endConnectionSafely()
                        }
                    }
                } catch (exception: Exception) {
                    logger.e(exception) { "Unexpected error in InstallReferrerStateListener" }
                    crashlyticsManager.recordException(exception, ExceptionType.INSTALL_REFERRER)
                    endConnectionSafely()
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                logger.i { "Play InstallReferrer service disconnected" }
                endConnectionSafely()
            }
        }

    private fun endConnectionSafely() {
        runCatching { client?.endConnection() }
            .onFailure { exception ->
                logger.e(exception) { "Failed to end InstallReferrer connection" }
                crashlyticsManager.recordException(
                    exception as? Exception ?: Exception(exception),
                    ExceptionType.INSTALL_REFERRER,
                )
            }
        client = null
    }

    private fun handleInstallReferrer(referrer: String?) {
        try {
            val raw = referrer?.takeIf { it.isNotBlank() } ?: return

            when {
                metaAttribution.isMetaInstallReferrerData(raw) -> {
                    logger.i { "Detected encrypted Meta Install Referrer data" }
                    metaAttribution.processEncryptedData(raw)
                }
                else -> {
                    val utmParams = extractUtmParams(raw)
                    if (utmParams.isEmpty()) {
                        logger.d { "No UTM parameters found in install referrer" }
                        return
                    }
                    runCatching {
                        utmAttributionStore.storeIfEmpty(
                            source = utmParams.source,
                            medium = utmParams.medium,
                            campaign = utmParams.campaign,
                            term = utmParams.term,
                            content = utmParams.content,
                        )
                        analyticsManager.trackEvent(
                            ReferralReceivedEventData(
                                source = utmParams.source,
                                medium = utmParams.medium,
                                campaign = utmParams.campaign,
                                term = utmParams.term,
                                content = utmParams.content,
                            ),
                        )
                        logger.i { "Successfully stored UTM params: $utmParams" }
                    }.onFailure { exception ->
                        logger.e(exception) { "Failed to store UTM params from Play InstallReferrer" }
                        crashlyticsManager.recordException(
                            exception as? Exception ?: Exception(exception),
                            ExceptionType.INSTALL_REFERRER,
                        )
                    }
                }
            }
        } catch (exception: Exception) {
            logger.e(exception) { "Unexpected error while parsing InstallReferrer" }
            crashlyticsManager.recordException(exception, ExceptionType.INSTALL_REFERRER)
        }
    }

    private fun extractUtmParams(rawReferrer: String): UtmParams {
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

    private fun UtmParams.isEmpty(): Boolean =
        source.isNullOrBlank() &&
            medium.isNullOrBlank() &&
            campaign.isNullOrBlank() &&
            term.isNullOrBlank() &&
            content.isNullOrBlank()
}
