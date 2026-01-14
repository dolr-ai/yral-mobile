package com.yral.shared.libs.phonevalidation

import java.util.Locale

actual class DeviceLocaleDetector {
    actual fun getDeviceRegionCode(): String? = Locale.getDefault().country.takeIf { it.isNotBlank() }

    actual fun getDeviceLanguageCode(): String? = Locale.getDefault().language.takeIf { it.isNotBlank() }
}
