package com.yral.shared.preferences

import com.russhwolf.settings.Settings

// Public keys to be reused across modules (Branch params, analytics properties, etc.)
const val UTM_SOURCE_PARAM: String = "utm_source"
const val UTM_MEDIUM_PARAM: String = "utm_medium"
const val UTM_CAMPAIGN_PARAM: String = "utm_campaign"
const val UTM_TERM_PARAM: String = "utm_term"
const val UTM_CONTENT_PARAM: String = "utm_content"

// Internal preference keys – kept in sync with the public param names above.
private const val UTM_SOURCE_KEY = UTM_SOURCE_PARAM
private const val UTM_MEDIUM_KEY = UTM_MEDIUM_PARAM
private const val UTM_CAMPAIGN_KEY = UTM_CAMPAIGN_PARAM
private const val UTM_TERM_KEY = UTM_TERM_PARAM
private const val UTM_CONTENT_KEY = UTM_CONTENT_PARAM
private const val INSTALL_REFERRER_COMPLETED_KEY = "install_referrer_completed"
private const val INSTALL_REFERRER_TRACKED_KEY = "install_referrer_tracked"

data class UtmParams(
    val source: String? = null,
    val medium: String? = null,
    val campaign: String? = null,
    val term: String? = null,
    val content: String? = null,
)

class UtmAttributionStore(
    private val settings: Settings,
) {
    /**
     * Store UTM parameters only if they haven't been set before.
     * This preserves first-touch attribution semantics.
     */
    fun storeIfEmpty(
        source: String? = null,
        medium: String? = null,
        campaign: String? = null,
        term: String? = null,
        content: String? = null,
    ) {
        source
            ?.takeIf { it.isNotBlank() }
            ?.let { value ->
                if (settings.getStringOrNull(UTM_SOURCE_KEY).isNullOrBlank()) {
                    settings.putString(UTM_SOURCE_KEY, value)
                }
            }

        medium
            ?.takeIf { it.isNotBlank() }
            ?.let { value ->
                if (settings.getStringOrNull(UTM_MEDIUM_KEY).isNullOrBlank()) {
                    settings.putString(UTM_MEDIUM_KEY, value)
                }
            }

        campaign
            ?.takeIf { it.isNotBlank() }
            ?.let { value ->
                if (settings.getStringOrNull(UTM_CAMPAIGN_KEY).isNullOrBlank()) {
                    settings.putString(UTM_CAMPAIGN_KEY, value)
                }
            }

        term
            ?.takeIf { it.isNotBlank() }
            ?.let { value ->
                if (settings.getStringOrNull(UTM_TERM_KEY).isNullOrBlank()) {
                    settings.putString(UTM_TERM_KEY, value)
                }
            }

        content
            ?.takeIf { it.isNotBlank() }
            ?.let { value ->
                if (settings.getStringOrNull(UTM_CONTENT_KEY).isNullOrBlank()) {
                    settings.putString(UTM_CONTENT_KEY, value)
                }
            }
    }

    fun get(): UtmParams =
        UtmParams(
            source = settings.getStringOrNull(UTM_SOURCE_KEY)?.takeIf { it.isNotBlank() },
            medium = settings.getStringOrNull(UTM_MEDIUM_KEY)?.takeIf { it.isNotBlank() },
            campaign = settings.getStringOrNull(UTM_CAMPAIGN_KEY)?.takeIf { it.isNotBlank() },
            term = settings.getStringOrNull(UTM_TERM_KEY)?.takeIf { it.isNotBlank() },
            content = settings.getStringOrNull(UTM_CONTENT_KEY)?.takeIf { it.isNotBlank() },
        )

    fun isInstallReferrerCompleted(): Boolean =
        settings
            .getBoolean(INSTALL_REFERRER_COMPLETED_KEY, false)

    fun markInstallReferrerCompleted() {
        settings.putBoolean(INSTALL_REFERRER_COMPLETED_KEY, true)
    }

    fun isInstallReferrerTracked(): Boolean =
        settings
            .getBoolean(INSTALL_REFERRER_TRACKED_KEY, false)

    fun markInstallReferrerTracked() {
        settings.putBoolean(INSTALL_REFERRER_TRACKED_KEY, true)
    }

    fun clear() {
        settings.remove(UTM_SOURCE_KEY)
        settings.remove(UTM_MEDIUM_KEY)
        settings.remove(UTM_CAMPAIGN_KEY)
        settings.remove(UTM_TERM_KEY)
        settings.remove(UTM_CONTENT_KEY)
        settings.remove(INSTALL_REFERRER_COMPLETED_KEY)
    }
}
