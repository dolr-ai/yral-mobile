package com.yral.shared.preferences

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual class PreferencesFactory {
    actual fun create(preferenceName: String): Settings {
        val userDefaults = NSUserDefaults(suiteName = preferenceName)
        return NSUserDefaultsSettings(userDefaults)
    }
}
