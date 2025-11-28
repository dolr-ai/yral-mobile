package com.yral.android.installReferrer

import android.app.Application
import android.content.ContentResolver
import androidx.core.net.toUri
import com.yral.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("TooGenericExceptionCaught")
class MetaInstallReferrerFetcher(
    private val application: Application,
) {
    private companion object {
        private const val FACEBOOK_PROVIDER = "com.facebook.katana.provider.InstallReferrerProvider"
        private const val INSTAGRAM_PROVIDER = "com.instagram.contentprovider.InstallReferrerProvider"
        private const val FACEBOOK_LITE_PROVIDER = "com.facebook.lite.provider.InstallReferrerProvider"
        private val PROJECTION = arrayOf("install_referrer", "is_ct", "actual_timestamp")
    }

    private val logger = AttributionManager.createLogger("MetaInstallReferrerFetcher")

    suspend fun fetch(): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                queryMetaInstallReferrer()?.also { referrerData ->
                    logger.i { "Meta Install Referrer fetched: $referrerData" }
                }
            }.onFailure { exception ->
                logger.e(exception) { "Failed to fetch Meta Install Referrer" }
            }.getOrNull()
        }

    private fun queryMetaInstallReferrer(): String? {
        val packageManager = application.packageManager
        val facebookAppId = application.getString(R.string.facebook_app_id)
        val contentResolver: ContentResolver = application.contentResolver
        // Check all three Meta apps in priority order
        val providers =
            listOf(
                FACEBOOK_PROVIDER to "Facebook",
                INSTAGRAM_PROVIDER to "Instagram",
                FACEBOOK_LITE_PROVIDER to "Facebook Lite",
            )
        for ((providerAuthority, appName) in providers) {
            if (packageManager.resolveContentProvider(providerAuthority, 0) == null) {
                continue
            }
            val result = queryProvider(providerAuthority, appName, facebookAppId, contentResolver)
            if (result != null) {
                return result
            }
        }
        logger.d { "No Meta Install Referrer data found in any provider" }
        return null
    }

    @Suppress("ReturnCount")
    private fun queryProvider(
        providerAuthority: String,
        appName: String,
        facebookAppId: String,
        contentResolver: ContentResolver,
    ): String? {
        val providerUri = "content://$providerAuthority/$facebookAppId".toUri()
        return runCatching {
            logger.d { "Querying $appName Install Referrer ContentProvider: $providerUri" }
            val cursor = contentResolver.query(providerUri, PROJECTION, null, null, null)
            cursor?.use { currentCursor ->
                if (!currentCursor.moveToFirst()) {
                    logger.d { "No data returned from $appName ContentProvider" }
                    return@runCatching null
                }

                val installReferrerIndex = currentCursor.getColumnIndex("install_referrer")
                if (installReferrerIndex < 0) {
                    logger.d { "install_referrer column not found in $appName" }
                    return@runCatching null
                }

                val installReferrer = currentCursor.getString(installReferrerIndex)
                val timestamp =
                    currentCursor
                        .getColumnIndex("actual_timestamp")
                        .takeIf { it >= 0 }
                        ?.let { currentCursor.getLong(it) }
                val isCT =
                    currentCursor
                        .getColumnIndex("is_ct")
                        .takeIf { it >= 0 }
                        ?.let { currentCursor.getInt(it) }

                if (installReferrer.isNullOrBlank()) {
                    null
                } else {
                    logger.i {
                        "Meta Install Referrer retrieved from $appName: " +
                            "length=${installReferrer.length}, is_ct=$isCT, timestamp=$timestamp"
                    }
                    installReferrer
                }
            }
        }.onFailure {
            logger.e(it) { "Error querying $appName Install Referrer ContentProvider" }
        }.getOrNull()
    }
}
