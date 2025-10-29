package com.yral.shared.features.profile.domain

import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.profile.domain.models.FollowNotification
import com.yral.shared.features.profile.domain.repository.ProfileRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.service.utils.delegatedIdentityWireToJson
import kotlinx.serialization.json.Json

class FollowNotificationUseCase(
    private val sessionManager: SessionManager,
    private val json: Json,
    private val profileRepository: ProfileRepository,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : SuspendUseCase<FollowNotificationUseCase.Params, Unit>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: Params) {
        val identityWire = sessionManager.identity
        identityWire?.let {
            val identityWireJson = delegatedIdentityWireToJson(identityWire)
            val delegatedIdentity =
                json.decodeFromString<KotlinDelegatedIdentityWire>(identityWireJson)
            profileRepository
                .followNotification(
                    request =
                        FollowNotification(
                            followerUsername = parameter.followerUsername,
                            targetPrincipal = parameter.targetPrincipal,
                            delegatedIdentity = delegatedIdentity,
                        ),
                )
        }
    }

    data class Params(
        val followerUsername: String,
        val targetPrincipal: String,
    )
}
