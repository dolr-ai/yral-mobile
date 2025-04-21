package com.yral.shared.core

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

abstract class SuspendUseCase<in P, out R>(
    coroutineDispatcher: CoroutineDispatcher,
) : BaseSuspendUseCase<P, R, Throwable>(coroutineDispatcher) {
    final override fun Throwable.toError() = this
}

abstract class ResultSuspendUseCase<in P, out R, out E>(
    coroutineDispatcher: CoroutineDispatcher,
) : BaseSuspendUseCase<P, R, E>(coroutineDispatcher) {
    abstract override suspend fun executeWith(parameter: P): Result<R, E>

    @Suppress("UseCheckOrError")
    override suspend fun execute(parameter: P): R = throw IllegalStateException("This should not be called")
}

abstract class BaseSuspendUseCase<in P, out R, out E> internal constructor(
    private val coroutineDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(parameter: P): Result<R, E> =
        try {
            withContext(coroutineDispatcher) {
                executeWith(parameter).onFailure { onFailure(UseCaseException("$it")) }
            }
        } catch (e: CancellationException) {
            @Suppress("RethrowCaughtException")
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") throwable: Throwable,
        ) {
            onFailure(throwable)
            Err(throwable.toError())
        }

    private fun onFailure(throwable: Throwable) {
        Err(throwable.toError())
        // crashReporter.recordException(throwable)
    }

    open suspend fun executeWith(parameter: P): Result<R, E> = Ok(execute(parameter))

    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameter: P): R

    protected abstract fun Throwable.toError(): E
}

private class UseCaseException(
    message: String,
) : Exception(message)
