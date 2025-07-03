package com.yral.shared.features.uploadvideo.data.remote.models

import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.features.uploadvideo.domain.models.UploadFileRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UpdateMetaDataRequestDto(
    @SerialName("video_uid")
    val videoUid: String,
    @SerialName("delegated_identity_wire")
    val delegatedIdentityWire: KotlinDelegatedIdentityWire,
    @SerialName("meta")
    val meta: Map<String, String>,
    @SerialName("post_details")
    val postDetails: PostDetailsDto,
)

@Serializable
internal data class PostDetailsDto(
    @SerialName("is_nsfw")
    val isNsfw: Boolean,
    @SerialName("hashtags")
    val hashtags: List<String>,
    @SerialName("description")
    val description: String,
    @SerialName("video_uid")
    val videoUid: String,
    @SerialName("creator_consent_for_inclusion_in_hot_or_not")
    val creatorConsentForInclusionInHotOrNot: Boolean,
)

internal fun UploadFileRequest.toUpdateMetaDataRequestDto(delegatedIdentityWire: KotlinDelegatedIdentityWire) = UpdateMetaDataRequestDto(
    videoUid = videoUid,
    delegatedIdentityWire = delegatedIdentityWire,
    meta = emptyMap(),
    postDetails = PostDetailsDto(
        isNsfw = isNSFW,
        hashtags = hashtags,
        description = caption,
        videoUid = videoUid,
        creatorConsentForInclusionInHotOrNot = true
    )
)
