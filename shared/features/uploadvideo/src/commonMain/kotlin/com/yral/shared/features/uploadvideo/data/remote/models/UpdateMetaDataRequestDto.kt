package com.yral.shared.features.uploadvideo.data.remote.models

import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.features.uploadvideo.domain.models.UploadFileRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UpdateMetaDataRequestDto(
    @SerialName("delegated_identity_wire")
    val delegatedIdentityWire: KotlinDelegatedIdentityWire,
    @SerialName("meta")
    val meta: Map<String, String>,
    @SerialName("post_details")
    val postDetails: PostDetailsDto,
)

@Serializable
internal data class PostDetailsDto(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("description")
    val description: String,
    @SerialName("creator_principal")
    val creatorPrincipal: String,
    @SerialName("video_uid")
    val videoUid: String,
    @SerialName("hashtags")
    val hashtags: List<String>,
    @SerialName("status")
    val status: String = "Published",
)

internal fun UploadFileRequest.toUpdateMetaDataRequestDto(
    delegatedIdentityWire: KotlinDelegatedIdentityWire,
    creatorPrincipal: String,
) = UpdateMetaDataRequestDto(
    delegatedIdentityWire = delegatedIdentityWire,
    meta = emptyMap(),
    postDetails =
        PostDetailsDto(
            id = videoUid,
            title = caption,
            description = caption,
            creatorPrincipal = creatorPrincipal,
            videoUid = videoUid,
            hashtags = hashtags,
        ),
)

@Serializable
internal data class UpdateMetaDataResponseDto(
    val success: Boolean,
    @SerialName("error_message")
    val errorMessage: String?,
    val data: String?,
)
