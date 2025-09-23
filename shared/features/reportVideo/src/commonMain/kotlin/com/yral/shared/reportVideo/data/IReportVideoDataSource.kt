package com.yral.shared.reportVideo.data

import com.yral.shared.reportVideo.data.models.ReportRequestDto

interface IReportVideoDataSource {
    suspend fun reportVideo(request: ReportRequestDto): String
}
