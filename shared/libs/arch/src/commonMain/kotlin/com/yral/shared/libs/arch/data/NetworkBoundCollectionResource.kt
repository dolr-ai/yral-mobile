package com.yral.shared.libs.arch.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

abstract class NetworkBoundCollectionResource<RequestType : Any, ResultType : Any>(
    dispatchers: AppDispatchers,
    failureListener: OnFailureListener,
) : NetworkBoundResource<RequestType, Collection<ResultType>>(dispatchers, failureListener) {
    override fun canEmitInitialDbValue(data: Collection<ResultType>): Boolean = data.isNotEmpty()

    override fun mapDataToResultOnNetworkFailure(
        data: Collection<ResultType>?,
        throwable: Throwable,
    ): Result<Collection<ResultType>, Throwable> =
        if (!data.isNullOrEmpty()) {
            Ok(data)
        } else {
            Err(throwable)
        }
}
