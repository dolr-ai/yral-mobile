package com.yral.shared.features.game.domain

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.game.domain.models.AutoRechargeBalanceError
import com.yral.shared.features.game.domain.models.AutoRechargeBalanceErrorCodes
import com.yral.shared.features.game.domain.models.AutoRechargeBalanceRequest
import com.yral.shared.features.game.domain.models.UpdatedBalance
import com.yral.shared.firebaseAuth.usecase.GetIdTokenUseCase
import com.yral.shared.libs.useCase.ResultSuspendUseCase

class AutoRechargeBalanceUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val getIdTokenUseCase: GetIdTokenUseCase,
    private val gameRepository: IGameRepository,
) : ResultSuspendUseCase<AutoRechargeBalanceRequest, UpdatedBalance, AutoRechargeBalanceError>(
        appDispatchers.io,
        crashlyticsManager,
    ) {
    @Suppress("MaxLineLength")
    override suspend fun executeWith(parameter: AutoRechargeBalanceRequest): Result<UpdatedBalance, AutoRechargeBalanceError> {
        val idToken = getIdTokenUseCase.invoke(GetIdTokenUseCase.DEFAULT).getOrThrow()
        return gameRepository
            .autoRechargeBalance(parameter.copy(idToken = idToken))
    }

    override fun Throwable.toError() =
        AutoRechargeBalanceError(
            code = AutoRechargeBalanceErrorCodes.UNKNOWN,
            message = message ?: "",
            throwable = this,
        )
}
