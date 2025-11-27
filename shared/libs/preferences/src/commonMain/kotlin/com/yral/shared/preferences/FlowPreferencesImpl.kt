package com.yral.shared.preferences

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.FlowSettings
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import kotlinx.coroutines.withContext

@OptIn(ExperimentalSettingsApi::class)
class FlowPreferencesImpl(
    private val flowSettings: FlowSettings,
    private val appDispatchers: AppDispatchers,
) : Preferences {
    override suspend fun putBoolean(
        key: String,
        boolean: Boolean,
    ) {
        withContext(appDispatchers.disk) {
            flowSettings.putBoolean(key, boolean)
        }
    }

    override suspend fun getBoolean(key: String): Boolean? =
        withContext(appDispatchers.disk) {
            flowSettings.getBooleanOrNull(key)
        }

    override suspend fun putString(
        key: String,
        value: String,
    ) {
        withContext(appDispatchers.disk) {
            flowSettings.putString(key, value)
        }
    }

    override suspend fun getString(key: String): String? =
        withContext(appDispatchers.disk) {
            flowSettings.getStringOrNull(key)
        }

    override suspend fun putInt(
        key: String,
        int: Int,
    ) {
        withContext(appDispatchers.disk) {
            flowSettings.putInt(key, int)
        }
    }

    override suspend fun getInt(key: String): Int? =
        withContext(appDispatchers.disk) {
            flowSettings.getIntOrNull(key)
        }

    override suspend fun putLong(
        key: String,
        long: Long,
    ) {
        withContext(appDispatchers.disk) {
            flowSettings.putLong(key, long)
        }
    }

    override suspend fun getLong(key: String): Long? =
        withContext(appDispatchers.disk) {
            flowSettings.getLongOrNull(key)
        }

    override suspend fun putFloat(
        key: String,
        float: Float,
    ) {
        withContext(appDispatchers.disk) {
            flowSettings.putFloat(key, float)
        }
    }

    override suspend fun getFloat(key: String): Float? =
        withContext(appDispatchers.disk) {
            flowSettings.getFloatOrNull(key)
        }

    override suspend fun putDouble(
        key: String,
        double: Double,
    ) {
        withContext(appDispatchers.disk) {
            flowSettings.putDouble(key, double)
        }
    }

    override suspend fun getDouble(key: String): Double? =
        withContext(appDispatchers.disk) {
            flowSettings.getDoubleOrNull(key)
        }

    override suspend fun putBytes(
        key: String,
        bytes: ByteArray,
    ) {
        withContext(appDispatchers.disk) {
            flowSettings.putString(key, bytes.encodeBase64())
        }
    }

    override suspend fun getBytes(key: String): ByteArray? =
        withContext(appDispatchers.disk) {
            flowSettings.getStringOrNull(key)?.decodeBase64Bytes()
        }

    override suspend fun remove(key: String) {
        withContext(appDispatchers.disk) {
            flowSettings.remove(key)
        }
    }

    override suspend fun clearAll() {
        withContext(appDispatchers.disk) {
            flowSettings.clear()
        }
    }
}
