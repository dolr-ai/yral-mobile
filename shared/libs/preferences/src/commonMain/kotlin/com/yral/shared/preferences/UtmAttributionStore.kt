package com.yral.shared.preferences

const val UTM_SOURCE_PARAM: String = "utm_source"
const val UTM_MEDIUM_PARAM: String = "utm_medium"
const val UTM_CAMPAIGN_PARAM: String = "utm_campaign"
const val UTM_TERM_PARAM: String = "utm_term"
const val UTM_CONTENT_PARAM: String = "utm_content"

private const val INSTALL_REFERRER_COMPLETED_KEY = "install_referrer_completed"
private const val UTM_SHARED_PREF_NAME = "YRAL_UTM_PREF"

data class UtmParams(
    val source: String? = null,
    val medium: String? = null,
    val campaign: String? = null,
    val term: String? = null,
    val content: String? = null,
)

class UtmAttributionStore(
    preferencesFactory: PreferencesFactory,
) {
    private val settings = preferencesFactory.create(UTM_SHARED_PREF_NAME)

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
        saveIfEmpty(UTM_SOURCE_PARAM, source)
        saveIfEmpty(UTM_MEDIUM_PARAM, medium)
        saveIfEmpty(UTM_CAMPAIGN_PARAM, campaign)
        saveIfEmpty(UTM_TERM_PARAM, term)
        saveIfEmpty(UTM_CONTENT_PARAM, content)
        markInstallReferrerCompleted()
    }

    private fun saveIfEmpty(
        key: String,
        value: String?,
    ) {
        value?.takeIf { it.isNotBlank() }?.let {
            if (settings.getStringOrNull(key).isNullOrBlank()) {
                settings.putString(key, it)
            }
        }
    }

    fun get(): UtmParams =
        UtmParams(
            source = settings.getStringOrNull(UTM_SOURCE_PARAM)?.takeIf { it.isNotBlank() },
            medium = settings.getStringOrNull(UTM_MEDIUM_PARAM)?.takeIf { it.isNotBlank() },
            campaign = settings.getStringOrNull(UTM_CAMPAIGN_PARAM)?.takeIf { it.isNotBlank() },
            term = settings.getStringOrNull(UTM_TERM_PARAM)?.takeIf { it.isNotBlank() },
            content = settings.getStringOrNull(UTM_CONTENT_PARAM)?.takeIf { it.isNotBlank() },
        )

    fun isInstallReferrerCompleted(): Boolean =
        settings
            .getBoolean(INSTALL_REFERRER_COMPLETED_KEY, false)

    private fun markInstallReferrerCompleted() {
        settings.putBoolean(INSTALL_REFERRER_COMPLETED_KEY, true)
    }

    fun clear() {
        settings.clear()
    }
}
