package com.yral.shared.features.game.domain.models

data class AboutGameItem(
    val name: String,
    val thumbnailUrl: String,
    val body: List<AboutGameItemBody>,
)

data class AboutGameItemBody(
    val type: String,
    val content: List<String>?,
    val colors: List<String>?,
    val imageUrls: List<String>?,
)
