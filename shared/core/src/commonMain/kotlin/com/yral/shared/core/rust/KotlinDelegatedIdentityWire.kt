package com.yral.shared.core.rust

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KotlinDelegatedIdentityWire(
    @SerialName("from_key") val fromKey: List<Int>,
    @SerialName("to_secret") val toSecret: KotlinJwkEcKey,
    @SerialName("delegation_chain") val delegationChain: List<SignedDelegation>,
)

@Serializable
data class KotlinJwkEcKey(
    val kty: String,
    val crv: String,
    val x: String,
    val y: String,
    val d: String?,
)

@Serializable
data class SignedDelegation(
    val signature: List<Int>,
    val delegation: Delegation,
)

@Serializable
data class Delegation(
    val pubkey: List<Int>,
    val expiration: Long,
    val targets: List<String>? = null,
)
