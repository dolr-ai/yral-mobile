package com.yral.shared.iap.util

import platform.Foundation.NSLock

internal inline fun <T> NSLock.withLock(block: () -> T): T {
    lock()
    return try {
        block()
    } finally {
        unlock()
    }
}
