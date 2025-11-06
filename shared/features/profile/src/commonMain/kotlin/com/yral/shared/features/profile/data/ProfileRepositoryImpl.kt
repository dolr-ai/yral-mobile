package com.yral.shared.features.profile.data

import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import com.yral.shared.features.profile.domain.models.FollowNotification
import com.yral.shared.features.profile.domain.models.ProfileVideosPageResult
import com.yral.shared.features.profile.domain.models.toDto
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

    override suspend fun uploadProfileImage(imageBase64: String): String =
        dataSource
            .uploadProfileImage(imageBase64)

    override suspend fun followNotification(request: FollowNotification) =
        dataSource
            .followNotification(request.toDto())
}
