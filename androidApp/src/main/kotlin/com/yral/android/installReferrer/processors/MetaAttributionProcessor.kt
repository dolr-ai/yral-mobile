package com.yral.android.installReferrer.processors

import android.app.Application
import com.yral.android.installReferrer.AttributionManager
import com.yral.android.installReferrer.AttributionProcessor
import com.yral.android.installReferrer.MetaInstallReferrerAttribution
import com.yral.android.installReferrer.MetaInstallReferrerFetcher
import com.yral.shared.preferences.stores.UtmParams
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MetaAttributionProcessor(
    private val application: Application,
    private val scope: CoroutineScope,
) : AttributionProcessor {
    override val priority: Int = 1
    override val name: String = "MetaInstallReferrer"

    private val metaAttribution by lazy { MetaInstallReferrerAttribution() }
    private val logger = AttributionManager.createLogger("MetaAttributionProcessor")

    override fun process(callback: (UtmParams?) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val fetcher = MetaInstallReferrerFetcher(application)
                val referrerData = fetcher.fetch()

                if (referrerData != null) {
                    val trimmed = referrerData.trim()
                    if (metaAttribution.isMetaInstallReferrerData(trimmed)) {
                        logger.i { "Detected encrypted Meta Install Referrer data" }
                        val utmParams = metaAttribution.extractUtmParams(trimmed)
                        callback(utmParams)
                    } else {
                        logger.d { "No encrypted Meta data found. Referrer: $trimmed" }
                        callback(null)
                    }
                } else {
                    logger.d { "No Meta Install Referrer data found" }
                    callback(null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught")
                e: Exception,
            ) {
                logger.e(e) { "Failed to process Meta Install Referrer" }
                callback(null)
            }
        }
    }
}
