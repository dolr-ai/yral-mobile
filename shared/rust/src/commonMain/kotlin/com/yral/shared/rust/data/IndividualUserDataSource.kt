package com.yral.shared.rust.data

import com.yral.shared.data.feed.data.PostDTO
import com.yral.shared.uniffi.generated.PostDetailsForFrontend

interface IndividualUserDataSource {
    suspend fun fetchFeedDetails(post: PostDTO): PostDetailsForFrontend
}
