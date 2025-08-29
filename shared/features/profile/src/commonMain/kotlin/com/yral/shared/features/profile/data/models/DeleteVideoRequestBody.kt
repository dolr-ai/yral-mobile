package com.yral.shared.features.profile.data.models

import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeleteVideoRequestBody(
    @SerialName("publisher_user_id")
    val principal: String,
    @SerialName("post_id")
    val postId: String,
    @SerialName("video_id")
    val videoId: String,
    @SerialName("delegated_identity_wire")
    val delegatedIdentityWire: KotlinDelegatedIdentityWire,
)
