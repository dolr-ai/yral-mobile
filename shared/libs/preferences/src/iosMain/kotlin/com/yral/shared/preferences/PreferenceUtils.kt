package com.yral.shared.preferences

import com.russhwolf.settings.Settings
import com.yral.shared.core.platform.PlatformResourcesFactory

actual fun provideSharedPreferences(
    preferenceName: String,
    platformResourcesFactory: PlatformResourcesFactory,
): Settings = error("Not implemented on iOS.")
