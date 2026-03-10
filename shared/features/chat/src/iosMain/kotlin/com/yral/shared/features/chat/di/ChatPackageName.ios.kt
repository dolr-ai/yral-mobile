package com.yral.shared.features.chat.di

import platform.Foundation.NSBundle

internal actual fun getAppPackageName(): String = NSBundle.mainBundle.bundleIdentifier ?: "com.yral.iosApp"
