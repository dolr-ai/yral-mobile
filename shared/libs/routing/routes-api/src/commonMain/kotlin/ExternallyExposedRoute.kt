/**
 * A marker interface for AppRoutes that are safe to be triggered
 * from an external source. The DeepLinkParser will reject any route
 * that does not implement this interface.
 */
interface ExternallyExposedRoute
