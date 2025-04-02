package com.yral.shared.preferences

import com.yral.shared.core.PlatformResources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject

class AsyncPreferencesFactory(
    private val platformResources: PlatformResources,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val asyncPrefs by lazy {
        AsyncPreferencesImpl(
            settings = provideSharedPreferences(USER_SHARED_PREF_NAME, platformResources),
            ioDispatcher = ioDispatcher,
        )
    }

    fun build(): Preferences = asyncPrefs

    @OptIn(InternalCoroutinesApi::class)
    companion object : SynchronizedObject() {
        private const val USER_SHARED_PREF_NAME = "YRAL_PREF"

        @Volatile
        private var instance: AsyncPreferencesFactory? = null

        fun getInstance(platformResources: PlatformResources, ioDispatcher: CoroutineDispatcher): AsyncPreferencesFactory =
            instance ?: synchronized(this) {
                instance ?: AsyncPreferencesFactory(platformResources, ioDispatcher).also { instance = it }
            }
    }
}