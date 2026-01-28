package com.yral.shared.features.subscriptions.domain

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.iap.IAPManager
import com.yral.shared.iap.PurchaseResult
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.libs.arch.domain.ResultSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class QueryPurchaseUseCase(
    private val iapManager: IAPManager,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : ResultSuspendUseCase<Unit, PurchaseResult, IAPError>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override suspend fun executeWith(parameter: Unit): Result<PurchaseResult, IAPError> {
        val result = iapManager.isProductPurchased(ProductId.YRAL_PRO)
        return result.fold(
            onSuccess = { purchaseResult ->
                Ok(purchaseResult)
            },
            onFailure = { error ->
                val iapError = error as? IAPError ?: IAPError.UnknownError(error)
                Err(iapError)
            },
        )
    }

    override fun Throwable.toError(): IAPError = this as? IAPError ?: IAPError.UnknownError(this)
}
