package com.yral.shared.features.profile.data

import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import com.yral.shared.features.profile.domain.models.ProfileVideosPageResult
import com.yral.shared.features.profile.domain.repository.ProfileRepository

class ProfileRepositoryImpl(
    private val dataSource: ProfileDataSource,
) : ProfileRepository {
    override suspend fun getProfileVideos(
        startIndex: ULong,
        pageSize: ULong,
    ): ProfileVideosPageResult =
        dataSource
            .getProfileVideos(startIndex, pageSize)

    override suspend fun deleteVideo(request: DeleteVideoRequest) =
        dataSource
            .deleteVideo(request)
}
