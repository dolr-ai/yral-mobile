package com.yral.shared.app.di

import com.yral.shared.libs.routing.deeplink.engine.DeepLinkParser
import com.yral.shared.libs.routing.deeplink.engine.DefaultRoutingService
import com.yral.shared.libs.routing.deeplink.engine.RoutingService
import com.yral.shared.libs.routing.deeplink.engine.RoutingTable
import com.yral.shared.libs.routing.deeplink.engine.UrlBuilder
import com.yral.shared.libs.routing.deeplink.engine.buildRoutingTable
import com.yral.shared.libs.routing.routes.api.AddVideo
import com.yral.shared.libs.routing.routes.api.Home
import com.yral.shared.libs.routing.routes.api.Leaderboard
import com.yral.shared.libs.routing.routes.api.PostDetailsRoute
import com.yral.shared.libs.routing.routes.api.Profile
import com.yral.shared.libs.routing.routes.api.TestProductRoute
import com.yral.shared.libs.routing.routes.api.TestUserRoute
import com.yral.shared.libs.routing.routes.api.Unknown
import com.yral.shared.libs.routing.routes.api.Wallet
import org.koin.dsl.module

val routingModule =
    module {

        single<RoutingTable> {
            buildRoutingTable {
                // Core app routes
                route<Home>(Home.PATH)
                route<Unknown>(Unknown.PATH)
                route<Wallet>(Wallet.PATH)
                route<Leaderboard>(Leaderboard.PATH)
                route<AddVideo>(AddVideo.PATH)
                route<Profile>(Profile.PATH)

                // Feed / Post routes
                route<PostDetailsRoute>(PostDetailsRoute.PATH)

                // Test routes (for development/testing)
                route<TestProductRoute>(TestProductRoute.PATH)
                route<TestUserRoute>(TestUserRoute.PATH)
            }
        }

        single<DeepLinkParser> {
            DeepLinkParser(
                routingTable = get(),
            )
        }

        single<UrlBuilder> {
            UrlBuilder(
                routingTable = get(),
                scheme = "yralm",
                host = "",
            )
        }

        single<RoutingService> {
            DefaultRoutingService(get(), get())
        }
    }
