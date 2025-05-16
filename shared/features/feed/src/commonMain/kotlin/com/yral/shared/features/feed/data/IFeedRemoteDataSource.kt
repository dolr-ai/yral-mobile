package com.yral.shared.features.feed.data

import com.yral.shared.features.feed.data.models.ReportRequestDto

interface IFeedRemoteDataSource {
    suspend fun reportVideo(request: ReportRequestDto): String
}
