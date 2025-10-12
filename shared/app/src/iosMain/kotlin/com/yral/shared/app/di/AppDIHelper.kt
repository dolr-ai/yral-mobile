package com.yral.shared.app.di

import com.yral.featureflag.FeatureFlagManager
import com.yral.shared.libs.routing.deeplink.engine.RoutingService
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class AppDIHelper : KoinComponent {
    fun getFeatureFlagManager(): FeatureFlagManager = get()
    fun getRoutingService(): RoutingService = get()
}
