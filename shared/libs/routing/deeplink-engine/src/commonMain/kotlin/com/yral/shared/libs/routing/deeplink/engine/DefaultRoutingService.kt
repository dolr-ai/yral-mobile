package com.yral.shared.libs.routing.deeplink.engine

import com.yral.shared.libs.routing.routes.api.AppRoute

/**
 * Default implementation of the [RoutingService] interface.
 * This class orchestrates the parsing and building of URLs using the
 * underlying [DeepLinkParser] and [UrlBuilder] components.
 *
 * As the primary entry point for the routing framework, this is the only
 * class that should be injected into feature modules.
 */
class DefaultRoutingService(
    private val deepLinkParser: DeepLinkParser,
    private val urlBuilder: UrlBuilder,
) : RoutingService {
    /**
     * {@inheritdoc}
     */
    override fun parseUrl(url: String): AppRoute = deepLinkParser.parse(url)

    /**
     * {@inheritdoc}
     */
    override fun parseParameters(params: Map<String, String>): AppRoute = deepLinkParser.parse(params)

    /**
     * {@inheritdoc}
     */
    override fun buildUrl(route: AppRoute): String? = urlBuilder.build(route)
}
