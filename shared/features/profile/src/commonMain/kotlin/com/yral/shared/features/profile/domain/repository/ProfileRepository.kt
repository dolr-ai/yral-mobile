package com.yral.shared.features.profile.domain.repository

import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import com.yral.shared.features.profile.domain.models.ProfileVideosPageResult
import com.yral.shared.features.profile.domain.models.VideoViews

interface ProfileRepository {
    suspend fun getProfileVideos(
        canisterId: String,
        userPrincipal: String,
        isFromServiceCanister: Boolean,
        startIndex: ULong,
        pageSize: ULong,
    ): ProfileVideosPageResult

    suspend fun deleteVideo(request: DeleteVideoRequest)

    suspend fun getProfileVideoViewsCount(videoId: String): List<VideoViews>
}
