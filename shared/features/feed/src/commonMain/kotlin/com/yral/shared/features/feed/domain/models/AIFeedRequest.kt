package com.yral.shared.features.feed.domain.models

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import com.yral.shared.features.feed.data.models.AIFeedRequestDto

data class AIFeedRequest(
    val userId: String,
    val count: Int,
    val recommendationType: RecommendationType = RecommendationType.MIXED,
)

enum class RecommendationType {
    MIXED,
    POPULARITY,
    FRESHNESS,
}

fun AIFeedRequest.toDto(): AIFeedRequestDto =
    AIFeedRequestDto(
        userId = userId,
        count = count,
        recommendationType = recommendationType.name.toLowerCase(Locale.current),
    )
