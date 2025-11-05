package com.yral.shared.data.feed.data

import com.yral.shared.data.feed.domain.VideoViews
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
data class VideoViewsDto(
    @SerialName("video_id")
    val videoId: String,
    @SerialName("total_count_all")
    val allViews: ULong,
    @SerialName("total_count_loggedin")
    val loggedInViews: ULong,
)

@OptIn(ExperimentalTime::class)
fun VideoViewsDto.toDomain(): VideoViews =
    VideoViews(
        videoId = videoId,
        allViews = allViews,
        loggedInViews = loggedInViews,
        lastFetched = Clock.System.now(),
    )
