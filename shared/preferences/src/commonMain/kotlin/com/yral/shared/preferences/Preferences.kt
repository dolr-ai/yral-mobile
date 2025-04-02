package com.yral.shared.preferences

import com.russhwolf.settings.Settings

expect fun provideSharedPreferences(): Settings

object Preferences {
    var settings: Settings = provideSharedPreferences()
}