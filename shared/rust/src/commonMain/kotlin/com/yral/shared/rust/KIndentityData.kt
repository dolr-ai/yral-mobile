package com.yral.shared.rust

import kotlinx.serialization.Serializable

@Serializable
data class KIdentityData constructor(
    val from_key: IntArray,
    val to_secret: Secret,
    val delegation_chain: List<DelegationChain>,
)

@Serializable
data class Secret(
    val kty: String,
    val crv: String,
    val x: String,
    val y: String,
    val d: String,
)

@Serializable
data class DelegationChain constructor(
    val delegation: Delegation,
    val signature: IntArray,
)

@Serializable
data class Delegation constructor(
    val pubkey: IntArray,
    val expiration: Long,
)
