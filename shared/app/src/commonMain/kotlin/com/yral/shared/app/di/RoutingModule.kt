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
import org.koin.dsl.module

private val appRoutingTable = buildRouting<AppRoute> {
    // Core app routes
    route<Home>("/")
    route<Unknown>("/unknown")

    // Test routes (for development/testing)
    route<TestProductRoute>("/test/product/{productId}")
    route<TestUserRoute>("/test/user/{userId}")
}

val routingModule = module {

    single<DeepLinkParser> {
        DeepLinkParser(
            routingTable = appRoutingTable
        )
    }

    single<UrlBuilder> {
        UrlBuilder(
            routingTable = appRoutingTable,
            scheme = "https",
            host = "yral.app"
        )
    }

    single<RoutingService> {
        AppRoutingServiceImpl(get(), get())
    }
}

class AppRoutingServiceImpl(
    private val deepLinkParser: DeepLinkParser,
    private val urlBuilder: UrlBuilder
) : RoutingService {

    override fun parseUrl(url: String): AppRoute {
        return deepLinkParser.parse(url)
    }

    override fun parseParameters(params: Map<String, String>): AppRoute {
        return deepLinkParser.parse(params)
    }

    override fun buildUrl(route: AppRoute): String? {
        return urlBuilder.build(route)
    }
}
