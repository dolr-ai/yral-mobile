package com.yral.shared.testsupport.metadata

import com.yral.shared.rust.service.domain.metadata.FollowersMetadataDataSource

class FakeFollowersMetadataDataSource(
    var usernamesByPrincipal: Map<String, String> = emptyMap(),
    var shouldThrow: Boolean = false,
) : FollowersMetadataDataSource {
    override suspend fun fetchUsernames(principals: List<String>): Map<String, String> {
        if (shouldThrow) error("Metadata fetch failed")
        return usernamesByPrincipal
    }
}
