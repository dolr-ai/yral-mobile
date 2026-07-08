package com.yral.shared.features.chat.domain.models

/**
 * An influencer's daily photo collage, fetched at render time so [isBlurred]
 * always reflects the viewer's CURRENT subscription state. Chat messages only
 * store the ([botId], [date]) reference — never these URLs.
 */
data class Collage(
    val botId: String,
    val date: String,
    val images: List<String>,
    val isBlurred: Boolean,
    val theme: String?,
    val generatedAt: String?,
)
