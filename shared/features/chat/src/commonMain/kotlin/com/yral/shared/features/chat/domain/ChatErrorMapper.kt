package com.yral.shared.features.chat.domain

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.features.chat.domain.models.ChatError
import com.yral.shared.http.exception.NetworkException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException

class ChatErrorMapper {
    fun mapException(
        throwable: Throwable,
        retry: () -> Unit,
    ): ChatError =
        when (throwable) {
            is NetworkException -> mapNetworkException(throwable, retry)
            is YralException -> mapYralException(throwable, retry)
            is CancellationException -> throw throwable // Don't catch cancellation
            else -> ChatError.UnknownError(throwable = throwable, retry = retry)
        }

    private fun mapNetworkException(
        exception: NetworkException,
        retry: () -> Unit,
    ): ChatError =
        when (val cause = exception.cause) {
            is ClientRequestException -> mapClientError(cause, retry)
            is ServerResponseException -> ChatError.ServerError(throwable = exception, retry = retry)
            is RedirectResponseException -> ChatError.NetworkError(throwable = exception, retry = retry)
            else -> ChatError.NetworkError(throwable = exception, retry = retry)
        }

    private fun mapClientError(
        exception: ClientRequestException,
        retry: () -> Unit,
    ): ChatError =
        when (exception.response.status) {
            HttpStatusCode.Unauthorized,
            HttpStatusCode.Forbidden,
            -> ChatError.AuthenticationError(throwable = exception)
            else -> ChatError.NetworkError(throwable = exception, retry = retry)
        }

    private fun mapYralException(
        exception: YralException,
        retry: () -> Unit,
    ): ChatError {
        val message = exception.text ?: exception.message
        return when {
            message?.contains("Authorisation", ignoreCase = true) == true ->
                ChatError.AuthenticationError(throwable = exception)
            else -> ChatError.UnknownError(throwable = exception, retry = retry)
        }
    }
}
