package com.yral.shared.core.analytics

import com.russhwolf.settings.Settings

private const val AFFILIATE_PREF_KEY = "signup_affiliate_name"

class AffiliateAttributionStore(
    private val settings: Settings,
) {
    fun storeIfEmpty(affiliate: String) {
        if (affiliate.isBlank()) return
        val current = settings.getStringOrNull(AFFILIATE_PREF_KEY)
        if (current.isNullOrBlank()) {
            settings.putString(AFFILIATE_PREF_KEY, affiliate)
        }
    }

    fun consume(): String? {
        val value =
            settings
                .getStringOrNull(AFFILIATE_PREF_KEY)
                ?.takeIf { it.isNotBlank() }
        if (value != null) {
            settings.remove(AFFILIATE_PREF_KEY)
        }
        return value
    }

    fun peek(): String? =
        settings
            .getStringOrNull(AFFILIATE_PREF_KEY)
            ?.takeIf { it.isNotBlank() }

    fun clear() {
        settings.remove(AFFILIATE_PREF_KEY)
    }
}
