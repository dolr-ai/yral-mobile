package com.yral.shared.features.auth.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateAiAccountRequestDto(
    @SerialName("user_principal") val userPrincipal: String,
    val signature: SignaturePayloadDto,
)

@Serializable
data class SignaturePayloadDto(
    @SerialName("sig") val sig: List<Int>,
    @SerialName("public_key") val publicKey: List<Int>,
    @SerialName("ingress_expiry") val ingressExpiry: IngressExpiryDto,
    val delegations: List<SignedDelegationDto>? = null,
    val sender: String,
)

@Serializable
data class IngressExpiryDto(
    val secs: Long,
    val nanos: Int,
)

@Serializable
data class DelegationDto(
    @SerialName("pubkey") val pubKey: List<Int>,
    @SerialName("expiration_ns") val expirationNs: Long,
    val targets: List<String>? = null,
)

@Serializable
data class SignedDelegationDto(
    val delegation: DelegationDto,
    val signature: List<Int>,
)
