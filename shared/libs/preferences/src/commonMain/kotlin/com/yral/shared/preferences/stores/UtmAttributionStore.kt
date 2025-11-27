package com.yral.shared.preferences.stores

import com.yral.shared.preferences.Preferences

const val UTM_SOURCE_PARAM: String = "utm_source"
const val UTM_MEDIUM_PARAM: String = "utm_medium"
const val UTM_CAMPAIGN_PARAM: String = "utm_campaign"
const val UTM_TERM_PARAM: String = "utm_term"
const val UTM_CONTENT_PARAM: String = "utm_content"

private const val INSTALL_REFERRER_COMPLETED_KEY = "install_referrer_completed"

data class UtmParams(
    val source: String? = null,
    val medium: String? = null,
    val campaign: String? = null,
    val term: String? = null,
    val content: String? = null,
)

class UtmAttributionStore(
    private val preferences: Preferences,
) {
    private var utmParams: UtmParams? = null

    /**
     * Store UTM parameters only if they haven't been set before.
     * This preserves first-touch attribution semantics.
     */
    suspend fun storeIfEmpty(
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

    private suspend fun saveIfEmpty(
        key: String,
        value: String?,
    ) {
        value?.takeIf { it.isNotBlank() }?.let {
            if (preferences.getString(key).isNullOrBlank()) {
                preferences.putString(key, it)
            }
        }
    }

    suspend fun get(): UtmParams? =
        if (isInstallReferrerCompleted()) {
            utmParams ?: UtmParams(
                source = preferences.getString(UTM_SOURCE_PARAM)?.takeIf { it.isNotBlank() },
                medium = preferences.getString(UTM_MEDIUM_PARAM)?.takeIf { it.isNotBlank() },
                campaign = preferences.getString(UTM_CAMPAIGN_PARAM)?.takeIf { it.isNotBlank() },
                term = preferences.getString(UTM_TERM_PARAM)?.takeIf { it.isNotBlank() },
                content = preferences.getString(UTM_CONTENT_PARAM)?.takeIf { it.isNotBlank() },
            ).also { utmParams = it }
        } else {
            null
        }

    suspend fun isInstallReferrerCompleted(): Boolean = preferences.getBoolean(INSTALL_REFERRER_COMPLETED_KEY) ?: false

    private suspend fun markInstallReferrerCompleted() {
        preferences.putBoolean(INSTALL_REFERRER_COMPLETED_KEY, true)
    }

    suspend fun clear() {
        utmParams = null
        preferences.clearAll()
    }
}
