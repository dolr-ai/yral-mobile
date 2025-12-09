import io.sentry.kotlin.multiplatform.Sentry

@Suppress("MagicNumber")
fun initializeSentry(
    dsn: String,
    environment: String,
    release: String,
) {
    require(dsn.isNotBlank()) { "SENTRY_DSN must not be blank" }
    Sentry.init { options ->
        options.dsn = dsn
        options.environment = environment
        options.release = release
        options.sendDefaultPii = true
        options.attachScreenshot = true
        options.attachViewHierarchy = true
        options.tracesSampleRate = 0.1
        options.debug = false
    }
}
