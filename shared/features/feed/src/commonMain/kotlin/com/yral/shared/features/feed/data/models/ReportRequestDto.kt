package com.yral.shared.features.feed.data.models

import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReportRequestDto(
    @SerialName("post_id")
    val postId: Long,
    @SerialName("video_id")
    val videoId: String,
    val reason: String,
    @SerialName("canister_id")
    val canisterID: String,
    @SerialName("publisher_principal")
    val principal: String,
    @SerialName("user_canister_id")
    val userCanisterId: String,
    @SerialName("user_principal")
    val userPrincipal: String,
    @SerialName("delegated_identity_wire")
    val delegatedIdentityWire: KotlinDelegatedIdentityWire,
    @SerialName("report_mode")
    val reportMode: String = "Android",
)
