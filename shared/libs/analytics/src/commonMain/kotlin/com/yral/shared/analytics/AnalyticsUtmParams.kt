package com.yral.shared.analytics

data class AnalyticsUtmParams(
    val source: String? = null,
    val medium: String? = null,
    val campaign: String? = null,
    val term: String? = null,
    val content: String? = null,
) {
    companion object {
        private const val UTM_SOURCE_PARAM: String = "utm_source"
        private const val UTM_MEDIUM_PARAM: String = "utm_medium"
        private const val UTM_CAMPAIGN_PARAM: String = "utm_campaign"
        private const val UTM_TERM_PARAM: String = "utm_term"
        // private const val UTM_CONTENT_PARAM: String = "utm_content"
    }

    fun toMap(): Map<String, Any?> {
        val utmParamsMap: MutableMap<String, Any?> = mutableMapOf()
        source?.let { utmParamsMap[UTM_SOURCE_PARAM] = it }
        medium?.let { utmParamsMap[UTM_MEDIUM_PARAM] = it }
        campaign?.let { utmParamsMap[UTM_CAMPAIGN_PARAM] = it }
        term?.let { utmParamsMap[UTM_TERM_PARAM] = it }
        // content?.let { utmParamsMap[UTM_CONTENT_PARAM] = it }
        return utmParamsMap.toMap()
    }
}
