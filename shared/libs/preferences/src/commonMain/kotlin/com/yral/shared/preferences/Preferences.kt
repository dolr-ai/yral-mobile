package com.yral.shared.preferences

interface Preferences {
    suspend fun putBoolean(
        key: String,
        boolean: Boolean,
    )
    suspend fun getBoolean(key: String): Boolean?
    suspend fun putString(
        key: String,
        value: String,
    )
    suspend fun getString(key: String): String?
    suspend fun putInt(
        key: String,
        int: Int,
    )
    suspend fun getInt(key: String): Int?
    suspend fun putLong(
        key: String,
        long: Long,
    )
    suspend fun getLong(key: String): Long?
    suspend fun putFloat(
        key: String,
        float: Float,
    )
    suspend fun getFloat(key: String): Float?
    suspend fun putDouble(
        key: String,
        double: Double,
    )
    suspend fun getDouble(key: String): Double?
    suspend fun putBytes(
        key: String,
        bytes: ByteArray,
    )
    suspend fun getBytes(key: String): ByteArray?
    suspend fun remove(key: String)
    suspend fun clearAll()
}
