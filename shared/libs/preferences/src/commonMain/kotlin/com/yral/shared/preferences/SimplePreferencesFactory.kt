package com.yral.shared.preferences

import com.yral.shared.core.PlatformResources
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject

class SimplePreferencesFactory(
    private val platformResources: PlatformResources,
) {
    private val prefs by lazy {
        SimplePreferences(
            settings = provideSharedPreferences(USER_SHARED_PREF_NAME, platformResources),
        )
    }

    fun build(): Preferences = prefs

    @OptIn(InternalCoroutinesApi::class)
    companion object : SynchronizedObject() {
        private const val USER_SHARED_PREF_NAME = "YRAL_PREF"

        @Volatile
        private var instance: SimplePreferencesFactory? = null

        fun getInstance(platformResources: PlatformResources): SimplePreferencesFactory =
            instance ?: synchronized(this) {
                instance ?: SimplePreferencesFactory(platformResources).also { instance = it }
            }
    }
}
