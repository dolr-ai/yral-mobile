package com.yral.shared.reportVideo.domain

import com.yral.shared.reportVideo.domain.models.ReportRequest

interface IReportVideoRepository {
    suspend fun reportVideo(request: ReportRequest): String
}
