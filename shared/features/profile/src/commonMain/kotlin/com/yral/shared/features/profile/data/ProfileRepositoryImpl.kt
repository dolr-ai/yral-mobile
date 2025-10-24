package com.yral.shared.features.profile.data

import com.yral.shared.features.profile.data.models.toDomain
import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import com.yral.shared.features.profile.domain.models.ProfileVideosPageResult
import com.yral.shared.features.profile.domain.models.VideoViews
import com.yral.shared.features.profile.domain.repository.ProfileRepository

class ProfileRepositoryImpl(
    private val dataSource: ProfileDataSource,
) : ProfileRepository {
    override suspend fun getProfileVideos(
        canisterId: String,
        userPrincipal: String,
        isFromServiceCanister: Boolean,
        startIndex: ULong,
        pageSize: ULong,
    ): ProfileVideosPageResult =
        dataSource
            .getProfileVideos(canisterId, userPrincipal, isFromServiceCanister, startIndex, pageSize)

    override suspend fun deleteVideo(request: DeleteVideoRequest) =
        dataSource
            .deleteVideo(request)

    override suspend fun getProfileVideoViewsCount(videoId: List<String>): List<VideoViews> =
        dataSource
            .getProfileVideoViewsCount(videoId)
            .map { it.toDomain() }
}
