package com.yral.shared.reportVideo.domain

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.reportVideo.domain.models.ReportRequest

class ReportVideoUseCase(
    private val repository: IReportVideoRepository,
    private val sessionManager: SessionManager,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<ReportRequestParams, String>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: ReportRequestParams): String {
        val userCanister = sessionManager.canisterID
        val userPrincipal = sessionManager.userPrincipal
        if (userCanister != null && userPrincipal != null) {
            return repository
                .reportVideo(
                    ReportRequest(
                        postId = parameter.postId,
                        videoId = parameter.videoId,
                        reason = parameter.reason,
                        canisterID = parameter.canisterID,
                        principal = parameter.principal,
                        userCanisterId = userCanister,
                        userPrincipal = userPrincipal,
                    ),
                )
        }
        throw YralException("Session not found while reporting video")
    }
}

data class ReportRequestParams(
    val postId: String,
    val videoId: String,
    val reason: String,
    val canisterID: String,
    val principal: String,
)
