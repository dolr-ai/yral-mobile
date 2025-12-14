package com.yral.shared.analytics

import com.russhwolf.settings.Settings
import com.yral.shared.preferences.PrefKeys
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class DeviceInstallIdStore(
    private val settings: Settings,
) {
    fun getOrCreate(): String {
        settings.getStringOrNull(PrefKeys.DEVICE_INSTALL_ID.name)?.let { existing ->
            if (existing.isNotBlank()) return existing
        }

        val created = newId()
        settings.putString(PrefKeys.DEVICE_INSTALL_ID.name, created)
        return created
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun newId(): String = Uuid.random().toString()
}
