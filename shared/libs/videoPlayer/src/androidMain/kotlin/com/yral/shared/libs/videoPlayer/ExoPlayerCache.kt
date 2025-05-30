package com.yral.shared.libs.videoPlayer

import android.annotation.SuppressLint
import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

const val CACHE_DIR_NAME = "cached_videos"
const val MAX_CACHE_SIZE = 256 * 1024 * 1024L // 256MB

class MediaCache private constructor(
    context: Context,
) {
    @SuppressLint("UnsafeOptInUsageError")
    val cacheFactory: CacheDataSource.Factory

    init {
        cacheFactory = setupExoPlayerCache(context)
    }

    companion object {
        @Volatile
        private lateinit var instance: MediaCache

        fun getInstance(context: Context): MediaCache {
            synchronized(this) {
                if (!Companion::instance.isInitialized) {
                    instance = MediaCache(context)
                }
                return instance
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun setupExoPlayerCache(context: Context): CacheDataSource.Factory {
        val cacheEvictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE)
        val databaseProvider = StandaloneDatabaseProvider(context)
        val cache =
            SimpleCache(
                File(context.cacheDir, CACHE_DIR_NAME),
                cacheEvictor,
                databaseProvider,
            )
        val upstreamFactory = DefaultDataSource.Factory(context)
        return CacheDataSource.Factory().apply {
            setCache(cache)
            setUpstreamDataSourceFactory(upstreamFactory)
            setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        }
    }
}
