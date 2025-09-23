package com.yral.shared.reportVideo.data

import com.yral.shared.reportVideo.domain.IReportVideoRepository
import com.yral.shared.reportVideo.domain.models.ReportRequest
import com.yral.shared.reportVideo.domain.models.toDto

class ReportVideoRepository(
    private val dataSource: IReportVideoDataSource,
) : IReportVideoRepository {
    override suspend fun reportVideo(request: ReportRequest): String =
        dataSource
            .reportVideo(request.toDto())
}
