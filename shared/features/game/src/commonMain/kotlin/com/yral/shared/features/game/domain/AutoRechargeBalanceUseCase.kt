package com.yral.shared.features.game.domain

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import com.yral.shared.features.game.domain.models.AutoRechargeBalanceError
import com.yral.shared.features.game.domain.models.AutoRechargeBalanceErrorCodes
import com.yral.shared.features.game.domain.models.AutoRechargeBalanceRequest
import com.yral.shared.features.game.domain.models.UpdatedBalance
import com.yral.shared.firebaseAuth.usecase.GetIdTokenUseCase
import com.yral.shared.libs.arch.domain.ResultSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class AutoRechargeBalanceUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val getIdTokenUseCase: GetIdTokenUseCase,
    private val gameRepository: IGameRepository,
) : ResultSuspendUseCase<AutoRechargeBalanceRequest, UpdatedBalance, AutoRechargeBalanceError>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    @Suppress("MaxLineLength")
    override suspend fun executeWith(parameter: AutoRechargeBalanceRequest): Result<UpdatedBalance, AutoRechargeBalanceError> {
        val idToken = getIdTokenUseCase.invoke(GetIdTokenUseCase.DEFAULT).getOrThrow()
        return gameRepository.autoRechargeBalance(idToken, parameter)
    }

    override fun Throwable.toError() =
        AutoRechargeBalanceError(
            code = AutoRechargeBalanceErrorCodes.UNKNOWN,
            message = message ?: "",
            throwable = this,
        )
}
