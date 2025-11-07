package com.yral.shared.features.profile.domain.repository

import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import com.yral.shared.features.profile.domain.models.FollowNotification
import com.yral.shared.features.profile.domain.models.ProfileVideosPageResult

interface ProfileRepository {
    suspend fun getProfileVideos(
        canisterId: String,
        userPrincipal: String,
        isFromServiceCanister: Boolean,
        startIndex: ULong,
        pageSize: ULong,
    ): ProfileVideosPageResult

    suspend fun deleteVideo(request: DeleteVideoRequest)

    suspend fun uploadProfileImage(imageBase64: String): String

    suspend fun followNotification(request: FollowNotification)
}
