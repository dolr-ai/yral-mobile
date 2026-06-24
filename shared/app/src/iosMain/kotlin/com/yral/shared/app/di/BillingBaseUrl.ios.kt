package com.yral.shared.app.di

import com.yral.shared.core.AppConfigurations
import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo

actual fun resolveBillingBaseUrl(): String {
    val env = NSProcessInfo.processInfo.environment["YRAL_BILLING_BASE_URL"] as? String
    val plist = NSBundle.mainBundle.objectForInfoDictionaryKey("YRAL_BILLING_BASE_URL") as? String
    return listOf(env, plist)
        .firstOrNull { !it.isNullOrBlank() }
        ?: AppConfigurations.BILLING_BASE_URL
}
