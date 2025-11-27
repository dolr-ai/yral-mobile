package com.yral.shared.preferences

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.FlowSettings
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

expect class PreferencesFactory() {
    fun create(preferenceName: String): Settings

    @OptIn(ExperimentalSettingsApi::class)
    fun createDataStore(
        preferenceName: String,
        appDispatchers: AppDispatchers,
    ): FlowSettings
}
