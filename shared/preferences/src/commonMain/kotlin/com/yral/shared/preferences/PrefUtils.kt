package com.yral.shared.preferences

import com.russhwolf.settings.Settings
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json

class PrefUtils(val settings: Settings = Preferences.settings) {
    fun putBoolean(key: PrefKeys, boolean: Boolean) {
        settings.putBoolean(key.name, boolean)
    }

    fun getBoolean(key: PrefKeys): Boolean? {
        return settings.getBooleanOrNull(key.name)
    }

    fun getBooleanWithDefaultValue(key: PrefKeys, default: Boolean): Boolean {
        return settings.getBoolean(key.name, default)
    }

    fun putInt(key: PrefKeys, int: Int) {
        settings.putInt(key.name, int)
    }

    fun getInt(key: PrefKeys): Int? {
        return settings.getIntOrNull(key.name)
    }

    fun getIntWithDefaultValue(key: PrefKeys, default: Int): Int {
        return settings.getInt(key.name, default)
    }

    fun putLong(key: PrefKeys, long: Long) {
        settings.putLong(key.name, long)
    }

    fun getLong(key: PrefKeys): Long? {
        return settings.getLongOrNull(key.name)
    }

    fun getLongWithDefaultValue(key: PrefKeys, default: Long): Long {
        return settings.getLong(key.name, default)
    }

    fun putDouble(key: PrefKeys, double: Double) {
        settings.putDouble(key.name, double)
    }

    fun getDouble(key: PrefKeys): Double? {
        return settings.getDoubleOrNull(key.name)
    }

    fun getDoubleWithDefaultValue(key: PrefKeys, default: Double): Double {
        return settings.getDouble(key.name, default)
    }

    fun putString(key: PrefKeys, string: String) {
        settings.putString(key.name, string)
    }

    fun getString(key: PrefKeys): String? {
        return settings.getStringOrNull(key.name)
    }

    fun putString(key: String, string: String) {
        settings.putString(key, string)
    }

    fun getString(key: String): String? {
        return settings.getStringOrNull(key)
    }

    fun getStringWithDefaultValue(key: PrefKeys, default: String): String {
        return settings.getString(key.name, default)
    }

    fun putListString(key: PrefKeys, stringList: ArrayList<String>) {
        val typedStringList = stringList.toTypedArray()
        putString(key, typedStringList.joinToString("‚‗‚"))
    }

    fun getListString(key: PrefKeys): ArrayList<String> {
        val joinedString = getStringWithDefaultValue(key, "")
        return ArrayList(joinedString.split("‚‗‚").toList())
    }

    fun putByteArray(key: PrefKeys, bytes: ByteArray) {
        settings.putString(key.name, bytes.encodeBase64())
    }

    fun getByteArray(key: PrefKeys): ByteArray? {
        return settings.getStringOrNull(key.name)?.let { return it.decodeBase64Bytes() }
    }

    inline fun <reified T> setData(key: PrefKeys, data: T) {
        settings.putString(key.name, Json.encodeToString(data))
    }

    inline fun <reified T> getData(key: PrefKeys, deserializer: DeserializationStrategy<T>): T? {
        return settings.getStringOrNull(key.name)?.let { Json.decodeFromString(deserializer, it) }
    }

    fun clearAll() {
        settings.clear()
    }

    fun remove(key: PrefKeys) {
        settings.remove(key.name)
    }

    fun remove(key: String) {
        settings.remove(key)
    }
}