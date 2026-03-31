package com.yral.shared.testsupport.preferences

import com.yral.shared.preferences.Preferences

class FakePreferences : Preferences {
    private val storage = mutableMapOf<String, Any?>()

    override suspend fun putBoolean(
        key: String,
        boolean: Boolean,
    ) {
        storage[key] = boolean
    }

    override suspend fun getBoolean(key: String): Boolean? = storage[key] as? Boolean

    override suspend fun putString(
        key: String,
        value: String,
    ) {
        storage[key] = value
    }

    override suspend fun getString(key: String): String? = storage[key] as? String

    override suspend fun putInt(
        key: String,
        int: Int,
    ) {
        storage[key] = int
    }

    override suspend fun getInt(key: String): Int? = storage[key] as? Int

    override suspend fun putLong(
        key: String,
        long: Long,
    ) {
        storage[key] = long
    }

    override suspend fun getLong(key: String): Long? = storage[key] as? Long

    override suspend fun putFloat(
        key: String,
        float: Float,
    ) {
        storage[key] = float
    }

    override suspend fun getFloat(key: String): Float? = storage[key] as? Float

    override suspend fun putDouble(
        key: String,
        double: Double,
    ) {
        storage[key] = double
    }

    override suspend fun getDouble(key: String): Double? = storage[key] as? Double

    override suspend fun putBytes(
        key: String,
        bytes: ByteArray,
    ) {
        storage[key] = bytes
    }

    override suspend fun getBytes(key: String): ByteArray? = storage[key] as? ByteArray

    override suspend fun remove(key: String) {
        storage.remove(key)
    }

    override suspend fun clearAll() {
        storage.clear()
    }
}
