package com.yral.shared.libs.designsystem.component

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.compose.resources.ExperimentalResourceApi
import yral_mobile.shared.libs.designsystem.generated.resources.Res

internal object GifBytesCache {
    private val cache = mutableMapOf<String, ByteArray>()

    fun get(resPath: String): ByteArray? = cache[resPath]

    fun put(
        resPath: String,
        bytes: ByteArray,
    ) {
        cache[resPath] = bytes
    }

    fun contains(resPath: String): Boolean = cache.containsKey(resPath)

    fun clear(vararg resPaths: String) {
        resPaths.forEach { cache.remove(it) }
    }
}

fun clearGifResources(vararg resPaths: String) {
    GifBytesCache.clear(*resPaths)
}

@OptIn(ExperimentalResourceApi::class)
suspend fun preloadGifResources(vararg resPaths: String) =
    coroutineScope {
        resPaths
            .filter { !GifBytesCache.contains(it) }
            .map { resPath ->
                async { GifBytesCache.put(resPath, Res.readBytes(resPath)) }
            }.awaitAll()
    }
