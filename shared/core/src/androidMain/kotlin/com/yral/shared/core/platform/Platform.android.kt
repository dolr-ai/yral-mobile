package com.yral.shared.core.platform

import android.content.Context

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
