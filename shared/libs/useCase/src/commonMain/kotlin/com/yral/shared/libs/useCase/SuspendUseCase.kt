package com.yral.shared.libs.useCase

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.yral.shared.crashlytics.core.CrashlyticsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Deprecated("use SuspendUseCase from arch module")
abstract class SuspendUseCase<in P, out R>(
    coroutineDispatcher: CoroutineDispatcher,
    crashlyticsManager: CrashlyticsManager,
) : BaseSuspendUseCase<P, R, Throwable>(coroutineDispatcher, crashlyticsManager) {
    final override fun Throwable.toError() = this
}

@Deprecated("use ResultSuspendUseCase from arch module")
abstract class ResultSuspendUseCase<in P, out R, out E>(
    coroutineDispatcher: CoroutineDispatcher,
    crashlyticsManager: CrashlyticsManager,
) : BaseSuspendUseCase<P, R, E>(coroutineDispatcher, crashlyticsManager) {
    abstract override suspend fun executeWith(parameter: P): Result<R, E>

    @Suppress("UseCheckOrError")
    override suspend fun execute(parameter: P): R = throw IllegalStateException("This should not be called")
}

@Deprecated("use BaseSuspendUseCase from arch module")
abstract class BaseSuspendUseCase<in P, out R, out E> internal constructor(
    private val coroutineDispatcher: CoroutineDispatcher,
    private val crashlyticsManager: CrashlyticsManager,
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
        crashlyticsManager.recordException(Exception("${this@BaseSuspendUseCase::class.simpleName}", throwable))
    }

    open suspend fun executeWith(parameter: P): Result<R, E> = Ok(execute(parameter))

    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameter: P): R

    protected abstract fun Throwable.toError(): E
}

private class UseCaseException(
    message: String,
) : Exception(message)
