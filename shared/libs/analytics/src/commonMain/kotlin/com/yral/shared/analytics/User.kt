package com.yral.shared.analytics

import com.yral.shared.analytics.events.TokenType

data class User(
    val userId: String,
    val canisterId: String,
    val isLoggedIn: Boolean?,
    val isCreator: Boolean?,
    val walletBalance: Double?,
    val tokenType: TokenType?,
    val emailId: String?,
    val utmParams: AnalyticsUtmParams?,
    val isMandatoryLogin: Boolean?,
    val phoneNumber: String?,
    val proStatus: Boolean?,
    val isHonExperiment: Boolean?,
) {
    constructor(
        userId: String,
        canisterId: String,
    ) : this(
        userId = userId,
        canisterId = canisterId,
        isLoggedIn = null,
        isCreator = null,
        walletBalance = null,
        tokenType = null,
        emailId = null,
        utmParams = null,
        isMandatoryLogin = null,
        phoneNumber = null,
        proStatus = null,
        isHonExperiment = null,
    )
}
