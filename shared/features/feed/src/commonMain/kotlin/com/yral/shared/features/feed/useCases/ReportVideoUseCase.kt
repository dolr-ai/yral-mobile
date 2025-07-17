package com.yral.shared.features.feed.useCases

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.core.session.SessionManager
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.feed.domain.IFeedRepository
import com.yral.shared.features.feed.domain.ReportRequest
import com.yral.shared.libs.useCase.SuspendUseCase
import com.yral.shared.uniffi.generated.delegatedIdentityWireToJson
import kotlinx.serialization.json.Json

class ReportVideoUseCase(
    private val feedRepository: IFeedRepository,
    private val sessionManager: SessionManager,
    private val json: Json,
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
) : SuspendUseCase<ReportRequestParams, String>(
        appDispatchers.io,
        crashlyticsManager,
    ) {
    override suspend fun execute(parameter: ReportRequestParams): String {
        val userCanister = sessionManager.canisterID
        val userPrincipal = sessionManager.userPrincipal
        val identity = sessionManager.identity
        if (identity != null && userCanister != null && userPrincipal != null) {
            val identityWireJson = delegatedIdentityWireToJson(identity)
            val delegatedIdentityWire =
                json.decodeFromString<KotlinDelegatedIdentityWire>(identityWireJson)
            return feedRepository
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
    val postId: Long,
    val videoId: String,
    val reason: String,
    val canisterID: String,
    val principal: String,
)
