package com.yral.shared.preferences

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import com.yral.shared.core.platform.PlatformResourcesFactory
import platform.Foundation.NSUserDefaults

actual fun provideSharedPreferences(
    preferenceName: String,
    platformResourcesFactory: PlatformResourcesFactory,
): Settings {
    val userDefaults = NSUserDefaults(suiteName = preferenceName)
    return NSUserDefaultsSettings(userDefaults)
}
