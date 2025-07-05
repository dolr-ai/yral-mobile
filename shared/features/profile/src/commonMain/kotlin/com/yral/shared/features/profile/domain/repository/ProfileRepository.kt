package com.yral.shared.features.profile.domain.repository

import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import com.yral.shared.features.profile.domain.models.ProfileVideosPageResult

interface ProfileRepository {
    suspend fun getProfileVideos(
        startIndex: ULong,
        pageSize: ULong,
    ): ProfileVideosPageResult

    suspend fun deleteVideo(request: DeleteVideoRequest)
}
