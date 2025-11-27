package com.yral.shared.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.datastore.DataStoreSettings
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import androidx.datastore.preferences.core.Preferences as AndroidPreferences

/**
 * Android implementation that produces encrypted [SharedPreferences]-backed [Settings].
 */
actual class PreferencesFactory : KoinComponent {
    private val context: Context by inject()

    private val dataStores = mutableMapOf<String, DataStore<AndroidPreferences>>()
    private lateinit var dataStoreScope: CoroutineScope

    actual fun create(preferenceName: String): Settings =
        SharedPreferencesSettings(initializeEncryptedSharedPreferencesManager(preferenceName))

    @OptIn(ExperimentalSettingsApi::class, ExperimentalSettingsImplementation::class)
    actual fun createDataStore(
        preferenceName: String,
        appDispatchers: AppDispatchers,
    ): FlowSettings {
        if (!::dataStoreScope.isInitialized) {
            dataStoreScope = CoroutineScope(appDispatchers.disk + SupervisorJob())
        }
        val dataStore =
            dataStores.getOrPut(preferenceName) {
                PreferenceDataStoreFactory.create(
                    produceFile = { context.preferencesDataStoreFile(preferenceName) },
                    scope = dataStoreScope,
                )
            }
        return DataStoreSettings(dataStore)
    }

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
