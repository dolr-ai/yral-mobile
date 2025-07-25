package com.yral.shared.preferences

import com.russhwolf.settings.Settings
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import kotlinx.coroutines.withContext

class AsyncPreferencesImpl(
    private val settings: Settings,
    private val appDispatchers: AppDispatchers,
) : Preferences {
    override suspend fun putBoolean(
        key: String,
        boolean: Boolean,
    ) {
        withContext(appDispatchers.disk) {
            settings.putBoolean(key, boolean)
        }
    }

    override suspend fun getBoolean(key: String): Boolean? =
        withContext(appDispatchers.disk) {
            settings.getBooleanOrNull(key)
        }

    override suspend fun putString(
        key: String,
        value: String,
    ) {
        withContext(appDispatchers.disk) {
            settings.putString(key, value)
        }
    }

    override suspend fun getString(key: String): String? =
        withContext(appDispatchers.disk) {
            settings.getStringOrNull(key)
        }

    override suspend fun putInt(
        key: String,
        int: Int,
    ) {
        withContext(appDispatchers.disk) {
            settings.putInt(key, int)
        }
    }

    override suspend fun getInt(key: String): Int? =
        withContext(appDispatchers.disk) {
            settings.getIntOrNull(key)
        }

    override suspend fun putLong(
        key: String,
        long: Long,
    ) {
        withContext(appDispatchers.disk) {
            settings.putLong(key, long)
        }
    }

    override suspend fun getLong(key: String): Long? =
        withContext(appDispatchers.disk) {
            settings.getLongOrNull(key)
        }

    override suspend fun putFloat(
        key: String,
        float: Float,
    ) {
        withContext(appDispatchers.disk) {
            settings.putFloat(key, float)
        }
    }

    override suspend fun getFloat(key: String): Float? =
        withContext(appDispatchers.disk) {
            settings.getFloatOrNull(key)
        }

    override suspend fun putDouble(
        key: String,
        double: Double,
    ) {
        withContext(appDispatchers.disk) {
            settings.putDouble(key, double)
        }
    }

    override suspend fun getDouble(key: String): Double? =
        withContext(appDispatchers.disk) {
            settings.getDoubleOrNull(key)
        }

    override suspend fun putBytes(
        key: String,
        bytes: ByteArray,
    ) {
        withContext(appDispatchers.disk) {
            settings.putString(key, bytes.encodeBase64())
        }
    }

    override suspend fun getBytes(key: String): ByteArray? =
        withContext(appDispatchers.disk) {
            settings.getStringOrNull(key)?.decodeBase64Bytes()
        }

    override suspend fun remove(key: String) {
        withContext(appDispatchers.disk) {
            settings.remove(key)
        }
    }

    override suspend fun clearAll() {
        withContext(appDispatchers.disk) {
            settings.clear()
        }
    }
}
