package com.yral.shared.features.game.data.models

import com.yral.shared.features.game.domain.models.AboutGameItem
import com.yral.shared.features.game.domain.models.AboutGameItemBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AboutGameItemDto(
    val name: String,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String,
    val body: List<AboutGameItemBodyDto>,
)

@Serializable
data class AboutGameItemBodyDto(
    val type: String,
    val content: List<String>? = null,
    val colors: List<String>? = null,
    @SerialName("image_urls")
    val imageUrls: List<String>? = null,
)

fun AboutGameItemDto.toAboutGameItem(): AboutGameItem =
    AboutGameItem(
        name = name,
        thumbnailUrl = thumbnailUrl,
        body =
            body.map {
                it.toAboutGameItemBody()
            },
    )

fun AboutGameItemBodyDto.toAboutGameItemBody(): AboutGameItemBody =
    AboutGameItemBody(
        type = type,
        content = content,
        colors = colors,
        imageUrls = imageUrls,
    )
