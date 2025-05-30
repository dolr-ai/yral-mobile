package com.yral.shared.core.platform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect interface PlatformResources
