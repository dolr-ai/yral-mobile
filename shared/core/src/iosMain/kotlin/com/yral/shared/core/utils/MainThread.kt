package com.yral.shared.core.utils

import platform.Foundation.NSThread
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait

inline fun <T> runOnMainSync(crossinline block: () -> T): T {
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
    dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)
    error?.let { throw it }
    @Suppress("UNCHECKED_CAST")
    return result as T
}
