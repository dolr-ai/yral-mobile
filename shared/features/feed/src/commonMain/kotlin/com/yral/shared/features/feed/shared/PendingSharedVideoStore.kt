package com.yral.shared.features.feed.shared

import com.yral.shared.libs.routing.routes.api.PostDetailsRoute
object PendingSharedVideoStore {
    private var pendingRoute: PostDetailsRoute? = null

    fun store(route: PostDetailsRoute) {
        pendingRoute = route
    }

    fun consume(): PostDetailsRoute? = pendingRoute
    fun clear() {
        pendingRoute = null
    }
}
