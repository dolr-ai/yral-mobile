package com.yral.shared.core

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect interface PlatformResources

class PlatformResourcesFactory {
    private var resources: PlatformResources? = null

    fun resources(): PlatformResources = resources ?: error("Not initialised")

    fun initialize(platformResources: PlatformResources) {
        this.resources = platformResources
    }
}
