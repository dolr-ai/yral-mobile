package com.yral.shared.core.utils

import platform.Foundation.NSThread
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.darwin.dispatch_time

/**
 * Runs [block] on the main queue and blocks the calling thread waiting for it to finish.
 *
 * WARNING: This uses [dispatch_semaphore_create] + [dispatch_async] +
 * [dispatch_semaphore_wait] with [DISPATCH_TIME_FOREVER], which can deadlock if the
 * main run loop is not servicing the queue or if the caller holds locks.
 */
inline fun <T> runOnMainSync(crossinline block: () -> T): T = runOnMainSync(timeoutMillis = null, block = block)

/**
 * Runs [block] on the main queue and blocks the calling thread waiting for it to finish.
 *
 * @param timeoutMillis Optional timeout. When provided, this uses [dispatch_time] instead of
 * [DISPATCH_TIME_FOREVER] and throws [TimeoutException] if the block does not complete in time.
 */
@Suppress("MagicNumber")
inline fun <T> runOnMainSync(
    timeoutMillis: Long?,
    crossinline block: () -> T,
): T {
    if (NSThread.isMainThread()) {
        return block()
    }
    var result: T? = null
    var error: Throwable? = null
    val semaphore = dispatch_semaphore_create(0)
    dispatch_async(dispatch_get_main_queue()) {
        try {
            result = block()
        } catch (
            @Suppress("TooGenericExceptionCaught") t: Throwable,
        ) {
            error = t
        } finally {
            dispatch_semaphore_signal(semaphore)
        }
    }
    val timeout =
        if (timeoutMillis == null) {
            DISPATCH_TIME_FOREVER
        } else {
            dispatch_time(0u, timeoutMillis * 1_000_000)
        }
    val waitResult = dispatch_semaphore_wait(semaphore, timeout)
    if (waitResult != 0L) {
        throw TimeoutException("Timed out waiting for main queue after ${timeoutMillis}ms")
    }
    error?.let { throw it }
    @Suppress("UNCHECKED_CAST")
    return result as T
}

class TimeoutException(
    message: String,
) : RuntimeException(message)
