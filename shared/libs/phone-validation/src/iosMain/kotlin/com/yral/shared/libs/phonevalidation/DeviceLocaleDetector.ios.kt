package com.yral.shared.libs.phonevalidation

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.regionCode

actual class DeviceLocaleDetector {
    actual fun getDeviceRegionCode(): String? = NSLocale.currentLocale.regionCode

    actual fun getDeviceLanguageCode(): String? = NSLocale.currentLocale.languageCode
}
