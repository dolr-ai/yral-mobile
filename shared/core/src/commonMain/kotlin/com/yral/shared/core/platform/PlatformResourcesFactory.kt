package com.yral.shared.core.platform

class PlatformResourcesFactory {
    private var resources: PlatformResources? = null

    fun resources(): PlatformResources = resources ?: error("Resources not initialised")

    fun initialize(platformResources: PlatformResources) {
        this.resources = platformResources
    }
}
