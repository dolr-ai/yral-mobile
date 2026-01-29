package com.yral.shared.features.tournament.data.models

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.features.tournament.domain.model.VideoEmojisResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoEmojisResponseDto(
    @SerialName("video_id")
    val videoId: String,
    @SerialName("emojis")
    val emojis: List<VideoEmojiDto>,
    @SerialName("is_custom")
    val isCustom: Boolean = false,
)

fun VideoEmojisResponseDto.toVideoEmojisResult(): Result<VideoEmojisResult, Throwable> =
    Ok(
        VideoEmojisResult(
            videoId = videoId,
            emojis = emojis.map { it.toVideoEmoji() },
            isCustom = isCustom,
        ),
    )
