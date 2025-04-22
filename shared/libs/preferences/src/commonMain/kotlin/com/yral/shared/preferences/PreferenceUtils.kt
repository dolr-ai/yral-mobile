package com.yral.shared.preferences

import com.russhwolf.settings.Settings
import com.yral.shared.core.PlatformResourcesFactory

expect fun provideSharedPreferences(
    preferenceName: String,
    platformResourcesFactory: PlatformResourcesFactory,
): Settings
