package com.yral.shared.iap.utils

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

internal actual object PackageNameProvider : KoinComponent {
    actual fun getPackageName(): String =
        runCatching {
            get<Context>().packageName
        }.getOrElse { "com.yral.android.app" }
}
