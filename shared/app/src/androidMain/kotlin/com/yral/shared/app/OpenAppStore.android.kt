package com.yral.shared.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.koin.koinInstance

actual fun openAppStoreForUpdate() {
    val context = koinInstance.get<Context>().applicationContext
    val crashlyticsManager = koinInstance.get<CrashlyticsManager>()
    val intent =
        Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=${context.packageName}".toUri(),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        crashlyticsManager.recordException(e)
    }
}
