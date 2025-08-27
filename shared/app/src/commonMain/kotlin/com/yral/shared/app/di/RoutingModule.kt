package com.yral.shared.app.di

import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.libs.routing.deeplink.engine.DeepLinkParser
import com.yral.shared.libs.routing.routes.api.Home
import com.yral.shared.libs.routing.deeplink.engine.RoutingService
import com.yral.shared.libs.routing.routes.api.TestProductRoute
import com.yral.shared.libs.routing.routes.api.TestUserRoute
import com.yral.shared.libs.routing.routes.api.Unknown
import com.yral.shared.libs.routing.deeplink.engine.UrlBuilder
import com.yral.shared.libs.routing.deeplink.engine.buildRouting
import com.yral.shared.libs.routing.deeplink.engine.DefaultRoutingService
import com.yral.shared.libs.routing.deeplink.engine.RoutingTable
import com.yral.shared.libs.routing.deeplink.engine.buildRoutingTable
import org.koin.dsl.module

val routingModule = module {

    single<RoutingTable> {
        buildRoutingTable {
            // Core app routes
            route<Home>("/")
            route<Unknown>("/unknown")

            // Test routes (for development/testing)
            route<TestProductRoute>("/test/product/{productId}")
            route<TestUserRoute>("/test/user/{userId}")
        }
    }

    single<DeepLinkParser> {
        DeepLinkParser(
            routingTable = get()
        )
    }

    single<UrlBuilder> {
        UrlBuilder(
            routingTable = get(),
            scheme = "https",
            host = "yral.app"
        )
    }

    single<RoutingService> {
        DefaultRoutingService(get(), get())
    }
}
