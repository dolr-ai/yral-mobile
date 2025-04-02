package com.yral.shared.preferences

import com.russhwolf.settings.Settings
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class AsyncPreferencesImpl(
    private val settings: Settings,
    private val ioDispatcher: CoroutineDispatcher,
) : Preferences {
    override suspend fun putBoolean(key: String, boolean: Boolean) {
        withContext(ioDispatcher) {
            settings.putBoolean(key, boolean)
        }
    }

    override suspend fun getBoolean(key: String): Boolean? {
        return withContext(ioDispatcher) {
            settings.getBooleanOrNull(key)
        }
    }

    override suspend fun putString(key: String, value: String) {
        withContext(ioDispatcher) {
            settings.putString(key, value)
        }
    }

    override suspend fun getString(key: String): String? {
        return withContext(ioDispatcher) {
            settings.getStringOrNull(key)
        }
    }

    override suspend fun putInt(key: String, int: Int) {
        withContext(ioDispatcher) {
            settings.putInt(key, int)
        }
    }

    override suspend fun getInt(key: String): Int? {
        return withContext(ioDispatcher) {
            settings.getIntOrNull(key)
        }
    }

    override suspend fun putLong(key: String, long: Long) {
        withContext(ioDispatcher) {
            settings.putLong(key, long)
        }
    }

    override suspend fun getLong(key: String): Long? {
        return withContext(ioDispatcher) {
            settings.getLongOrNull(key)
        }
    }

    override suspend fun putFloat(key: String, float: Float) {
        withContext(ioDispatcher) {
            settings.putFloat(key, float)
        }
    }

    override suspend fun getFloat(key: String): Float? {
        return withContext(ioDispatcher) {
            settings.getFloatOrNull(key)
        }
    }

    override suspend fun putDouble(key: String, double: Double) {
        withContext(ioDispatcher) {
            settings.putDouble(key, double)
        }
    }

    override suspend fun getDouble(key: String): Double? {
        return withContext(ioDispatcher) {
            settings.getDoubleOrNull(key)
        }
    }

    override suspend fun putBytes(key: String, bytes: ByteArray) {
        withContext(ioDispatcher) {
            settings.putString(key, bytes.encodeBase64())
        }
    }

    override suspend fun getBytes(key: String): ByteArray? {
        return withContext(ioDispatcher) {
            settings.getStringOrNull(key)?.decodeBase64Bytes()
        }
    }

    override suspend fun remove(key: String) {
        withContext(ioDispatcher) {
            settings.remove(key)
        }
    }

    override suspend fun clearAll() {
        withContext(ioDispatcher) {
            settings.clear()
        }
    }
}