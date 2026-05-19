package com.yral.shared.features.uploadvideo.data.remote.models

import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.core.rust.KotlinJwkEcKey
import com.yral.shared.features.uploadvideo.domain.models.UploadFileRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateMetaDataRequestDtoTest {
    @Test
    fun updateMetadataRequestUsesDistinctPostIdAndVideoUid() {
        val dto =
            UploadFileRequest(
                postId = "post-123",
                videoUid = "video-456",
                caption = "caption",
                hashtags = listOf("tag"),
            ).toUpdateMetaDataRequestDto(
                delegatedIdentityWire = delegatedIdentityWire(),
                creatorPrincipal = "creator-principal",
            )

        assertEquals("post-123", dto.postDetails.id)
        assertEquals("video-456", dto.postDetails.videoUid)
    }

    private fun delegatedIdentityWire() =
        KotlinDelegatedIdentityWire(
            fromKey = emptyList(),
            toSecret =
                KotlinJwkEcKey(
                    kty = "EC",
                    crv = "P-256",
                    x = "x",
                    y = "y",
                    d = null,
                ),
            delegationChain = emptyList(),
        )
}
