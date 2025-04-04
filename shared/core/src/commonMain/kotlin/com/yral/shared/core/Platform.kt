package com.yral.shared.core

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect interface PlatformResources

object PlatformResourcesHolder {
    lateinit var platformResources: PlatformResources

    fun initialize(platformResources: PlatformResources) {
        this.platformResources = platformResources
    }
}
