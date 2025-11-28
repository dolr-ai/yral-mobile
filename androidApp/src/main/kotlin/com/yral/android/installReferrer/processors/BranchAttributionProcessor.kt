package com.yral.android.installReferrer.processors

import com.yral.android.installReferrer.AttributionManager
import com.yral.android.installReferrer.AttributionProcessor
import com.yral.android.installReferrer.isEmpty
import com.yral.shared.preferences.stores.UTM_CAMPAIGN_PARAM
import com.yral.shared.preferences.stores.UTM_CONTENT_PARAM
import com.yral.shared.preferences.stores.UTM_MEDIUM_PARAM
import com.yral.shared.preferences.stores.UTM_SOURCE_PARAM
import com.yral.shared.preferences.stores.UTM_TERM_PARAM
import com.yral.shared.preferences.stores.UtmParams
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.CancellationException

class BranchAttributionProcessor : AttributionProcessor {
    @Suppress("MagicNumber")
    override val priority: Int = 3
    override val name: String = "Branch"

    private val logger = AttributionManager.createLogger("BranchAttributionProcessor")

    private val lock = Any()
    private var pendingLinkProperties: LinkProperties? = null
    private var pendingCallback: ((UtmParams?) -> Unit)? = null

    fun setLinkProperties(linkProperties: LinkProperties?) {
        synchronized(lock) { pendingLinkProperties = linkProperties }
        processPendingCallback()
    }

    override fun process(callback: (UtmParams?) -> Unit) {
        synchronized(lock) { pendingCallback = callback }
        processPendingCallback()
    }

    @Suppress("ReturnCount", "LongMethod", "NestedBlockDepth")
    private fun processPendingCallback() {
        val callback: ((UtmParams?) -> Unit)?
        val linkProperties: LinkProperties?
        synchronized(lock) {
            callback = pendingCallback
            linkProperties = pendingLinkProperties
            // Clear callback atomically to avoid race condition if process() is called again
            if (callback != null && linkProperties != null) {
                pendingCallback = null
            }
        }

        if (callback == null) {
            return
        }

        if (linkProperties == null) {
            logger.d { "Branch attribution not ready yet, waiting for session callback" }
            // Keep callback for when setLinkProperties is called later
            return
        }

        try {
            val controlParams = linkProperties.controlParams
            if (controlParams == null) {
                logger.d { "No control params in Branch link properties" }
                callback(null)
                return
            }

            logger.d { "Processing Branch attribution with controlParams: $controlParams" }

            val utmSource = controlParams[UTM_SOURCE_PARAM]
            val utmMedium = controlParams[UTM_MEDIUM_PARAM]
            val utmCampaign = controlParams[UTM_CAMPAIGN_PARAM]
            val utmTerm = controlParams[UTM_TERM_PARAM]
            val utmContent = controlParams[UTM_CONTENT_PARAM]

            val utmParams =
                UtmParams(
                    source = utmSource,
                    medium = utmMedium,
                    campaign = utmCampaign,
                    term = utmTerm,
                    content = utmContent,
                )

            val result =
                if (utmParams.isEmpty()) {
                    logger.d { "No UTM parameters found in Branch link properties" }
                    null
                } else {
                    logger.i {
                        "Branch attribution params: " +
                            "source=$utmSource, campaign=$utmCampaign, " +
                            "medium=$utmMedium, term=$utmTerm, content=$utmContent"
                    }
                    utmParams
                }

            callback(result)
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught")
            e: Exception,
        ) {
            logger.e(e) { "Failed to process Branch attribution" }
            callback(null)
        }
    }
}
