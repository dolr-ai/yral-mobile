package com.yral.shared.iap.util

import platform.Foundation.NSLock

/**
 * Extension function for NSLock to provide a convenient way to execute code within a lock.
 * Ensures the lock is always released, even if an exception is thrown.
 *
 * @param block Code block to execute while holding the lock
 * @return Result of the block execution
 */
internal inline fun <T> NSLock.withLock(block: () -> T): T {
    lock()
    return try {
        block()
    } finally {
        unlock()
    }
}
