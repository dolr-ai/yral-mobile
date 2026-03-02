package com.yral.shared.features.subscriptions.domain

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.iap.IAPManager
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.libs.arch.domain.ResultSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class FetchProductsUseCase(
    private val iapManager: IAPManager,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : ResultSuspendUseCase<List<ProductId>, List<Product>, IAPError>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override suspend fun executeWith(parameter: List<ProductId>): Result<List<Product>, IAPError> =
        iapManager.fetchProducts(parameter).fold(
            onSuccess = { Ok(it) },
            onFailure = { error ->
                val iapError = error as? IAPError ?: IAPError.UnknownError(error)
                Err(iapError)
            },
        )

    override fun Throwable.toError(): IAPError = this as? IAPError ?: IAPError.UnknownError(this)
}
