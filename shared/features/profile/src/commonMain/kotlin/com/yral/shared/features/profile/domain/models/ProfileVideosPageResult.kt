package com.yral.shared.features.profile.domain.models

import com.yral.shared.uniffi.generated.PostDetailsForFrontend

data class ProfileVideosPageResult(
    val posts: List<PostDetailsForFrontend>,
    val hasNextPage: Boolean,
    val nextStartIndex: ULong,
)
