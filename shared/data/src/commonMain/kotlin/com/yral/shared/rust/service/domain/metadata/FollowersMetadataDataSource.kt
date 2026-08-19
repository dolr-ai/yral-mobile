package com.yral.shared.rust.service.domain.metadata

interface FollowersMetadataDataSource {
    suspend fun fetchUsernames(principals: List<String>): Map<String, String>
}
