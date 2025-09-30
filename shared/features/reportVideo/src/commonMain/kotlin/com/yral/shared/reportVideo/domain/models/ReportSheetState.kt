package com.yral.shared.reportVideo.domain.models

sealed interface ReportSheetState {
    data object Closed : ReportSheetState
    data class Open(
        val pageNo: Int,
        val reasons: List<VideoReportReason> =
            listOf(
                VideoReportReason.NUDITY_PORN,
                VideoReportReason.VIOLENCE,
                VideoReportReason.OFFENSIVE,
                VideoReportReason.SPAM,
                VideoReportReason.OTHERS,
            ),
    ) : ReportSheetState
}

enum class VideoReportReason(
    val reason: String,
) {
    NUDITY_PORN("Nudity / Porn"),
    VIOLENCE("Violence / Gore"),
    OFFENSIVE("Offensive"),
    SPAM("Spam / Ad"),
    OTHERS("Others"),
}
