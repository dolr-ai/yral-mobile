package com.yral.shared.features.profile.data

import com.yral.shared.data.feed.data.VideoViewsDto
import com.yral.shared.features.profile.data.models.FollowNotificationDto
import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import com.yral.shared.features.profile.domain.models.ProfileVideosPageResult

interface ProfileDataSource {
    suspend fun getProfileVideos(
        canisterId: String,
        userPrincipal: String,
        isFromServiceCanister: Boolean,
        startIndex: ULong,
        pageSize: ULong,
    ): ProfileVideosPageResult

    suspend fun deleteVideo(request: DeleteVideoRequest)

    suspend fun getProfileVideoViewsCount(videoId: List<String>): List<VideoViewsDto>

    suspend fun uploadProfileImage(imageBase64: String): String

    suspend fun followNotification(request: FollowNotificationDto)
}
