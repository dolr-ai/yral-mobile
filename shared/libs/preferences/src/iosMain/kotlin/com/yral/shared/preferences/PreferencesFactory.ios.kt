package com.yral.shared.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.datastore.DataStoreSettings
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask

actual class PreferencesFactory {
    private val dataStores = mutableMapOf<String, DataStore<Preferences>>()
    private lateinit var dataStoreScope: CoroutineScope

    actual fun create(preferenceName: String): Settings {
        val userDefaults = NSUserDefaults(suiteName = preferenceName)
        return NSUserDefaultsSettings(userDefaults)
    }

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
                PreferenceDataStoreFactory.createWithPath(
                    produceFile = { getDataStoreFilePath(preferenceName) },
                    scope = dataStoreScope,
                )
            }
        return DataStoreSettings(dataStore)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun getDataStoreFilePath(preferenceName: String): okio.Path {
        val documentsPath: String? =
            NSFileManager.defaultManager
                .URLForDirectory(
                    NSDocumentDirectory,
                    NSUserDomainMask,
                    appropriateForURL = null,
                    create = false,
                    error = null,
                )?.path
        return "${requireNotNull(documentsPath)}/$preferenceName.preferences_pb".toPath()
    }
}
