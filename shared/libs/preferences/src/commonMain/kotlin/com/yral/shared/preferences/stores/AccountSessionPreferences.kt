package com.yral.shared.preferences.stores

import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences

class AccountSessionPreferences(
    private val preferences: Preferences,
) {
    suspend fun getMainPrincipal(): String? = preferences.getString(PrefKeys.MAIN_PRINCIPAL.name)

    suspend fun setMainPrincipal(value: String?) {
        if (value != null) {
            preferences.putString(PrefKeys.MAIN_PRINCIPAL.name, value)
        } else {
            preferences.remove(PrefKeys.MAIN_PRINCIPAL.name)
        }
    }

    suspend fun getLastActivePrincipal(): String? = preferences.getString(PrefKeys.LAST_ACTIVE_PRINCIPAL.name)

    suspend fun setLastActivePrincipal(value: String?) {
        if (value != null) {
            preferences.putString(PrefKeys.LAST_ACTIVE_PRINCIPAL.name, value)
        } else {
            preferences.remove(PrefKeys.LAST_ACTIVE_PRINCIPAL.name)
        }
    }

    suspend fun getMainIdentity(): ByteArray? = preferences.getBytes(PrefKeys.MAIN_IDENTITY.name)

    suspend fun setMainIdentity(value: ByteArray?) {
        if (value != null) {
            preferences.putBytes(PrefKeys.MAIN_IDENTITY.name, value)
        } else {
            preferences.remove(PrefKeys.MAIN_IDENTITY.name)
        }
    }
}
