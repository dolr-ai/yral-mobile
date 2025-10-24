package com.yral.shared.analytics

import com.yral.shared.analytics.events.TokenType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class User(
    val userId: String,
    val canisterId: String,
    val isLoggedIn: Boolean?,
    val isCreator: Boolean?,
    val walletBalance: Double?,
    val tokenType: TokenType?,
    val isForcedGamePlayUser: Boolean?,
    val emailId: String?,
)

@Serializable
data class SuperProperties(
    @SerialName("is_creator")
    val isCreator: Boolean?,
    @SerialName("is_logged_in")
    val isLoggedIn: Boolean?,
    @SerialName("wallet_balance")
    val walletBalance: Double?,
    @SerialName("token_type")
    val tokenType: TokenType?,
    @SerialName("canister_id")
    val canisterId: String?,
    @SerialName("is_forced_gameplay_test_user")
    val isForcedGamePlayUser: Boolean?,
    @SerialName("email_id")
    val emailId: String?,
    @SerialName("user_id")
    val userId: String?,
    @SerialName("visitor_id")
    val visitorId: String?,
) {
    constructor() : this(
        isCreator = null,
        isLoggedIn = null,
        walletBalance = null,
        tokenType = null,
        canisterId = null,
        isForcedGamePlayUser = null,
        emailId = null,
        userId = null,
        visitorId = null,
    )
}

fun User.toSuperProperties(): SuperProperties =
    SuperProperties(
        isCreator = isCreator,
        isLoggedIn = isLoggedIn,
        walletBalance = walletBalance,
        tokenType = tokenType,
        canisterId = canisterId,
        isForcedGamePlayUser = isForcedGamePlayUser,
        emailId = emailId,
        userId = if (isLoggedIn == true) userId else null,
        visitorId = if (isLoggedIn != true) userId else null,
    )
