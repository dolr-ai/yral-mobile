package com.yral.shared.crashlytics.core

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import io.sentry.kotlin.multiplatform.protocol.User

internal class SentryCrashlyticsProvider : CrashlyticsProvider {
    override val name: String
        get() = "sentry"

    override fun recordException(exception: Exception) {
        Sentry.captureException(exception)
    }

    override fun recordException(
        exception: Exception,
        type: ExceptionType,
    ) {
        Sentry.captureException(exception) { scope ->
            scope.setTag(ERROR_TYPE_TAG, type.name.lowercase())
        }
    }

    override fun logMessage(message: String) {
        Sentry.addBreadcrumb(Breadcrumb.info(message))
    }

    override fun setUserId(id: String) {
        Sentry.configureScope { scope ->
            scope.user = User(id = id)
        }
    }

    private companion object {
        const val ERROR_TYPE_TAG = "error_type"
    }
}
