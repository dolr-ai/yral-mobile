package com.yral.shared.preferences

import com.russhwolf.settings.Settings

/**
 * Expect declaration of a platform-specific PreferencesFactory able to create a type-safe
 * [Settings] instance for the provided preference file / suite.
 */
expect class PreferencesFactory() {
    fun create(preferenceName: String): Settings
}
