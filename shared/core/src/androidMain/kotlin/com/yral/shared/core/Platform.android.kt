package com.yral.shared.core

import android.content.Context

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual interface PlatformResources {
    val applicationContext: Context
    val activityContext: Context
}

class AndroidPlatformResources(
    private val context: Context,
) : PlatformResources {

    override val applicationContext: Context
        get() = context.applicationContext

    override val activityContext: Context
        get() = context
}