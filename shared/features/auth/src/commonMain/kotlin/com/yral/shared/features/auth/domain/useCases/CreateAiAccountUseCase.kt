package com.yral.shared.features.auth.domain.useCases

import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class CreateAiAccountUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val authRepository: AuthRepository,
) : SuspendUseCase<CreateAiAccountUseCase.Params, ByteArray>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override suspend fun execute(parameter: Params): ByteArray =
        authRepository.createAiAccount(
            userPrincipal = parameter.userPrincipal,
            signature = parameter.signature,
            publicKey = parameter.publicKey,
            signedMessage = parameter.signedMessage,
            ingressExpirySecs = parameter.ingressExpirySecs,
            ingressExpiryNanos = parameter.ingressExpiryNanos,
            delegations = parameter.delegations,
        )

    data class Params(
        val userPrincipal: String,
        val signature: ByteArray,
        val publicKey: ByteArray,
        val signedMessage: ByteArray,
        val ingressExpirySecs: Long,
        val ingressExpiryNanos: Int,
        val delegations: List<com.yral.shared.rust.service.utils.SignedDelegationPayload>?,
    )
}
