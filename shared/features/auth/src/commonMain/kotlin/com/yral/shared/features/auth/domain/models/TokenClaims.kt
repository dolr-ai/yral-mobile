package com.yral.shared.features.auth.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenClaims(
    val aud: List<String>,
    @SerialName("exp")
    val expiry: Long,
    @SerialName("iat")
    val issuedAtTime: Long,
    @SerialName("iss")
    val issuerHost: String,
    @SerialName("sub")
    val principal: String,
    @SerialName("nonce")
    val nonce: String?,
    @SerialName("ext_is_anonymous")
    val extIsAnonymous: Boolean,
    @SerialName("email")
    val email: String? = null,
    @SerialName("ext_ai_account_ids")
    val botAccountIds: List<String>? = null,
) {
    fun isValid(currentTimeInEpochSeconds: Long): Boolean = expiry > currentTimeInEpochSeconds
}
