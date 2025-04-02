package com.yral.shared.rust.http

import com.yral.shared.preferences.Preferences
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

enum class CookieType(val value: String) {
    USER_IDENTITY("user-identity")
}

class PersistentCookieStorage(private val preferences: Preferences) : CookiesStorage {
    private val mutex = Mutex()

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        if (CookieType.entries.any { it.value == cookie.name }) {
            mutex.withLock {
                preferences.putString(cookie.name, Json.encodeToString(cookie))
            }
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        return mutex.withLock {
            val cookies = mutableListOf<Cookie>()
            CookieType.entries.forEach { entry ->
                preferences.getString(entry.value)?.let { value ->
                    cookies.add(Json.decodeFromString(value))
                }
            }
            return@withLock cookies
        }
    }

    override fun close() {}
}


fun Cookie.maxAgeOrExpires(createdAt: Long): Long? =
    maxAge?.let { createdAt + it * 1000L } ?: expires?.timestamp
