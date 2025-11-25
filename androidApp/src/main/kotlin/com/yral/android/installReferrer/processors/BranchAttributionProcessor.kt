package com.yral.android.installReferrer.processors

import com.yral.android.installReferrer.AttributionManager
import com.yral.android.installReferrer.AttributionProcessor
import com.yral.android.installReferrer.isEmpty
import com.yral.shared.preferences.UTM_CAMPAIGN_PARAM
import com.yral.shared.preferences.UTM_CONTENT_PARAM
import com.yral.shared.preferences.UTM_MEDIUM_PARAM
import com.yral.shared.preferences.UTM_SOURCE_PARAM
import com.yral.shared.preferences.UTM_TERM_PARAM
import com.yral.shared.preferences.UtmParams
import io.branch.referral.util.LinkProperties

class BranchAttributionProcessor : AttributionProcessor {
    @Suppress("MagicNumber")
    override val priority: Int = 3
    override val name: String = "Branch"

    private val logger = AttributionManager.createLogger("BranchAttributionProcessor")

    private var pendingLinkProperties: LinkProperties? = null
    private var pendingCallback: ((UtmParams?) -> Unit)? = null

    fun setLinkProperties(linkProperties: LinkProperties?) {
        pendingLinkProperties = linkProperties
        processPendingCallback()
    }

    override fun process(callback: (UtmParams?) -> Unit) {
        pendingCallback = callback
        processPendingCallback()
    }

    @Suppress("ReturnCount")
    private fun processPendingCallback() {
        val callback = pendingCallback ?: return
        val linkProperties = pendingLinkProperties

        if (linkProperties == null) {
            logger.d { "Branch attribution not ready yet, waiting for session callback" }
            return
        }

        pendingCallback = null
        runCatching {
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

            if (utmParams.isEmpty()) {
                logger.d { "No UTM parameters found in Branch link properties" }
                callback(null)
            } else {
                logger.i {
                    "Branch attribution params: " +
                        "source=$utmSource, campaign=$utmCampaign, " +
                        "medium=$utmMedium, term=$utmTerm, content=$utmContent"
                }
                callback(utmParams)
            }
        }.onFailure {
            logger.e(it) { "Failed to process Branch attribution" }
            callback(null)
        }
    }
}
