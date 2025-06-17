package com.yral.shared.libs.useCase

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.yral.shared.crashlytics.core.CrashlyticsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach

abstract class FlowUseCase<in P, out R>(
    coroutineDispatcher: CoroutineDispatcher,
    crashlyticsManager: CrashlyticsManager,
) : BaseFlowUseCase<P, R, Throwable>(coroutineDispatcher, crashlyticsManager) {
    final override fun Throwable.toError() = this
    final override fun Throwable.toThrowable(): Throwable = this
}

abstract class BaseFlowUseCase<in P, out R, E> internal constructor(
    private val coroutineDispatcher: CoroutineDispatcher,
    private val crashlyticsManager: CrashlyticsManager,
) {
    operator fun invoke(parameters: P): Flow<Result<R, E>> =
        execute(parameters)
            .onEach { result ->
                result.onFailure { error ->
                    crashlyticsManager.recordException(Exception(error.toThrowable()))
                }
            }.catch { throwable ->
                /* Catch block is not executed for kotlinx.coroutines.CancellationException and it
                is desired to not record it, hence we can simply log the caught exception.*/
                crashlyticsManager.recordException(Exception(throwable))
                emit(Err(throwable.toError()))
            }.flowOn(coroutineDispatcher)

    protected abstract fun execute(parameters: P): Flow<Result<R, E>>

    protected abstract fun Throwable.toError(): E

    protected abstract fun E.toThrowable(): Throwable
}
