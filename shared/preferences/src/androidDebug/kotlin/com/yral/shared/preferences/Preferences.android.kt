package com.yral.shared.preferences

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.yral.shared.core.PlatformResourcesHolder

actual fun provideSharedPreferences(): Settings {
    return SharedPreferencesSettings(initializeEncryptedSharedPreferencesManager())
}

private fun initializeEncryptedSharedPreferencesManager(): SharedPreferences {
    val platformResources = PlatformResourcesHolder.platformResources
    val keyGenParameterSpec = MasterKey.Builder(platformResources.applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    return EncryptedSharedPreferences.create(
        platformResources.applicationContext,
        "yral",
        keyGenParameterSpec,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}