package com.yral.shared.preferences

import com.russhwolf.settings.Settings
import com.yral.shared.core.PlatformResources

expect fun provideSharedPreferences(
    preferenceName: String,
    platformResources: PlatformResources,
): Settings
