package com.yral.shared.preferences

import com.russhwolf.settings.Settings
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64

class SimplePreferences(val settings: Settings) : Preferences {
    override suspend fun putBoolean(key: String, boolean: Boolean) {
        settings.putBoolean(key, boolean)
    }

    override suspend fun getBoolean(key: String): Boolean? {
        return settings.getBooleanOrNull(key)
    }

    override suspend fun putString(key: String, value: String) {
        settings.putString(key, value)
    }

    override suspend fun getString(key: String): String? {
        return settings.getStringOrNull(key)
    }

    override suspend fun putInt(key: String, int: Int) {
        settings.putInt(key, int)
    }

    override suspend fun getInt(key: String): Int? {
        return settings.getIntOrNull(key)
    }

    override suspend fun putLong(key: String, long: Long) {
        settings.putLong(key, long)
    }

    override suspend fun getLong(key: String): Long? {
        return settings.getLongOrNull(key)
    }

    override suspend fun putFloat(key: String, float: Float) {
        settings.putFloat(key, float)
    }

    override suspend fun getFloat(key: String): Float? {
        return settings.getFloatOrNull(key)
    }

    override suspend fun putDouble(key: String, double: Double) {
        settings.putDouble(key, double)
    }

    override suspend fun getDouble(key: String): Double? {
        return settings.getDoubleOrNull(key)
    }

    override suspend fun putBytes(key: String, bytes: ByteArray) {
        settings.putString(key, bytes.encodeBase64())
    }

    override suspend fun getBytes(key: String): ByteArray? {
        return settings.getStringOrNull(key)?.decodeBase64Bytes()
    }

    override suspend fun remove(key: String) {
        settings.remove(key)
    }

    override suspend fun clearAll() {
        settings.clear()
    }
}