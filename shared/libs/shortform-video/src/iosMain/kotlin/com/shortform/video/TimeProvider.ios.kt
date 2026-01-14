package com.shortform.video

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long {
    val seconds = NSDate().timeIntervalSince1970
    return (seconds * 1000.0).toLong()
}
