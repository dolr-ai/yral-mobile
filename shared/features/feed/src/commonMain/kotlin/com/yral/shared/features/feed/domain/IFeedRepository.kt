package com.yral.shared.features.feed.domain

interface IFeedRepository {
    suspend fun reportVideo(request: ReportRequest): String
}
