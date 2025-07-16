package com.yral.shared.features.profile.domain.models

import com.yral.shared.analytics.events.VideoDeleteCTA
import com.yral.shared.rust.domain.models.FeedDetails

data class DeleteVideoRequest(
    val feedDetails: FeedDetails,
    val ctaType: VideoDeleteCTA,
)
