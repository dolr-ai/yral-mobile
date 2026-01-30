package com.yral.shared.iap.utils

import platform.Foundation.NSBundle

internal actual object PackageNameProvider {
    actual fun getPackageName(): String = NSBundle.mainBundle.bundleIdentifier ?: "com.yral.iosApp"
}
