package com.yral.android.installReferrer

import android.app.Application
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import com.yral.android.R
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.koin.koinInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.qualifier.named

/**
 * Fetches install referrer from Meta apps via ContentProvider.
 * Reference: https://developers.facebook.com/docs/app-ads/meta-install-referrer/
 */
@Suppress("TooGenericExceptionCaught")
class MetaInstallReferrerFetcher(
    private val application: Application,
    private val scope: CoroutineScope,
) {
    private companion object {
        private const val FACEBOOK_PROVIDER = "com.facebook.katana.provider.InstallReferrerProvider"
        private const val INSTAGRAM_PROVIDER = "com.instagram.contentprovider.InstallReferrerProvider"
        private const val FACEBOOK_LITE_PROVIDER = "com.facebook.lite.provider.InstallReferrerProvider"
        private val PROJECTION = arrayOf("install_referrer", "is_ct", "actual_timestamp")
    }

    private val crashlyticsManager: CrashlyticsManager by lazy { koinInstance.get<CrashlyticsManager>() }
    private val metaAttribution by lazy { MetaInstallReferrerAttribution(scope) }
    private val logger: Logger by lazy {
        val baseLogger = koinInstance.get<YralLogger>()
        val sentryLogWriter = koinInstance.get<LogWriter>(named("installReferrerLogWriter"))
        baseLogger.withAdditionalLogWriter(sentryLogWriter).withTag("MetaInstallReferrerFetcher")
    }

    fun fetchAndProcess(onComplete: () -> Unit = {}) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                queryMetaInstallReferrer()?.let { referrerData ->
                    logger.i { "Meta Install Referrer fetched: $referrerData" }
                    processReferrerData(referrerData)
                } ?: logger.d { "No Meta Install Referrer data found" }
            }.onFailure { exception ->
                logger.e(exception) { "Failed to fetch Meta Install Referrer" }
                crashlyticsManager.recordException(
                    exception as? Exception ?: Exception(exception),
                    ExceptionType.INSTALL_REFERRER,
                )
            }.also { onComplete() }
        }
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
        @Suppress("LoopWithTooManyJumpStatements")
        for ((providerAuthority, appName) in providers) {
            if (packageManager.resolveContentProvider(providerAuthority, 0) == null) {
                continue
            }
            val providerUri = Uri.parse("content://$providerAuthority/$facebookAppId")
            var cursor: Cursor? = null
            try {
                logger.d { "Querying $appName Install Referrer ContentProvider: $providerUri" }
                cursor = contentResolver.query(providerUri, PROJECTION, null, null, null)

                if (cursor == null || !cursor.moveToFirst()) {
                    logger.d { "No data returned from $appName ContentProvider" }
                    continue
                }

                val installReferrerIndex = cursor.getColumnIndex("install_referrer")
                if (installReferrerIndex < 0) {
                    logger.d { "install_referrer column not found in $appName" }
                    continue
                }

                val installReferrer = cursor.getString(installReferrerIndex)
                val timestamp = cursor.getColumnIndex("actual_timestamp").takeIf { it >= 0 }?.let { cursor.getLong(it) }
                val isCT = cursor.getColumnIndex("is_ct").takeIf { it >= 0 }?.let { cursor.getInt(it) }

                if (!installReferrer.isNullOrBlank()) {
                    logger.i {
                        "Meta Install Referrer retrieved from $appName: " +
                            "length=${installReferrer.length}, is_ct=$isCT, timestamp=$timestamp"
                    }
                    return installReferrer
                }
            } catch (exception: Exception) {
                logger.e(exception) { "Error querying $appName Install Referrer ContentProvider" }
                // Continue to next provider instead of throwing
            } finally {
                cursor?.close()
            }
        }
        logger.d { "No Meta Install Referrer data found in any provider" }
        return null
    }

    private fun processReferrerData(referrerData: String) {
        try {
            val trimmed = referrerData.trim()
            if (metaAttribution.isMetaInstallReferrerData(trimmed)) {
                logger.i { "Detected encrypted Meta Install Referrer data" }
                metaAttribution.processEncryptedData(trimmed)
            } else {
                logger.d { "No encrypted data found. Referrer: $trimmed" }
            }
        } catch (exception: Exception) {
            logger.e(exception) { "Error processing Meta Install Referrer data" }
            crashlyticsManager.recordException(exception, ExceptionType.INSTALL_REFERRER)
        }
    }
}
