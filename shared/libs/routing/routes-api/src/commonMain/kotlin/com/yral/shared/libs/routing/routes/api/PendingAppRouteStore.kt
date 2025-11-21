package com.yral.shared.libs.routing.routes.api

object PendingAppRouteStore {
    private var pendingRoute: AppRoute? = null

    fun store(route: AppRoute) {
        pendingRoute = route
    }

    fun peek(): AppRoute? = pendingRoute

    fun consume(): AppRoute? = pendingRoute.also { pendingRoute = null }

    fun clear() {
        pendingRoute = null
    }
}
