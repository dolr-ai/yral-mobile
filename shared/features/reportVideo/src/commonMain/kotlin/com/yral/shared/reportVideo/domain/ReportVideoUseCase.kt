package com.yral.shared.reportVideo.domain

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.core.session.SessionManager
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.reportVideo.domain.models.ReportRequest
import com.yral.shared.rust.service.utils.delegatedIdentityWireToJson
import kotlinx.serialization.json.Json

class ReportVideoUseCase(
    private val repository: IReportVideoRepository,
    private val sessionManager: SessionManager,
    private val json: Json,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<ReportRequestParams, String>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: ReportRequestParams): String {
        val userCanister = sessionManager.canisterID
        val userPrincipal = sessionManager.userPrincipal
        val identity = sessionManager.identity
        if (identity != null && userCanister != null && userPrincipal != null) {
            val identityWireJson = delegatedIdentityWireToJson(identity)
            val delegatedIdentityWire =
                json.decodeFromString<KotlinDelegatedIdentityWire>(identityWireJson)
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
                        delegatedIdentityWire = delegatedIdentityWire,
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
