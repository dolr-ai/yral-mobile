package com.yral.shared.app.di

import AppRoute
import DeepLinkParser
import Home
import RoutingService
import TestProductRoute
import TestUserRoute
import Unknown
import UrlBuilder
import buildRouting
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

    single<DeepLinkParser<AppRoute>> {
        DeepLinkParser(
            routingTable = appRoutingTable
        )
    }

    single<UrlBuilder<AppRoute>> {
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
    private val deepLinkParser: DeepLinkParser<AppRoute>,
    private val urlBuilder: UrlBuilder<AppRoute>
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
