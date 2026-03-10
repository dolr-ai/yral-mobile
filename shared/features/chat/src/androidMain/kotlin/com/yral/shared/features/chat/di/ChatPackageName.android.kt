package com.yral.shared.features.chat.di

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

internal actual fun getAppPackageName(): String =
    runCatching {
        ChatPackageNameResolver.get<Context>().packageName
    }.getOrElse { "com.yral.android.app" }

private object ChatPackageNameResolver : KoinComponent
