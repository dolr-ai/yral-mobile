package com.yral.shared.core.rust

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KotlinDelegatedIdentityWire(
    @SerialName("from_key") val fromKey: IntArray,
    @SerialName("to_secret") val toSecret: KotlinJwkEcKey,
    @SerialName("delegation_chain") val delegationChain: List<SignedDelegation>,
)

@Serializable
data class KotlinJwkEcKey(
    val crv: String,
    val x: String,
    val y: String,
    val d: String?,
)

@Serializable
data class SignedDelegation(
    val signature: IntArray,
    val delegation: Delegation,
)

@Serializable
data class Delegation(
    val pubkey: IntArray,
    val expiration: Long,
    val targets: List<String>? = null,
)
