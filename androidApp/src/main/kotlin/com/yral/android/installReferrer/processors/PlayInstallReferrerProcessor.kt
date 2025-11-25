package com.yral.android.installReferrer.processors

import android.app.Application
import com.yral.android.installReferrer.AttributionManager
import com.yral.android.installReferrer.AttributionProcessor
import com.yral.android.installReferrer.InstallReferrerAttribution
import com.yral.android.installReferrer.MetaInstallReferrerAttribution
import com.yral.android.installReferrer.isEmpty
import com.yral.shared.preferences.UtmParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlayInstallReferrerProcessor(
    private val application: Application,
    private val scope: CoroutineScope,
) : AttributionProcessor {
    override val priority: Int = 2
    override val name: String = "PlayInstallReferrer"

    private val metaAttribution by lazy { MetaInstallReferrerAttribution() }
    private val logger = AttributionManager.createLogger("PlayInstallReferrerProcessor")

    override fun process(callback: (UtmParams?) -> Unit) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val installReferrerAttribution = InstallReferrerAttribution(application, scope)
                val referrer = installReferrerAttribution.fetchReferrer()

                if (referrer != null && referrer.isNotBlank()) {
                    logger.i { "Play Install Referrer received: $referrer" }

                    if (metaAttribution.isMetaInstallReferrerData(referrer)) {
                        logger.i { "Detected encrypted Meta Install Referrer data in Play Install Referrer" }
                        val utmParams = metaAttribution.extractUtmParams(referrer)
                        callback(utmParams)
                    } else {
                        val utmParams = installReferrerAttribution.extractUtmParams(referrer)
                        if (utmParams.isEmpty()) {
                            logger.d { "No UTM parameters found in install referrer" }
                            callback(null)
                        } else {
                            callback(utmParams)
                        }
                    }
                } else {
                    logger.d { "No Play Install Referrer data found" }
                    callback(null)
                }
            }.onFailure {
                logger.e(it) { "Failed to process Play Install Referrer" }
                callback(null)
            }
        }
    }
}
