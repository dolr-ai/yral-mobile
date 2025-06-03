package com.yral.shared.firebaseStore.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AboutGameItemDto(
    override val id: String,
    val name: String,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String,
    val body: List<AboutGameItemBodyDto>,
) : FirestoreDocument

@Serializable
data class AboutGameItemBodyDto(
    val type: String,
    val content: List<String>? = null,
    val colors: List<String>? = null,
    @SerialName("image_urls")
    val imageUrls: List<String>? = null,
)
