package com.yral.shared.features.game.domain.models

data class AboutGameItem(
    val name: String,
    val thumbnailUrl: String,
    val body: List<AboutGameItemBody>,
)

data class AboutGameItemBody(
    val type: AboutGameBodyType,
    val content: List<String>?,
    val bolds: List<Boolean>?,
    val colors: List<String>?,
    val imageUrls: List<String>?,
)

enum class AboutGameBodyType {
    TEXT,
    IMAGES,
    UNKNOWN,
}
