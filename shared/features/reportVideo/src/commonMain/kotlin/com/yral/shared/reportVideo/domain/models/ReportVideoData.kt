package com.yral.shared.reportVideo.domain.models

data class ReportVideoData(
    val reason: VideoReportReason,
    val otherReasonText: String,
    val successMessage: Pair<String, String>,
)
