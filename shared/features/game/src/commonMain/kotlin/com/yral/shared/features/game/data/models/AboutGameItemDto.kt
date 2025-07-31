package com.yral.shared.features.game.data.models

import com.yral.shared.features.game.domain.models.AboutGameBodyType
import com.yral.shared.features.game.domain.models.AboutGameItem
import com.yral.shared.features.game.domain.models.AboutGameItemBody
import com.yral.shared.firebaseStore.model.AboutGameItemBodyDto
import com.yral.shared.firebaseStore.model.AboutGameItemDto

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
        type =
            when (type) {
                "text" -> AboutGameBodyType.TEXT
                "images" -> AboutGameBodyType.IMAGES
                else -> AboutGameBodyType.UNKNOWN
            },
        content = content,
        bolds = bolds,
        colors = colors,
        imageUrls = imageUrls,
    )
