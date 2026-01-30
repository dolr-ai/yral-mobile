package com.yral.shared.iap.core.util

import com.yral.shared.iap.core.IAPError
import kotlin.coroutines.cancellation.CancellationException

/**
 * Executes an IAP operation with standard error handling.
 * Handles CancellationException, IAPError, and generic Exception types.
 *
 * @param operation The suspend operation to execute
 * @return Result containing the operation result or an IAPError
 */
suspend inline fun <T> handleIAPOperation(crossinline operation: suspend () -> T): Result<T> =
    try {
        Result.success(operation())
    } catch (e: CancellationException) {
        throw e
    } catch (e: IAPError) {
        Result.failure(e)
    } catch (
        @Suppress("TooGenericExceptionCaught")
        e: Exception,
    ) {
        Result.failure(IAPError.UnknownError(e))
    }

/**
 * Executes an IAP operation that already returns Result<T>.
 * This is useful for chaining operations that return Result.
 *
 * @param operation The suspend operation that returns Result<T>
 * @return Result containing the operation result or an IAPError
 */
suspend inline fun <T> handleIAPResultOperation(crossinline operation: suspend () -> Result<T>): Result<T> =
    try {
        operation()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IAPError) {
        Result.failure(e)
    } catch (
        @Suppress("TooGenericExceptionCaught")
        e: Exception,
    ) {
        Result.failure(IAPError.UnknownError(e))
    }
