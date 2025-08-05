package com.yral.shared.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Android implementation that produces encrypted [SharedPreferences]-backed [Settings].
 */
actual class PreferencesFactory : KoinComponent {
    private val context: Context by inject()

    actual fun create(preferenceName: String): Settings =
        SharedPreferencesSettings(initializeEncryptedSharedPreferencesManager(preferenceName))

    private fun initializeEncryptedSharedPreferencesManager(preferenceName: String): SharedPreferences {
        val keyGenParameterSpec =
            MasterKey
                .Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        return EncryptedSharedPreferences.create(
            context,
            preferenceName,
            keyGenParameterSpec,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}
