package com.yral.shared.preferences

import com.russhwolf.settings.Settings
import com.yral.shared.core.platform.PlatformResourcesFactory

expect fun provideSharedPreferences(
    preferenceName: String,
    platformResourcesFactory: PlatformResourcesFactory,
): Settings
