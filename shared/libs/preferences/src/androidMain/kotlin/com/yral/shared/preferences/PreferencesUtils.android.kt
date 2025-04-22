package com.yral.shared.preferences

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.yral.shared.core.PlatformResources
import com.yral.shared.core.PlatformResourcesFactory

actual fun provideSharedPreferences(
    preferenceName: String,
    platformResourcesFactory: PlatformResourcesFactory,
): Settings =
    SharedPreferencesSettings(
        initializeEncryptedSharedPreferencesManager(
            preferenceName = preferenceName,
            platformResources = platformResourcesFactory.resources(),
        ),
    )

private fun initializeEncryptedSharedPreferencesManager(
    preferenceName: String,
    platformResources: PlatformResources,
): SharedPreferences {
    val keyGenParameterSpec =
        MasterKey
            .Builder(platformResources.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    return EncryptedSharedPreferences.create(
        platformResources.applicationContext,
        preferenceName,
        keyGenParameterSpec,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}
